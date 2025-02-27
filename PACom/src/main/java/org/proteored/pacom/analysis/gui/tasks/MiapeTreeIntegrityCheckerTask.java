package org.proteored.pacom.analysis.gui.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;
import org.proteored.miapeapi.cv.ControlVocabularyManager;
import org.proteored.miapeapi.exceptions.IllegalMiapeArgumentException;
import org.proteored.miapeapi.exceptions.MiapeDatabaseException;
import org.proteored.miapeapi.exceptions.MiapeSecurityException;
import org.proteored.miapeapi.experiment.model.Replicate;
import org.proteored.miapeapi.interfaces.msi.MiapeMSIDocument;
import org.proteored.miapeapi.xml.msi.MIAPEMSIXmlFile;
import org.proteored.pacom.analysis.conf.jaxb.CPExperiment;
import org.proteored.pacom.analysis.conf.jaxb.CPExperimentList;
import org.proteored.pacom.analysis.conf.jaxb.CPMSI;
import org.proteored.pacom.analysis.conf.jaxb.CPReplicate;
import org.proteored.pacom.analysis.gui.Miape2ExperimentListDialog;
import org.proteored.pacom.analysis.util.FileManager;
import org.proteored.pacom.gui.tasks.OntologyLoaderTask;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.THashSet;

public class MiapeTreeIntegrityCheckerTask extends SwingWorker<String, Void> {
	private static Logger log = Logger.getLogger("log4j.logger.org.proteored");

	public static String INTEGRITY_ERROR = "integrity error";
	public static String INTEGRITY_OK = "integrity_ok";
	public static String INTEGRITY_START = "integrity_start";
	private final CPExperimentList expList;
	private final Miape2ExperimentListDialog parent;

	private final boolean processInParallel;

	public MiapeTreeIntegrityCheckerTask(Miape2ExperimentListDialog miape2ExperimentListDialog,
			CPExperimentList expList, boolean processInParallel) {
		this.expList = expList;
		parent = miape2ExperimentListDialog;
		this.processInParallel = processInParallel;
	}

	@Override
	protected String doInBackground() {
		if (expList != null) {
			try {
				parent.appendStatus("Checking integrity of Inspection Project: '" + expList.getName() + "'");
				parent.jProgressBar.setIndeterminate(true);
				checkIntegrity();
				parent.appendStatus("Project: '" + expList.getName() + "' is OK.");
			} catch (final IllegalMiapeArgumentException e) {
				return e.getMessage();
			}
		}
		return null;
	}

	@Override
	protected void done() {
		if (!isCancelled()) {

			try {
				final String message = this.get();
				if (message != null)
					firePropertyChange(INTEGRITY_ERROR, null, message);
				else
					firePropertyChange(INTEGRITY_OK, null, null);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			} catch (final ExecutionException e) {
				e.printStackTrace();
			}

		}
	}

