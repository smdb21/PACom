package org.proteored.pacom.utils;

import org.proteored.miapeapi.interfaces.Software;
import org.proteored.pacom.gui.MainFrame;

public class PACOMSoftware implements Software {
	private static PACOMSoftware instance;

	private PACOMSoftware() {

	}

	public static PACOMSoftware getInstance() {
		if (instance == null) {
			instance = new PACOMSoftware();
		}
		return instance;
	}

	@Override
	public int getId() {
		return -1;
	}

	@Override
	public String getVersion() {
		return MainFrame.getVersion().toString();
	}

	@Override
	public String getName() {
		return "PACOM";
	}

	@Override
	public String getManufacturer() {
		return "ProteoRed + TSRI";
	}

	@Override
	public String getModel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		return "The PACOM tool is a standalone tool that allows: to obtain graphical representations of qualitative information from MIAPE MSI documents; to export your MIAPE-compliant reports to standard data files (currently to PRIDE XML).";
	}

	@Override
	public String getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getComments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCatalogNumber() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getURI() {
		return "http://github.com/smdb21/PACom";
	}

	@Override
	public String getCustomizations() {
		// TODO Auto-generated method stub
		return null;
	}

}
