package net.zeddev.zedlog.logger.impl;
/* Copyright (C) 2013  Zachary Scott <zscott.dev@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import net.zeddev.litelogger.Logger;
import net.zeddev.zedlog.logger.AbstractDataLogger;
import net.zeddev.zedlog.logger.DataLogger;
import net.zeddev.zedlog.logger.DataLoggerObserver;
import net.zeddev.zedlog.logger.LogEntry;
import static net.zeddev.zedlog.util.Assertions.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * A collection of multiple {@code DataLogger}'s.
 *
 * @author Zachary Scott <zscott.dev@gmail.com>
 */
public final class CompositeDataLogger extends AbstractDataLogger implements DataLoggerObserver {

	private final Logger logger = Logger.getLogger(this);

	// each log entry made by the children loggers
	private final List<LogEntry> logEntries = new ArrayList<>();

	private final List<DataLogger> loggers = new ArrayList<>();

	// the output file to write log files
	private File logFile = null;

	// the xml document used to log data logger entries
	private Document xmlLog = null;
	
	/** Creates a new {@code CompositeDataLogger}. */
	public CompositeDataLogger() {
		super();
	}

	@Override
	public void shutdown() {
		super.shutdown();
		
		// flush the log file
		try {
			
			if (getLogFile() != null)
				flushXmlLog();
		
		} catch (Exception ex) {
			logger.error("Failed to write xml log file!", ex);	
		}
		
		logger.debug("CompositeLogger shutdown.");

	}

	@Override
	public String type() {
		return "CompositeLogger";
	}

	/**
	 * Returns the {@code DataLogger} at the given index in the
	 * {@code CompositeDataLogger}.
	 *
	 * @param index The index of the logger to get.
	 * @return The {@code DataLogger} at the given index.
	 */
	public DataLogger getLogger(int index) {

		require(index >= 0 && index < loggers.size());

		synchronized (loggers) {
			return loggers.get(index);
		}

	}

	/**
	 * Adds a new {@code DataLogger}.
	 *
	 * @param logger The logger to add.
	 * @throws IOException If exception occurs while establishing log file.
	 */
	public void addLogger(final DataLogger logger) throws IOException {

		requireNotNull(logger);

		synchronized (loggers) {

			logger.setRecording(isRecording());

			logger.addObserver(this);
			loggers.add(logger);
			
		}

	}

	/**
	 * Removes the given {@code DataLogger}.
	 *
	 * @param logger The logger to be removed.
	 * @throws IOException If error occured when closing log file for the logger.
	 */
	public void removeLogger(final DataLogger logger) throws IOException {

		requireNotNull(logger);

		synchronized (loggers) {

			logger.removeObserver(this);
			loggers.remove(logger);

		}

	}

	/**
	 * Removes the {@code DataLogger} at the given index.
	 *
	 * @param index The index of the {@code DataLogger} to be removed.
	 */
	public void removeLogger(int index) {
		require(index >= 0 && index < loggers.size());
		loggers.remove(index);
	}
	
	/** Whether the {@code CompopsiteDataLogger} contains the given logger. */
	public boolean containsLogger(DataLogger logger) {
		requireNotNull(logger);
		return loggers.contains(logger);
	}

	/**
	 * Clears all log entries and the log files.
	 *
	 * @throws IOException If an error occurs when the log files are closed.
	 */
	public void clearAll() throws IOException {
		logEntries.clear();
	}

	/**
	 * Returns the children {@code DataLogger}s.
	 *
	 * @return The children {@code DataLogger}s.
	 */
	public List<DataLogger> getLoggers() {
		return new ArrayList<>(loggers);
	}
	
	/**
	 * Returns a list of all entries made by children loggers.
	 *
	 * @return A list of all entries made by children loggers.
	 */
	public List<LogEntry> logEntries() {
		return new ArrayList<>(logEntries);
	}

	/**
	 * Sets the log file to which log entries are stored.
	 *
	 * @param file The log file (must be a valid filename and
	 * cannot be {@code null}).
	 */
	public void setLogFile(File file) throws IOException {
		
		requireNotNull(file);
		
		if (file.exists()) {
			require(file.isFile());
		} else {
			file.createNewFile();
		}
		
		this.logFile = file;
		
	}
	
	/** Returns the xml log file. */
	public File getLogFile() {
		return logFile;
	}
	
	// returns the xml document used to log data logger entries
	private Document getXmlLog() throws ParserConfigurationException {

		// create xml document object if does not already exist
		if (xmlLog == null) {
			
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			
			xmlLog = docBuilder.newDocument();
			
			// create the root element in the document
			Element root = xmlLog.createElement("zedlog");
			xmlLog.appendChild(root);
			
			// create the loggers element/section
			Element loggers = xmlLog.createElement("loggers");
			root.appendChild(loggers);
			
			// create the root element in the document
			Element entries = xmlLog.createElement("entries");
			root.appendChild(entries);
			
		}
		
		return xmlLog;
		
	}

