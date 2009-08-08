package net.sourceforge.adela.tests;

import java.applet.Applet;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class ClientTestApplet extends Applet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2475051385290611190L;

	private ClientTestPanel ctp;

	@Override
	public void init() {
		super.init();
		this.ctp = new ClientTestPanel();
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		add(ctp, gbc);
		setPreferredSize(new Dimension(640, 480));
	}
}
