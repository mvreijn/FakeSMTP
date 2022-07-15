package com.nilhcem.fakesmtp.gui.info;

import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JOptionPane;

import com.nilhcem.fakesmtp.core.Configuration;
import com.nilhcem.fakesmtp.core.I18n;
import com.nilhcem.fakesmtp.model.UIModel;

/**
 * Button to follow the email file directory for changes
 *
 * @author mvreijn
 * @since 2.1
 */
public final class FollowFilesButton extends Observable implements Observer {
	private final I18n i18n = I18n.INSTANCE;

	private final JButton button = new JButton(i18n.get("files.follow"));

	/**
	 * Creates a start button to start the SMTP server.
	 * <p>
	 * If the user selects a wrong port before starting the server, the method will display an error message.
	 * </p>
	 */
	public FollowFilesButton() {
		button.addActionListener(e -> toggleButton());
	}

	/**
	 * Switches the text inside the button and calls the PortTextField observer to enable/disable the port field.
	 *
	 * @see PortTextField
	 */
	public void toggleButton() {
		try {
			UIModel.INSTANCE.toggleFollowButton();
		} catch (RuntimeException re) {
			displayError(String.format(i18n.get("files.err.default"), re.getMessage()));
		}

		if (UIModel.INSTANCE.isFollowing()) {
			button.setText(i18n.get("files.following"));
		} else {
			button.setText(i18n.get("files.follow"));
		}
		setChanged();
		notifyObservers();
	}

	/**
	 * Returns the JButton object.
	 *
	 * @return the JButton object.
	 */
	public JButton get() {
		return button;
	}

	/**
	 * Displays a message dialog displaying the error specified in parameter.
	 *
	 * @param error a string representing the error which will be displayed in a message dialog.
	 */
	private void displayError(String error) {
		JOptionPane.showMessageDialog(button.getParent(), error,
			String.format(i18n.get("startsrv.err.title"), Configuration.INSTANCE.get("application.name")),
			JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof PortTextField) {
			toggleButton();
		}
	}
}