	/**
	 * Opens the given log file and reads the log entries.
	 * Implies clearing of the currently held log entries.
	 *
	 * @param file The file in which to read (file must exist and
	 * cannot be {@code null}).
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws Exception
	 */
	public void openLogFile(File file)
			throws ParserConfigurationException, SAXException,
			ClassNotFoundException, InstantiationException, 
			IllegalAccessException, Exception {

		requireNotNull(file);
		require(file.exists());
				
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		
		Document doc = docBuilder.parse(file);
		doc.getDocumentElement().normalize();
		
		// add each data logger
		NodeList loggerNodes = doc.getElementsByTagName("logger");
		for (int i = 0; i < loggerNodes.getLength(); i++) {
			if (loggerNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
				
				Element loggerElement = (Element) loggerNodes.item(i);
				
				String loggerType = loggerElement.getAttribute("type");
				addLogger(
					DataLoggers.newDataLogger(loggerType)
				);
				
			}
			
		}
		
		// handle each log entry
		NodeList entries = doc.getElementsByTagName("entry");
		for (int i = 0; i < entries.getLength(); i++) {
			if (entries.item(i).getNodeType() == Node.ELEMENT_NODE) {
				
				Element entry = (Element) entries.item(i);
				
				LogEntry logEntry = new LogEntry();
				logEntry.fromXML(entry);

				notifyLog(null, logEntry);
				
			}
			
		}
		
	}
	
	// updates the list of loggers in the given xml element 
	private void updateLoggersInXmlLog(Element parent) throws Exception {
		
		requireNotNull(parent != null);
		assert requireEquals(parent.getTagName(), "loggers");
		
		// the set of data loggers currently in the XML document
		Map<String, Element> dataLoggerSet = new HashMap<>();
		
		// populate the data logger set
		NodeList loggerNodes = parent.getElementsByTagName("logger");
		for (int i = 0; i < loggerNodes.getLength(); i++) {
			if (loggerNodes.item(i) instanceof Element) { // weed out all non-logger nodes
				Element loggerElement = (Element) loggerNodes.item(i);
				
				dataLoggerSet.put(
					loggerElement.getAttribute("type"),
					loggerElement
				);
				
			}
			
		}
		
		// remove loggers not longer in the composite logger
		for (String loggerType : dataLoggerSet.keySet()) {
			
			DataLogger logger = DataLoggers.newDataLogger(loggerType); 
				// NOTE actually returns cached version 
			
			if (!loggers.contains(logger))
				parent.removeChild(dataLoggerSet.get(loggerType));
			
		}
		
		// update the logger elements not in the data logger set
		for (DataLogger logger : loggers) {
			
			if (!dataLoggerSet.containsKey(logger.type())) {
			
				Element loggerElement = getXmlLog().createElement("logger");
				loggerElement.setAttribute("type", logger.type());
				
				parent.appendChild(loggerElement);
				
			}
			
		}
		
	}
	
	// flushes the XML log document to the disk
	private void flushXmlLog() throws Exception {
	
		requireNotNull(getLogFile());
		
		Document doc = getXmlLog();
		checkNotNull(doc);
		
		Element root = doc.getDocumentElement();
		checkNotNull(root);		

		// update the loggers list in 
		Element loggers = firstXmlElement(root, "loggers");
		updateLoggersInXmlLog(loggers);
		
		// the transformer which will update the XML source on disk
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		
		// the XML input/output objects
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(getLogFile());
		
		// update the XML file
		transformer.transform(source, result);
		
	}
	
	// the number of log entries before flushing to disk
	private static final int LOG_FLUSH_THRESHOLD = 10;
	private int logEntrysSinceFlush = 0; // the current count of log entries since last flush
	
	// Returns the first XML element with the given name, in the given parent element
	private static Element firstXmlElement(Element parent, String tagname) {
	
		requireNotNull(parent);
		requireNotNull(tagname);
		requireNotEquals(tagname, "");
		
		NodeList elements = parent.getElementsByTagName(tagname);
		check(elements.getLength() > 0);
		// TODO check properly
		
		Element first = (Element) elements.item(0);
		ensureNotNull(first);
		
		return first;
		
	}
	
	// writes the log entry to XML
	private void writeXmlLogEntry(final LogEntry logEntry) {

		requireNotNull(logEntry);
		
		// dont write to file if not set
		if (getLogFile() != null) {
		
			try {
				
				Document doc = getXmlLog();
				checkNotNull(doc);
				
				Element root = doc.getDocumentElement();
				checkNotNull(root);
				
				Element entries = firstXmlElement(root, "entries");
				checkNotNull(entries);
				
				// encode the log entry
				logEntry.toXML(entries);
				logEntrysSinceFlush++;
				
				// flush log to disk
				if (logEntrysSinceFlush >= LOG_FLUSH_THRESHOLD) {
					flushXmlLog();
					logEntrysSinceFlush = 0;
				}
				
			} catch (Exception ex) {
				 logger.error("Failed encode log entry to XML!", ex);
			}
			
		}
		
	}

	@Override
	public void notifyLog(final DataLogger logger, final LogEntry logEntry) {

		requireNotNull(logEntry);

		if (isRecording()) {

			logEntries.add(logEntry); // TODO optimise using fast(er) list implementation

			writeXmlLogEntry(logEntry);

			notifyDataLoggerObservers(logger, logEntry);

		}

	}

	@Override
	public final void setRecording(boolean recording) {

		synchronized (loggers) {

			// set for all children
			for (DataLogger logger : loggers)
				logger.setRecording(recording);

			super.setRecording(recording);

		}

	}

	@Override
	public String toString() {

		final StringBuilder log = new StringBuilder();
		DataLogger lastLogger = null;

		for (LogEntry logEntry : logEntries) {

			// add newline to separate different logger messages
			if (lastLogger == null) {
				lastLogger = logEntry.getParent();
			} else if (logger != lastLogger) {

				// dont append if already a newline
				if (log.charAt(log.length()-1) != '\n')
					log.append("\n");

				lastLogger = logEntry.getParent();

			}

			log.append(logEntry);

		}

		return log.toString();

	}

}