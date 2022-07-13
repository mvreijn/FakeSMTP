package com.nilhcem.fakesmtp.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nilhcem.fakesmtp.model.EmailModel;

/**
 * Saves emails and notifies components so they can refresh their views with new data.
 *
 * @author Nilhcem
 * @since 1.0
 */
public final class MailLoader extends Observable {

	private static final Logger LOGGER = LoggerFactory.getLogger(MailLoader.class);
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	// This can be a static variable since it is Thread Safe
	private static final Pattern SUBJECT_PATTERN = Pattern.compile("^Subject: (.*)$");
	private static final Pattern FROM_PATTERN = Pattern.compile("^From: (.*)$");
	private static final Pattern RCPT_TO_PATTERN = Pattern.compile("^To: (.*)$");
	private static final Pattern WHEN_PATTERN = Pattern.compile("^Date: (.*)$");

	private final SimpleDateFormat fileDateFormat = new SimpleDateFormat("ddMMyyhhmmssSSS");
	private final SimpleDateFormat mailDateFormat = new SimpleDateFormat("EEE, d MMM yyyy hh:mm:ss Z");

	/**
	 * Loads all emails from the file system and notifies observers.
	 *
	 * @param filePath the save path containing the email files.
	 * @see com.nilhcem.fakesmtp.gui.MainPanel#addObservers to see which observers will be notified
	 */
	public void loadEmailsAndNotify(String filePath) 
	{
		Path pth = Paths.get(filePath);
		try
		{
			Files.walk(pth)
				.filter(Files::isRegularFile)
				.forEach(e -> readEmailFromFile(e.toFile()));
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void readEmailFromFile(File eml)
	{
		LOGGER.info("Loading email {}", eml.getName());
		InputStream data;
		try
		{
			data = new FileInputStream(eml);
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			// Skip this file
			return;
		}
		String mailContent = convertStreamToString(data);
		LOGGER.trace("Loaded email: \"{}\"", mailContent);

		String to = getRecipientFromStr(mailContent);
		String from = getSenderFromStr(mailContent);
		String subject = getSubjectFromStr(mailContent);
		String dateStr = getDateFromStr(mailContent);

		LOGGER.debug("Email: {} from {} to {} ", subject, from, to);

		// We move everything that we can move outside the synchronized block to limit the impact
		EmailModel model = new EmailModel();
		model.setFrom(from);
		model.setTo(to);
		model.setSubject(subject);
		model.setReceivedDate(parseDateString(dateStr, eml.getName()));
		model.setEmailStr(mailContent);

		synchronized (getLock()) 
		{
			model.setFilePath(eml.getAbsolutePath());

			setChanged();
			notifyObservers(model);
		}
		
	}

	private Date parseDateString( String dateStr, String name )
	{
		try
		{
			Date msgDate = mailDateFormat.parse(dateStr);
			return msgDate;
		}
		catch (ParseException e)
		{
			LOGGER.info("Content date parsing failed, trying filename", e);
			try
			{
				Date msgDate = fileDateFormat.parse(name);
				return msgDate;
			}
			catch (ParseException e1)
			{
				LOGGER.info("Filename date parsing failed", e);
			}
		}
		LOGGER.warn("Parsing failed, returning now()");
		return new Date();
	}

	/**
	 * Returns a lock object.
	 * <p>
	 * This lock will be used to make the application thread-safe, and
	 * avoid receiving and deleting emails in the same time.
	 * </p>
	 *
	 * @return a lock object <i>(which is actually the current instance of the {@code MailLoader} object)</i>.
	 */
	public Object getLock() {
		return this;
	}

	/**
	 * Converts an {@code InputStream} into a {@code String} object.
	 * <p>
	 * The method will not copy the first 4 lines of the input stream.<br>
	 * These 4 lines are SubEtha SMTP additional information.
	 * </p>
	 *
	 * @param is the InputStream to be converted.
	 * @return the converted string object, containing data from the InputStream passed in parameters.
	 */
	private String convertStreamToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder();

		String line;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line).append(LINE_SEPARATOR);
			}
		} catch (IOException e) {
			LOGGER.error("", e);
		}
		return sb.toString();
	}

	/**
	 * Gets the subject from the email data passed in parameters.
	 *
	 * @param data a string representing the email content.
	 * @return the subject of the email, or an empty subject if not found.
	 */
	private String getSubjectFromStr(String data) {
		try {
			BufferedReader reader = new BufferedReader(new StringReader(data));

			String line;
			while ((line = reader.readLine()) != null) {
				 Matcher matcher = SUBJECT_PATTERN.matcher(line);
				 if (matcher.matches()) {
					 return matcher.group(1);
				 }
			}
		} catch (IOException e) {
			LOGGER.error("", e);
		}
		return "";
	}

	/**
	 * Gets the recipient from the email data passed in parameters.
	 *
	 * @param data a string representing the email content.
	 * @return the recipient of the email, or an empty recipient if not found.
	 */
	private String getRecipientFromStr(String data) {
		try {
			BufferedReader reader = new BufferedReader(new StringReader(data));

			String line;
			while ((line = reader.readLine()) != null) {
				 Matcher matcher = RCPT_TO_PATTERN.matcher(line);
				 if (matcher.matches()) {
					 return matcher.group(1);
				 }
			}
		} catch (IOException e) {
			LOGGER.error("", e);
		}
		return "";
	}

	/**
	 * Gets the sender from the email data passed in parameters.
	 *
	 * @param data a string representing the email content.
	 * @return the sender of the email, or an empty sender if not found.
	 */
	private String getSenderFromStr(String data) {
		try {
			BufferedReader reader = new BufferedReader(new StringReader(data));

			String line;
			while ((line = reader.readLine()) != null) {
				 Matcher matcher = FROM_PATTERN.matcher(line);
				 if (matcher.matches()) {
					 return matcher.group(1);
				 }
			}
		} catch (IOException e) {
			LOGGER.error("", e);
		}
		return "";
	}

	/**
	 * Gets the date from the email data passed in parameters.
	 *
	 * @param data a string representing the email content.
	 * @return the send date of the email, or an empty date if not found.
	 */
	private String getDateFromStr(String data) {
		try {
			BufferedReader reader = new BufferedReader(new StringReader(data));

			String line;
			while ((line = reader.readLine()) != null) {
				 Matcher matcher = WHEN_PATTERN.matcher(line);
				 if (matcher.matches()) {
					 return matcher.group(1);
				 }
			}
		} catch (IOException e) {
			LOGGER.error("", e);
		}
		return "";
	}
}