	private void checkIntegrity() {
		final List<String> databaseNames = new ArrayList<String>();
		final TIntArrayList listofNonExistingFiles = new TIntArrayList();
		final Set<String> uniqueIds = new THashSet<String>();
		if (expList != null) {
			uniqueIds.add(expList.getName());
			final List<CPExperiment> cpExperiments = expList.getCPExperiment();
			if (cpExperiments != null) {
				final int total = cpExperiments.size();
				int n = 1;
				for (final CPExperiment cpExperiment : cpExperiments) {
					if (uniqueIds.contains(cpExperiment.getName())) {
						throw new IllegalMiapeArgumentException("'" + cpExperiment.getName()
								+ "' node name is repeated. You can not name different nodes with the same name."
								+ " Please rename one of them with a unique name");
					}
					uniqueIds.add(cpExperiment.getName());
					setProgress(n * 100 / total);
					n++;
					final boolean curated = cpExperiment.isCurated();
					if (cpExperiment.getCPReplicate() != null) {
						for (final CPReplicate cpReplicate : cpExperiment.getCPReplicate()) {
							final String uniqueName = cpExperiment.getName() + cpReplicate.getName();
							if (uniqueIds.contains(uniqueName)) {
								throw new IllegalMiapeArgumentException("'" + cpReplicate.getName()
										+ "' node name is repeated. You can not name different nodes with the same name."
										+ " Please rename one of them with a unique name");
							}
							uniqueIds.add(uniqueName);
							final List<String> repScoreNames = new ArrayList<String>();

							if (true) {
								continue;
							}

							// key=MIAPEMSI_ID - value=List<scoreNames>:
							final TIntObjectHashMap<List<String>> replicateScoreNames = new TIntObjectHashMap<List<String>>();
							// just check if more than one MIAPE MSI is in a
							// replicate
							if (!cpReplicate.getCPMSIList().getCPMSI().isEmpty())
								for (final CPMSI cpMSI : cpReplicate.getCPMSIList().getCPMSI()) {
									final List<MiapeMSIDocument> miapeMSIs = new ArrayList<MiapeMSIDocument>();
									final MiapeMSIDocument miapeMSI = getMIAPEMSIFromFile(cpMSI, curated);
									if (miapeMSI != null) {
										miapeMSIs.add(miapeMSI);
										// create a replicate with one miape msi
										final Replicate replicate = new Replicate(cpReplicate.getName(),
												cpExperiment.getName(), null, miapeMSIs, null, true, false,
												Integer.MAX_VALUE, // para
												// que
												// no
												// coja
												// p�ptidos
												// y
												// vaya
												// m�s
												// r�pido,
												// ya
												// que
												// aqu�
												// no
												// se
												// necesitan
												OntologyLoaderTask.getCvManager(), processInParallel);

										// check databaseNames
										final Set<String> databases = replicate.getDifferentSearchedDatabases();
										if (databases != null) {
											for (final String database : databases) {
												if (!databaseNames.contains(database))
													databaseNames.add(database);
											}
										}

										final List<String> peptideScoreNames = replicate.getPeptideScoreNames();
										if (repScoreNames.isEmpty()) {
											for (final String scoreName : peptideScoreNames) {
												repScoreNames.add(scoreName);
											}
											Collections.sort(repScoreNames);
										} else {
											final String message = "<html>Some datasets seems to have been searched by different search engines"
													+ " in level 2 node " + "'" + cpExperiment.getName()
													+ "'<br>If you continue, you will not be able to apply a valid FDR threshold at level 2."
													+ "<br>Do you want to continue?</html>";

											if (repScoreNames.size() != peptideScoreNames.size()) {
												throw new IllegalMiapeArgumentException(message);
											}
											Collections.sort(peptideScoreNames);
											for (int i = 0; i < peptideScoreNames.size(); i++) {
												if (!peptideScoreNames.get(i).equals(repScoreNames.get(i)))
													throw new IllegalMiapeArgumentException(message);
											}

										}
										if (!replicateScoreNames.isEmpty()) {
											for (final int miape_msi_id : replicateScoreNames.keys()) {
												final List<String> scores = replicateScoreNames.get(miape_msi_id);
												if (scores != null && !scores.isEmpty())
													if (!hasOneElementInCommon(scores, peptideScoreNames))
														throw new IllegalMiapeArgumentException("<html>Level 2 node '"
																+ cpReplicate.getName() + "' and '" + miape_msi_id
																+ "' from level 1 node '" + expList.getName()
																+ "' seems to have been searched by different search engines.<br>"
																+ "If you continue, you will not be able to apply a valid FDR threshold."
																+ "<br>Do you want to continue?</html>");
											}
										}
										replicateScoreNames.put(cpMSI.getId(), peptideScoreNames);
									} else {
										listofNonExistingFiles.add(cpMSI.getId());
									}
								}
						}
					}
				}
			}
		}
		// if there is more than one database name, throw a warning
		if (databaseNames.size() > 1) {
			throw new IllegalMiapeArgumentException("<html><b>Warning:</b><br>"
					+ "Not all datasets have been searched with the same protein database:<br>" + "'"
					+ getCSVStringFromStringList(databaseNames) + "'<br>"
					+ "If you continue it is possible that identifiers from different databases<br>"
					+ "will not have the same format and the comparisons will not be performed correctly." + "</html>");
		}
		if (!listofNonExistingFiles.isEmpty()) {
			// Check if they are in the UnnatendedRetriever waiting to be
			// dowloaded
			final TIntArrayList miapesNotBeingDownloaded = new TIntArrayList(listofNonExistingFiles);

			String message = "<html><b>Warning:</b><br>" + "Some datasets are not stored locally in "
					+ FileManager.getMiapeDataPath() + ".<br>" + "Some datasets in the comparison project";
			if (!miapesNotBeingDownloaded.isEmpty()) {
				message = message + " ('" + getCSVStringFromIntegerList(miapesNotBeingDownloaded)
						+ "') are not being downloaded from server.";
			}

			throw new IllegalMiapeArgumentException(message);
		}
		// check names
		checkExperimentListNames(expList);

	}

