package org.proteored.pacom.analysis.gui.tasks;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.SwingWorker;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.proteored.miapeapi.cv.ControlVocabularyManager;
import org.proteored.miapeapi.experiment.model.Experiment;
import org.proteored.miapeapi.experiment.model.ExperimentList;
import org.proteored.miapeapi.experiment.model.Replicate;
import org.proteored.miapeapi.experiment.msi.MiapeMSIFiltered;
import org.proteored.miapeapi.interfaces.msi.MiapeMSIDocument;
import org.proteored.pacom.analysis.conf.ComparisonProjectFileUtil;
import org.proteored.pacom.analysis.conf.jaxb.CPExperiment;
import org.proteored.pacom.analysis.conf.jaxb.CPExperimentList;
import org.proteored.pacom.analysis.conf.jaxb.CPMS;
import org.proteored.pacom.analysis.conf.jaxb.CPMSI;
import org.proteored.pacom.analysis.conf.jaxb.CPMSIList;
import org.proteored.pacom.analysis.conf.jaxb.CPMSList;
import org.proteored.pacom.analysis.conf.jaxb.CPReplicate;
import org.proteored.pacom.analysis.gui.CuratedExperimentNameCreatorDialog;
import org.proteored.pacom.analysis.gui.CuratedExperimentNamePane;
import org.proteored.pacom.analysis.util.FileManager;

public class CuratedExperimentSaver extends SwingWorker<Void, Void> {
	private static Logger log = Logger.getLogger("log4j.logger.org.proteored");
	public static final String CURATED_EXP_SAVER_START = "curated exp saver start";
	public static final String CURATED_EXP_SAVER_PROGRESS = "curated exp saver progress";
	public static final String CURATED_EXP_SAVER_END = "curated exp saver end";
	public static final String CURATED_EXP_SAVER_ERROR = "curated exp saver error";
	private final ExperimentList experimentList;
	private final ControlVocabularyManager cvManager;
	private final Frame parentFrame;
	private CPExperimentList originalCfgProject;

	public CuratedExperimentSaver(Frame parentFrame, File originalCfgProjectFile, ExperimentList idSet,
			ControlVocabularyManager cvManager, boolean doNotGroupNonConclusiveProteins,
			boolean separateNonConclusiveProteins) {
		experimentList = idSet;
		this.cvManager = cvManager;
		this.parentFrame = parentFrame;
		if (originalCfgProjectFile != null && originalCfgProjectFile.exists()) {
			try {
				this.originalCfgProject = ComparisonProjectFileUtil
						.getExperimentListFromComparisonProjectFile(originalCfgProjectFile);
			} catch (JAXBException e) {
				e.printStackTrace();
				this.originalCfgProject = null;
			}
		} else {
			this.originalCfgProject = null;
		}
	}

	@Override
	protected Void doInBackground() throws Exception {
		try {
			log.info("Creating curated experiments");
			firePropertyChange(CURATED_EXP_SAVER_START, null, null);

			final CuratedExperimentNameCreatorDialog showCurateExperimentNameDialog = CuratedExperimentNamePane
					.showCurateExperimentNameDialog(parentFrame, experimentList);

			final List<Experiment> experimentList2 = experimentList.getExperiments();

			if (experimentList2 != null) {
				for (Experiment experiment : experimentList2) {
					CPExperiment originalcpExp = getOriginalCPExperiment(experiment.getName());
					CPExperiment cpExp = new CPExperiment();
					final String experimentName = showCurateExperimentNameDialog
							.getExperimentName(experiment.getName());
					cpExp.setName(experimentName);
					cpExp.setCurated(true);
					if (FileManager.existsCuratedExperimentXMLFile(cpExp.getName()))
						log.info("The curated experiment " + cpExp.getName() + " is going to be overwrited");
					final List<Replicate> replicates = experiment.getReplicates();
					int i = 1;
					int total = replicates.size();
					for (Replicate replicate : replicates) {
						CPReplicate originalCpRep = getOriginalCPReplicate(originalcpExp, replicate.getName());
						setProgress(i++ * 100 / total);
						firePropertyChange(CURATED_EXP_SAVER_PROGRESS, null,
								"Saving: '" + replicate.getName() + "' from experiment '" + cpExp.getName() + "'");
						final List<MiapeMSIDocument> miapeMSIs = replicate.getMiapeMSIs();
						if (miapeMSIs != null) {

							CPReplicate cpRep = new CPReplicate();
							cpRep.setName(replicate.getName());

							cpExp.getCPReplicate().add(cpRep);

							CPMSIList cpMsiList = new CPMSIList();
							CPMSList cpMsList = new CPMSList();
							for (MiapeMSIDocument miapeMSIDocument : miapeMSIs) {
								CPMSI cpMSI = new CPMSI();
								cpMSI.setId(miapeMSIDocument.getId());
								cpMSI.setLocal(true);

								MiapeMSIFiltered miapeMSIFiltered = new MiapeMSIFiltered(miapeMSIDocument, replicate,
										cvManager);
								final int msDocumentReference = miapeMSIDocument.getMSDocumentReference();
								if (msDocumentReference > 0) {
									// get the original CpMS node. it remains
									// the same
									CPMS cpMS = getOriginalCPMs(originalCpRep, msDocumentReference);
									cpMsList.getCPMS().add(cpMS);
								}

								try {
									String savedPath = FileManager.saveCuratedMiapeMSI(cpExp.getName(),
											miapeMSIFiltered, miapeMSIDocument.getName());
									log.info("MIAPE MSI filtered saved at: " + savedPath);
									cpMSI.setName(miapeMSIDocument.getName());
									cpMSI.setLocalProjectName(cpExp.getName());
									cpMsiList.getCPMSI().add(cpMSI);
								} catch (IOException ex) {
									ex.printStackTrace();
									log.warn(ex.getMessage());
								}
							}
							cpRep.setCPMSIList(cpMsiList);
							cpRep.setCPMSList(cpMsList);
						}
					}
					// save configuration file
					FileManager.saveCuratedExperimentFile(cpExp);
				}
			}

			firePropertyChange(CURATED_EXP_SAVER_END, null, null);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
			firePropertyChange(CURATED_EXP_SAVER_ERROR, null, e.getMessage());
		}
		return null;

	}

	private CPMS getOriginalCPMs(CPReplicate originalCpRep, int msDocumentReference) {
		if (originalCpRep != null) {
			for (CPMS cpMS : originalCpRep.getCPMSList().getCPMS()) {
				if (cpMS.getId() == msDocumentReference) {
					return cpMS;
				}
			}
		}
		return null;
	}

	private CPExperiment getOriginalCPExperiment(String expName) {
		if (originalCfgProject != null) {
			for (CPExperiment exp : originalCfgProject.getCPExperiment()) {
				if (exp.getName().equals(expName)) {
					return exp;
				}
			}
		}
		return null;
	}

	private CPReplicate getOriginalCPReplicate(CPExperiment exp, String repName) {

		if (exp != null) {
			for (CPReplicate rep : exp.getCPReplicate()) {
				if (rep.getName().equals(repName)) {
					return rep;
				}
			}
		}
		return null;
	}
}
