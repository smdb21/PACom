package org.proteored.pacom.utils;

import java.io.File;

public class MiapeExtractionResult {
	private final int miapeExtractionTaskIdentifier;
	private Integer miapeMS_Identifier;
	private Integer miapeMSI_Identifier;
	private String directLinkText;
	private File directLinkToMIAPEMS;
	private File directLinkToMIAPEMSI;
	private String errorMessage;
	private long milliseconds;

	public MiapeExtractionResult(int id, String errorMeString) {
		this.errorMessage = errorMeString;
		this.miapeExtractionTaskIdentifier = id;
	}

	public MiapeExtractionResult(int id) {
		this.miapeExtractionTaskIdentifier = id;
	}

	public Integer getMiapeMS_Identifier() {
		return miapeMS_Identifier;
	}

	public void setMiapeMS_Identifier(int miapeMS_Identifier) {
		this.miapeMS_Identifier = miapeMS_Identifier;
	}

	public Integer getMiapeMSI_Identifier() {
		return miapeMSI_Identifier;
	}

	public void setMiapeMSI_Identifier(int miapeMSI_Identifier) {
		this.miapeMSI_Identifier = miapeMSI_Identifier;
	}

	public String getDirectLinkText() {
		return directLinkText;
	}

	public void setDirectLinkText(String directLinkText) {
		this.directLinkText = directLinkText;
	}

	public File getDirectLinkToMIAPEMS() {
		return directLinkToMIAPEMS;
	}

	public void setDirectLinkToMIAPEMS(File directLinkToMIAPEMS) {
		this.directLinkToMIAPEMS = directLinkToMIAPEMS;
	}

	public File getDirectLinkToMIAPEMSI() {
		return directLinkToMIAPEMSI;
	}

	public void setDirectLinkToMIAPEMSI(File directLinkToMIAPEMSI) {
		this.directLinkToMIAPEMSI = directLinkToMIAPEMSI;
	}

	public int getMiapeExtractionTaskIdentifier() {
		return miapeExtractionTaskIdentifier;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public long getMilliseconds() {
		return milliseconds;
	}

	public void setMilliseconds(long milliseconds) {
		this.milliseconds = milliseconds;
	}

}