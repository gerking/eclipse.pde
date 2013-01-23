/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.icu.text.MessageFormat;

/**
 * This task can be used to convert all reports generated by the apitooling.analysis task
 * into html reports.
 */
public class AnalysisReportConversionTask extends Task {
	static final class Problem {
		String message;
		int severity;
		public Problem(String message, int severity) {
			this.message = message;
			this.severity = severity;
		}
		public String getHtmlMessage() {
			StringBuffer buffer = new StringBuffer();
			char[] chars = this.message.toCharArray();
			for (int i = 0, max = chars.length; i < max; i++) {
				char character = chars[i];
				switch(character) {
					case '<':
						buffer.append("&lt;"); //$NON-NLS-1$
						break;
					case '>':
						buffer.append("&gt;"); //$NON-NLS-1$
						break;
					case '&':
						buffer.append("&amp;"); //$NON-NLS-1$
						break;
					case '"':
						buffer.append("&quot;"); //$NON-NLS-1$
						break;
					default:
						buffer.append(character);
				}
			}
			return String.valueOf(buffer);
		}
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Problem : ").append(this.message).append(' ').append(this.severity); //$NON-NLS-1$
			return String.valueOf(buffer);
		}
	}
	static final class ConverterDefaultHandler extends DefaultHandler {
		String category;
		boolean debug;
		Report report;

		String currentSkippedBundle;
		List currentSkippedBundleProblems = new ArrayList();

		public ConverterDefaultHandler(boolean debug) {
			this.debug = debug;
		}
		public Report getReport() {
			return this.report;
		}
		public void startElement(String uri, String localName,
				String name, Attributes attributes) throws SAXException {
			if (IApiXmlConstants.ELEMENT_API_TOOL_REPORT.equals(name)) {
				String componentID = attributes.getValue(IApiXmlConstants.ATTR_COMPONENT_ID);
				if (this.debug) {
					System.out.println("component id : " + String.valueOf(componentID)); //$NON-NLS-1$
				}
				this.report = new Report(componentID);
			} else if (IApiXmlConstants.ATTR_CATEGORY.equals(name)) {
				this.category = attributes.getValue(IApiXmlConstants.ATTR_VALUE);
				if (this.debug) {
					System.out.println("category : " + this.category); //$NON-NLS-1$
				}
			} else if (IApiXmlConstants.ELEMENT_API_PROBLEM.equals(name)) {
				String message = attributes.getValue(IApiXmlConstants.ATTR_MESSAGE);
				if (this.debug) {
					System.out.println("problem message : " + message); //$NON-NLS-1$
				}
				int severity = Integer.parseInt(attributes.getValue(IApiXmlConstants.ATTR_SEVERITY));
				if (this.debug) {
					System.out.println("problem severity : " + severity); //$NON-NLS-1$
				}
				this.report.addProblem(this.category, new Problem(message, severity));
			} else if (IApiXmlConstants.ELEMENT_BUNDLE.equals(name)) {
				currentSkippedBundle = attributes.getValue(IApiXmlConstants.ATTR_NAME);
				currentSkippedBundleProblems.clear();
				if (this.debug) {
					System.out.println("Skipped bundle name : " + currentSkippedBundle); //$NON-NLS-1$
				}
			} else if (IApiXmlConstants.ELEMENT_RESOLVER_ERROR.equals(name)){
				String error = attributes.getValue(IApiXmlConstants.ATTR_MESSAGE);
				currentSkippedBundleProblems.add(error);
				if (this.debug) {
					System.out.println("Bundle skipped because : " + error); //$NON-NLS-1$
				}
			}
			if (!IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENTS.equals(name)
					&& !IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENT.equals(name)
					&& !IApiXmlConstants.ELEMENT_API_PROBLEMS.equals(name)
					&& !IApiXmlConstants.ELEMENT_PROBLEM_EXTRA_ARGUMENT.equals(name)
					&& !IApiXmlConstants.ELEMENT_PROBLEM_EXTRA_ARGUMENTS.equals(name)) {
				System.out.println("unknown element : " + String.valueOf(name)); //$NON-NLS-1$
			}
		}
		
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			if (IApiXmlConstants.ELEMENT_BUNDLE.equals(qName)) {
				String[] errors = null;
				if (!currentSkippedBundleProblems.isEmpty()){
					errors = (String[])currentSkippedBundleProblems.toArray(new String[currentSkippedBundleProblems.size()]);
				}
				this.report.addSkippedBundle(currentSkippedBundle, errors);
				currentSkippedBundle = null;
				currentSkippedBundleProblems.clear();
			}
		}
		
	}
	static private final class Report {
		String componentID;
		String link;
		Map skippedBundles;
		Map problemsPerCategories;

		Report(String componentID) {
			this.componentID = componentID;
		}

		/**
		 * Adds an entry for a skipped bundle, if null is provided as the problems
		 * it is assumed that the bundle was skipped because it isn't an API bundle.
		 * 
		 * @param bundleName name of the bundle
		 * @param problems one or more problem messages prevent this bundle from being analyzed, can be <code>null</code> if bundle wasn't API tools enabled
		 */
		public void addSkippedBundle(String bundleName, String[] problems) {
			if (this.skippedBundles == null) {
				this.skippedBundles = new HashMap();
			}
			this.skippedBundles.put(bundleName, problems);
		}
		
		public void addProblem(String category, Problem problem) {
			if (this.problemsPerCategories == null) {
				this.problemsPerCategories = new HashMap();
			}
			List problemsList = (List) this.problemsPerCategories.get(category);
			if (problemsList == null) {
				problemsList = new ArrayList();
				this.problemsPerCategories.put(category, problemsList);
			}
			problemsList.add(problem);
		}

		/**
		 * @return map of string bundle name to String[] problem messages or <code>null</code> if bundle was skipped because it isn't API Tools enabled
		 */
		public Map getSkippedBundles() {
			if (this.skippedBundles == null || this.skippedBundles.size() == 0) {
				return new HashMap(0);
			}
			return skippedBundles;
		}
		
		public Problem[] getProblems(String category) {
			if (this.problemsPerCategories == null) return NO_PROBLEMS;
			List problemsList = (List) this.problemsPerCategories.get(category);
			int size = problemsList == null ? 0 : problemsList.size();
			if (size == 0) {
				return NO_PROBLEMS;
			}
			Problem[] problems = new Problem[size];
			problemsList.toArray(problems);
			return problems;
		}
		public int getProblemSize(String category) {
			if (this.problemsPerCategories == null) return 0;
			List problemsList = (List) this.problemsPerCategories.get(category);
			return problemsList == null ? 0 : problemsList.size();
		}
		
		public boolean isNonApiBundlesReport() {
			return this.componentID == null;
		}
		public void setLink(String link) {
			this.link = link;
		}
	}

	static private final class Summary {
		int apiUsageNumber;
		int bundleVersionNumber;
		int compatibilityNumber;
		String componentID;
		String link;
		
		public Summary(Report report) {
			super();
			this.apiUsageNumber = report.getProblemSize(APIToolsAnalysisTask.USAGE);
			this.bundleVersionNumber = report.getProblemSize(APIToolsAnalysisTask.BUNDLE_VERSION);
			this.compatibilityNumber = report.getProblemSize(APIToolsAnalysisTask.COMPATIBILITY);
			this.componentID = report.componentID;
			this.link = report.link;
		}
		
		public String toString() {
			return MessageFormat.format("{0} : compatibility {1}, api usage {2}, bundle version {3}, link {4}", //$NON-NLS-1$
					new String[] {
						this.componentID,
						Integer.toString(this.compatibilityNumber),
						Integer.toString(this.apiUsageNumber),
						Integer.toString(this.bundleVersionNumber),
						this.link
					});
		}
	}
	static final Problem[] NO_PROBLEMS = new Problem[0];
	boolean debug;

	private String htmlReportsLocation;
	private File htmlRoot;
	private File reportsRoot;
	private String xmlReportsLocation;

	private void dumpFooter(PrintWriter writer) {
		writer.println(Messages.W3C_page_footer);
	}
	private void dumpHeader(PrintWriter writer, Report report) {
		writer.println(
			MessageFormat.format(
				Messages.fullReportTask_apiproblemheader,
				new String[] {
						report.componentID
				}));
		// dump the summary
		writer.println(
			MessageFormat.format(
				Messages.fullReportTask_apiproblemsummary,
				new String[] {
					Integer.toString(report.getProblemSize(APIToolsAnalysisTask.COMPATIBILITY)),
					Integer.toString(report.getProblemSize(APIToolsAnalysisTask.USAGE)),
					Integer.toString(report.getProblemSize(APIToolsAnalysisTask.BUNDLE_VERSION)),
				}));
	}
	private void dumpIndexEntry(int i, PrintWriter writer, Summary summary) {
		if (debug) {
			System.out.println(summary);
		}
		if ((i % 2) == 0) {
			writer.println(
					MessageFormat.format(
						Messages.fullReportTask_indexsummary_even,
						new String[] {
							summary.componentID,
							Integer.toString(summary.compatibilityNumber),
							Integer.toString(summary.apiUsageNumber),
							Integer.toString(summary.bundleVersionNumber),
							summary.link,
						}));
		} else {
			writer.println(
				MessageFormat.format(
					Messages.fullReportTask_indexsummary_odd,
					new String[] {
						summary.componentID,
						Integer.toString(summary.compatibilityNumber),
						Integer.toString(summary.apiUsageNumber),
						Integer.toString(summary.bundleVersionNumber),
						summary.link,
					}));
		}
	}
	private void dumpIndexFile(File reportsRoot, Summary[] summaries, Summary allNonApiBundleSummary) {
		File htmlFile = new File(this.htmlReportsLocation, "index.html"); //$NON-NLS-1$
		PrintWriter writer = null;
		try {
			FileWriter fileWriter = new FileWriter(htmlFile);
			writer = new PrintWriter(new BufferedWriter(fileWriter));
			if (allNonApiBundleSummary != null) {
				writer.println(
					MessageFormat.format(
						Messages.fullReportTask_indexheader,
						new String[] {
							NLS.bind(Messages.fullReportTask_nonApiBundleSummary, allNonApiBundleSummary.link)
						}));
			} else {
				writer.println(
					MessageFormat.format(
						Messages.fullReportTask_indexheader,
						new String[] {
							Messages.fullReportTask_apiBundleSummary
						}));
			}
			Arrays.sort(summaries, new Comparator() {
				public int compare(Object o1, Object o2) {
					Summary summary1 = (Summary) o1; 
					Summary summary2 = (Summary) o2;
					return summary1.componentID.compareTo(summary2.componentID);
				}
			});
			for (int i = 0, max = summaries.length; i < max; i++) {
				dumpIndexEntry(i, writer, summaries[i]);
			}
			writer.println(Messages.fullReportTask_indexfooter);
			writer.flush();
		} catch (IOException e) {
			throw new BuildException(NLS.bind(Messages.ioexception_writing_html_file, htmlFile.getAbsolutePath()));
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	private void dumpNonApiBundles(PrintWriter writer, Report report) {
		writer.println(Messages.fullReportTask_bundlesheader);
		Map nonApiBundleNames = report.getSkippedBundles();
		int count = 0;
		for (Iterator iterator = nonApiBundleNames.keySet().iterator(); iterator.hasNext();) {
			StringBuffer result = new StringBuffer();
			String bundleName = (String) iterator.next();
			result.append(bundleName);
			String[] bundleErrors = (String[]) nonApiBundleNames.get(bundleName);
			if (bundleErrors == null){
				result.append(Messages.AnalysisReportConversionTask_BundleErrorNewline);
				result.append(Messages.AnalysisReportConversionTask_NotSetupForAPIAnalysis);
			} else {
				for (int i = 0; i < bundleErrors.length; i++) {
					result.append(Messages.AnalysisReportConversionTask_BundleErrorNewline);
					result.append(bundleErrors[i]);
				}
			}
			if ((count % 2) == 0) {
				writer.println(MessageFormat.format(Messages.fullReportTask_bundlesentry_even, new String[] { result.toString() }));
			} else { 
				writer.println(MessageFormat.format(Messages.fullReportTask_bundlesentry_odd, new String[] { result.toString() }));
			}
			count++;
		}
		writer.println(Messages.fullReportTask_bundlesfooter);
	}
	private void dumpProblems(PrintWriter writer, String categoryName, Problem[] problems) {
		if (problems != null && problems.length != 0) {
			writer.println(
					MessageFormat.format(
						Messages.fullReportTask_categoryheader,
						new String[] {categoryName}));
			for (int i = 0, max = problems.length; i < max; i++) {
				Problem problem = problems[i];
				if ((i % 2) == 0) {
					switch(problem.severity) {
						case ApiPlugin.SEVERITY_ERROR :
							writer.println(MessageFormat.format(Messages.fullReportTask_problementry_even_error, new String[] { problem.getHtmlMessage() }));
							break;
						case ApiPlugin.SEVERITY_WARNING :
							writer.println(MessageFormat.format(Messages.fullReportTask_problementry_even_warning, new String[] { problem.getHtmlMessage() }));
					}
				} else { 
					switch(problem.severity) {
						case ApiPlugin.SEVERITY_ERROR :
							writer.println(MessageFormat.format(Messages.fullReportTask_problementry_odd_error, new String[] { problem.getHtmlMessage() }));
							break;
						case ApiPlugin.SEVERITY_WARNING :
							writer.println(MessageFormat.format(Messages.fullReportTask_problementry_odd_warning, new String[] { problem.getHtmlMessage() }));
					}
				}
			}
			writer.println(Messages.fullReportTask_categoryfooter);
		} else {
			writer.println(
					MessageFormat.format(
						Messages.fullReportTask_category_no_elements,
						new String[] {categoryName}));
		}
	}

	private void dumpReport(File xmlFile, Report report) {
		// create file writer
		// generate the html file name from the xml file name
		String htmlName = extractNameFromXMLName(xmlFile);
		report.setLink(extractLinkFrom(htmlName));
		File htmlFile = new File(htmlName);
		File parent = htmlFile.getParentFile();
		if (!parent.exists()) {
			if (!parent.mkdirs()) {
				throw new BuildException(NLS.bind(Messages.could_not_create_file, htmlName));
			}
		}
		PrintWriter writer = null;
		try {
			FileWriter fileWriter = new FileWriter(htmlFile);
			writer = new PrintWriter(new BufferedWriter(fileWriter));
			if (report.isNonApiBundlesReport()) {
				dumpNonApiBundles(writer, report);
			} else {
				dumpHeader(writer, report);
				// generate compatibility category
				dumpProblems(writer, Messages.fullReportTask_compatibility_header, report.getProblems(APIToolsAnalysisTask.COMPATIBILITY));
				writer.println(Messages.fullReportTask_categoryseparator);
				dumpProblems(writer, Messages.fullReportTask_api_usage_header, report.getProblems(APIToolsAnalysisTask.USAGE));
				writer.println(Messages.fullReportTask_categoryseparator);
				dumpProblems(writer, Messages.fullReportTask_bundle_version_header, report.getProblems(APIToolsAnalysisTask.BUNDLE_VERSION));
				dumpFooter(writer);
			}
			writer.flush();
		} catch (IOException e) {
			throw new BuildException(NLS.bind(Messages.ioexception_writing_html_file, htmlName));
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	/**
	 * Run the ant task
	 */
	public void execute() throws BuildException {
		if (this.debug) {
			System.out.println("xmlReportsLocation : " + this.xmlReportsLocation); //$NON-NLS-1$
			System.out.println("htmlReportsLocation : " + this.htmlReportsLocation); //$NON-NLS-1$
		}
		if (this.xmlReportsLocation == null) {
			throw new BuildException(Messages.missing_xml_files_location);
		}
		this.reportsRoot = new File(this.xmlReportsLocation);
		if (!this.reportsRoot.exists() || !this.reportsRoot.isDirectory()) {
			throw new BuildException(NLS.bind(Messages.invalid_directory_name, this.xmlReportsLocation));
		}
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = null;
		try {
			parser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		if (parser == null) {
			throw new BuildException(Messages.could_not_create_sax_parser);
		}

		if (this.htmlReportsLocation == null) {
			this.htmlReportsLocation = this.xmlReportsLocation;
		}
		this.htmlRoot = new File(this.htmlReportsLocation);
		if (!this.htmlRoot.exists()) {
			if (!this.htmlRoot.mkdirs()) {
				throw new BuildException(NLS.bind(Messages.could_not_create_file, this.htmlReportsLocation));
			}
		}
		if (this.debug) {
			System.out.println("output name :" + this.htmlReportsLocation); //$NON-NLS-1$
		}
		try {
			// retrieve all xml reports
			File[] allFiles = Util.getAllFiles(reportsRoot, new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.isDirectory() || pathname.getName().endsWith(".xml"); //$NON-NLS-1$
				}
			});
			if (allFiles != null) {
				int length = allFiles.length;
				List summariesList = new ArrayList(length);
				Summary nonApiBundleSummary = null;
				for (int i = 0; i < length; i++) {
					File file = allFiles[i];
					ConverterDefaultHandler defaultHandler = new ConverterDefaultHandler(this.debug);
					parser.parse(file, defaultHandler);
					Report report = defaultHandler.getReport();
					if (report == null) {
						// ignore that file. It could be the counts.xml file.
						if (this.debug) {
							System.out.println("Skipped file : " + file.getAbsolutePath()); //$NON-NLS-1$
						}
						continue;
					}
					dumpReport(file, report);
					if (report.isNonApiBundlesReport()) {
						nonApiBundleSummary = new Summary(report);
					} else {
						summariesList.add(new Summary(report));
					}
				}
				// dump index file
				Summary[] summaries = new Summary[summariesList.size()];
				summariesList.toArray(summaries);
				dumpIndexFile(reportsRoot, summaries, nonApiBundleSummary);
			}
		} catch (SAXException e) {
			// ignore
		} catch (IOException e) {
			// ignore
		}
	}
	private String extractLinkFrom(String fileName) {
		StringBuffer buffer = new StringBuffer();
		buffer.append('.').append(fileName.substring(this.htmlRoot.getAbsolutePath().length()).replace('\\', '/'));
		return String.valueOf(buffer);
	}
	private String extractNameFromXMLName(File xmlFile) {
		String fileName = xmlFile.getAbsolutePath();
		int index = fileName.lastIndexOf('.');
		StringBuffer buffer = new StringBuffer();
		buffer.append(fileName.substring(this.reportsRoot.getAbsolutePath().length(), index)).append(".html"); //$NON-NLS-1$
		File htmlFile = new File(this.htmlReportsLocation, String.valueOf(buffer));
		return htmlFile.getAbsolutePath();
	}
	/**
	 * Set the debug value.
	 * <p>The possible values are: <code>true</code>, <code>false</code></p>
	 * <p>Default is <code>false</code>.</p>
	 *
	 * @param debugValue the given debug value
	 */
	public void setDebug(String debugValue) {
		this.debug = Boolean.toString(true).equals(debugValue); 
	}
	/**
	 * Set the location where the html reports are generated.
	 * 
	 * <p>This is optional. If not set, the html files are created in the same folder as the
	 * xml files.</p>
	 * <p>The location is set using an absolute path.</p>
	 * 
	 * @param htmlFilesLocation the given the location where the html reports are generated
	 */
	public void setHtmlFiles(String htmlFilesLocation) {
		this.htmlReportsLocation = htmlFilesLocation;
	}
	/**
	 * Set the location where the xml reports are retrieved.
	 * 
	 * <p>The location is set using an absolute path.</p>
	 *
	 * @param xmlFilesLocation the given location to retrieve the xml reports
	 */
	public void setXmlFiles(String xmlFilesLocation) {
		this.xmlReportsLocation = xmlFilesLocation;
	}
}