	private String getCSVStringFromIntegerList(TIntArrayList list) {
		String ret = "";
		if (list != null) {
			for (final int string : list.toArray()) {
				if (!"".equals(ret))
					ret = ret + ", ";
				ret = ret + string;
			}
		}
		return ret;
	}

	private String getCSVStringFromStringList(List<String> list) {
		String ret = "";
		if (list != null) {
			for (final Object string : list) {
				if (!"".equals(ret))
					ret = ret + ", ";
				ret = ret + string;
			}
		}
		return ret;
	}

	private void checkExperimentListNames(CPExperimentList cpExpList) throws IllegalMiapeArgumentException {
		final Set<String> experimentNames = new THashSet<String>();
		if (cpExpList != null) {
			final List<CPExperiment> experiments = cpExpList.getCPExperiment();
			if (experiments != null) {
				boolean thereIsAReplicate = false;
				for (final CPExperiment cpExperiment : experiments) {
					final String expName = cpExperiment.getName();
					if (experimentNames.contains(expName))
						throw new IllegalMiapeArgumentException(
								"<html>Error in comparison project tree.<br>Level 1 node name duplicated: '" + expName
										+ "'.<br>If two experiments have the same name, some charts can show not valid data!.</html>");
					else
						experimentNames.add(expName);
					final List<CPReplicate> replicates = cpExperiment.getCPReplicate();
					final Set<String> replicateNames = new THashSet<String>();
					if (replicates != null) {
						for (final CPReplicate cpReplicate : replicates) {
							thereIsAReplicate = true;
							final String repName = cpReplicate.getName();
							if (replicateNames.contains(repName)) {
								throw new IllegalMiapeArgumentException(
										"<html>Error in comparison project tree.<br>Level 2 node name duplicated: '"
												+ repName + "' in level 1 node: '" + expName + "'.</html>");
							} else {
								replicateNames.add(repName);
							}
							if (cpReplicate.getCPMSIList() == null || cpReplicate.getCPMSIList().getCPMSI().isEmpty()) {
								throw new IllegalMiapeArgumentException("<html>Error in comparison project tree.<br>"
										+ "Level 2 node '" + repName + "' is not associated with any dataset.<br>"
										+ "In order to associate a dataset to it, just select it, and <br>double click on one dataset from the left tree</html>");
							}
						}
					}
				}
				if (!thereIsAReplicate) {
					throw new IllegalMiapeArgumentException(
							"<html>Error in comparison project tree.<br>At least a level 2 node is needed.</html>");
				}
			} else {
				throw new IllegalMiapeArgumentException(
						"<html>Error in project tree.<br>At least a level 1 and a level 2 node is needed.</html>");
			}
		}
	}

	private boolean hasOneElementInCommon(List<String> list1, List<String> list2) {

		for (final String string1 : list1) {
			for (final String string2 : list2) {
				if (string1.equals(string2))
					return true;
			}
		}
		return false;
	}

	private MiapeMSIDocument getMIAPEMSIFromFile(CPMSI cpMSI, boolean curated) {

		File file = null;
		if (cpMSI.isLocal() == null || !cpMSI.isLocal()) {
			file = new File(FileManager.getMiapeDataPath() + cpMSI);
		} else if (cpMSI.isLocal() != null && cpMSI.isLocal()) {
			if (!curated) {
				file = new File(FileManager.getMiapeMSIXMLFileLocalPathFromMiapeInformation(cpMSI));
			} else {
				file = new File(FileManager.getMiapeMSICuratedXMLFilePathFromMiapeInformation(
						cpMSI.getLocalProjectName(), cpMSI.getName()));
			}
		}
		if (file == null || !file.exists())
			return null;
		MiapeMSIDocument ret;
		final MIAPEMSIXmlFile msiFile = new MIAPEMSIXmlFile(file);
		final ControlVocabularyManager cvManager = OntologyLoaderTask.getCvManager();
		msiFile.setCvUtil(cvManager);
		try {
			ret = msiFile.toDocument();

			return ret;
		} catch (final MiapeDatabaseException e) {
			log.warn(e.getMessage());
			e.printStackTrace();
		} catch (final MiapeSecurityException e) {
			log.warn(e.getMessage());
			e.printStackTrace();
		} catch (final Exception e) {
			log.warn(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
}
