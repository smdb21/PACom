package org.proteored.pacom.analysis.exporters;

import org.proteored.miapeapi.experiment.model.sort.ProteinGroupComparisonType;
import org.proteored.pacom.analysis.util.DataLevel;

public interface ExporterManager {

	public boolean isDecoyHitsIncluded();

	public boolean isFDRApplied();

	public boolean showPeptides();

	public boolean showBestPeptides();

	public boolean showBestProteins();

	public boolean retrieveFromUniprotKB();

	public boolean showGeneInfo();

	public boolean isDistinguishModifiedPeptides();

	/**
	 * @return
	 */
	public ProteinGroupComparisonType getComparisonType();

	public DataLevel getDataLevel();

}
