package com.nilhcem.fakesmtp.gui.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nilhcem.fakesmtp.gui.MainFrame;
import com.nilhcem.fakesmtp.model.UIModel;
import com.nilhcem.fakesmtp.server.MailLoader;

/**
 * Implements the Load messages action.
 *
 * @author mvreijn
 * @since 2.2
 */
public class LoadActionListener implements ActionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoadActionListener.class);

	private final MainFrame mainFrame;

	/**
	 * MainFrame is used for updating the message list.
	 *
	 * @param mainFrame MainFrame window that will be closed.
	 */
	public LoadActionListener(MainFrame mainFrame) {
		this.mainFrame = mainFrame;
	}

	@Override
	public void actionPerformed(ActionEvent e) 
	{
		String filePath = UIModel.INSTANCE.getSavePath();
		LOGGER.info("Loading emails from {}", filePath);
		MailLoader loader = UIModel.INSTANCE.getMailLoader();
		loader.loadEmailsAndNotify(filePath);
	}
}
