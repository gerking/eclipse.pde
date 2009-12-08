/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.PDECoreMessages;

public class SourceEntryErrorReporter extends BuildErrorReporter {

	public SourceEntryErrorReporter(IFile file, int buildSeverity) {
		super(file);
		fBuildSeverity = buildSeverity;
	}

	class ProjectFolder {
		IPath fPath;
		String fToken;
		ArrayList fLibs = new ArrayList(1);
		String dupeLibName = null;

		public ProjectFolder(IPath path) {
			fPath = path;
		}

		public IPath getPath() {
			return fPath;
		}

		void setToken(String token) {
			fToken = token;
		}

		public String getToken() {
			if (fToken == null) {
				return fPath.toString();
			}
			return fToken;
		}

		public void addLib(String libName) {
			if (fLibs.contains(libName)) {
				dupeLibName = libName;
			} else
				fLibs.add(libName);
		}

		public ArrayList getLibs() {
			return fLibs;
		}

		public String getDupeLibName() {
			return dupeLibName;
		}
	}

	class SourceFolder extends ProjectFolder {

		OutputFolder fOutputFolder;

		/**
		 * Constructs a source folder with the given project relative path.
		 * 
		 * @param path source folder path
		 * @param outputFolder associated output folder
		 */
		public SourceFolder(IPath path, OutputFolder outputFolder) {
			super(path);
			fOutputFolder = outputFolder;
		}

		public OutputFolder getOutputLocation() {
			return fOutputFolder;
		}

	}

	class OutputFolder extends ProjectFolder {

		private ArrayList fSourceFolders = new ArrayList();

		/**
		 * Creates an output folder with the given relative path (relative to the project).
		 * 
		 * @param path project relative path
		 */
		public OutputFolder(IPath path) {
			super(path);
		}

		public void addSourceFolder(SourceFolder sourceFolder) {
			if (!fSourceFolders.contains(sourceFolder))
				fSourceFolders.add(sourceFolder);
		}

		public ArrayList getSourceFolders() {
			return fSourceFolders;
		}

	}

	HashMap fSourceFolderMap = new HashMap(4);
	HashMap fOutputFolderMap = new HashMap(4);

	public void initialize(ArrayList sourceEntries, ArrayList outputEntries, IClasspathEntry[] cpes, IProject project) {

		fProject = project;
		IPath defaultOutputLocation = null;
		try {
			defaultOutputLocation = JavaCore.create(fProject).getOutputLocation();
		} catch (JavaModelException e) {
		}

		for (int i = 0; i < cpes.length; i++) {
			if (cpes[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath sourcePath = cpes[i].getPath().removeFirstSegments(1).addTrailingSeparator();
				IPath outputLocation = cpes[i].getOutputLocation();
				if (outputLocation == null)
					outputLocation = defaultOutputLocation;
				IPath outputPath = outputLocation.removeFirstSegments(1).addTrailingSeparator();

				OutputFolder outputFolder = (OutputFolder) fOutputFolderMap.get(outputPath);
				if (outputFolder == null) {
					outputFolder = new OutputFolder(outputPath);
				}

				SourceFolder sourceFolder = (SourceFolder) fSourceFolderMap.get(sourcePath);
				if (sourceFolder == null) {
					sourceFolder = new SourceFolder(sourcePath, outputFolder);
				}

				outputFolder.addSourceFolder(sourceFolder);
				fOutputFolderMap.put(outputPath, outputFolder);
				fSourceFolderMap.put(sourcePath, sourceFolder);
			}
		}

		for (Iterator iterator = sourceEntries.iterator(); iterator.hasNext();) {
			IBuildEntry sourceEntry = (IBuildEntry) iterator.next();
			String libName = sourceEntry.getName().substring(PROPERTY_SOURCE_PREFIX.length());
			String[] tokens = sourceEntry.getTokens();
			for (int i = 0; i < tokens.length; i++) {
				IPath path = new Path(tokens[i]).addTrailingSeparator();
				SourceFolder sourceFolder = (SourceFolder) fSourceFolderMap.get(path);
				if (sourceFolder == null) {
					sourceFolder = new SourceFolder(path, null);
					fSourceFolderMap.put(path, sourceFolder);
				}
				sourceFolder.setToken(tokens[i]);
				sourceFolder.addLib(libName);
			}
		}

		for (Iterator iterator = outputEntries.iterator(); iterator.hasNext();) {
			IBuildEntry outputEntry = (IBuildEntry) iterator.next();
			String libName = outputEntry.getName().substring(PROPERTY_OUTPUT_PREFIX.length());
			String[] tokens = outputEntry.getTokens();
			for (int i = 0; i < tokens.length; i++) {
				IPath path = new Path(tokens[i]).addTrailingSeparator();
				OutputFolder outputFolder = (OutputFolder) fOutputFolderMap.get(path);
				if (outputFolder == null) {
					outputFolder = new OutputFolder(path);
					fOutputFolderMap.put(path, outputFolder);
				}
				outputFolder.setToken(tokens[i]);
				outputFolder.addLib(libName);
			}
		}
	}

	public void validate() {

		for (Iterator iterator = fOutputFolderMap.keySet().iterator(); iterator.hasNext();) {
			IPath outputPath = (IPath) iterator.next();
			OutputFolder outputFolder = (OutputFolder) fOutputFolderMap.get(outputPath);
			ArrayList sourceFolders = outputFolder.getSourceFolders();
			ArrayList outputFolderLibs = new ArrayList(outputFolder.getLibs());

			if (sourceFolders.size() == 0) {
				// report error - invalid output folder				
				for (Iterator libNameiterator = outputFolderLibs.iterator(); libNameiterator.hasNext();) {
					String libName = (String) libNameiterator.next();
					IResource folderEntry = fProject.findMember(outputPath);
					String message;
					if (folderEntry == null || !folderEntry.exists() || !(folderEntry instanceof IContainer))
						message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFolder, outputPath.toString());
					else
						message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_InvalidOutputFolder, outputPath.toString());
					prepareError(PROPERTY_OUTPUT_PREFIX + libName, outputFolder.getToken(), message, PDEMarkerFactory.B_REMOVAL, PDEMarkerFactory.CAT_OTHER);
				}
			} else {
				String srcFolderLibName = null;

				for (int i = 0; i < sourceFolders.size(); i++) {
					SourceFolder sourceFolder = (SourceFolder) sourceFolders.get(i);
					ArrayList srcFolderLibs = sourceFolder.getLibs();
					outputFolderLibs.removeAll(srcFolderLibs);
					switch (srcFolderLibs.size()) {
						case 0 :
							//error - src folder with no lib
							//do nothing. already caught in super
							break;
						case 1 :
							if (srcFolderLibName == null) {
								srcFolderLibName = (String) srcFolderLibs.get(0);
								break;
							} else if (srcFolderLibName.equals(srcFolderLibs.get(0))) {
								break;
							}
						default :
							//error - targeted to diff libs
							String erringSrcFolders = join((SourceFolder[]) sourceFolders.toArray(new SourceFolder[sourceFolders.size()]));
							for (int j = 0; j < sourceFolders.size(); j++) {
								SourceFolder srcFolder = (SourceFolder) sourceFolders.get(j);
								for (int k = 0; k < srcFolder.getLibs().size(); k++) {
									String libName = (String) srcFolder.getLibs().get(k);
									String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_DifferentTargetLibrary, erringSrcFolders);
									prepareError(PROPERTY_SOURCE_PREFIX + libName, srcFolder.getToken(), message, PDEMarkerFactory.NO_RESOLUTION, PDEMarkerFactory.CAT_OTHER);
								}
							}
					}
				}
				for (int i = 0; i < outputFolderLibs.size(); i++) {
					String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_ExtraOutputFolder, outputFolder.getPath().toString(), PROPERTY_SOURCE_PREFIX + outputFolderLibs.get(i));
					prepareError(PROPERTY_OUTPUT_PREFIX + outputFolderLibs.get(i), outputFolder.getToken(), message, PDEMarkerFactory.B_REMOVAL, PDEMarkerFactory.CAT_OTHER);
				}

				if (outputFolder.getDupeLibName() != null) {
					String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_DupeOutputFolder, outputPath.toString(), PROPERTY_OUTPUT_PREFIX + outputFolder.getDupeLibName());
					prepareError(PROPERTY_OUTPUT_PREFIX + outputFolder.getDupeLibName(), outputFolder.getToken(), message, PDEMarkerFactory.NO_RESOLUTION, PDEMarkerFactory.CAT_OTHER);
				}
			}
		}

		for (Iterator iterator = fSourceFolderMap.keySet().iterator(); iterator.hasNext();) {
			IPath sourcePath = (IPath) iterator.next();
			SourceFolder sourceFolder = (SourceFolder) fSourceFolderMap.get(sourcePath);
			OutputFolder outputFolder = sourceFolder.getOutputLocation();

			if (outputFolder == null) {
				//error - not a src folder
				IResource folderEntry = fProject.findMember(sourcePath);
				String message;
				if (folderEntry == null || !folderEntry.exists() || !(folderEntry instanceof IContainer))
					message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFolder, sourcePath.toString());
				else
					message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_InvalidSourceFolder, sourcePath.toString());

				ArrayList srcLibs = sourceFolder.getLibs();
				for (int i = 0; i < srcLibs.size(); i++) {
					String libName = (String) srcLibs.get(i);
					prepareError(PROPERTY_SOURCE_PREFIX + libName, sourceFolder.getToken(), message, PDEMarkerFactory.B_REMOVAL, PDEMarkerFactory.CAT_OTHER);
				}
			} else {
				if (outputFolder.getLibs().size() == 0 && sourceFolder.getLibs().size() == 1) {
					String libName = (String) sourceFolder.getLibs().get(0);
					//error - missing output folder
					String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_MissingOutputEntry, sourcePath.toString(), PROPERTY_OUTPUT_PREFIX + libName);
					prepareError(PROPERTY_OUTPUT_PREFIX + libName, outputFolder.getToken(), message, PDEMarkerFactory.B_ADDDITION, CompilerFlags.getFlag(fFile.getProject(), CompilerFlags.P_BUILD_MISSING_OUTPUT), PDEMarkerFactory.CAT_OTHER);
				}

				if (sourceFolder.getDupeLibName() != null) {
					String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_DupeSourceFolder, sourcePath.toString(), PROPERTY_SOURCE_PREFIX + sourceFolder.getDupeLibName());
					prepareError(PROPERTY_SOURCE_PREFIX + sourceFolder.getDupeLibName(), sourceFolder.getToken(), message, PDEMarkerFactory.NO_RESOLUTION, PDEMarkerFactory.CAT_OTHER);
				}
			}
		}

	}

	private String join(ProjectFolder[] folders) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < folders.length; i++) {
			String text = folders[i].getPath().toString().trim();
			if (text.length() > 0) {
				result.append(text);
				result.append(',');
			}
		}
		result.deleteCharAt(result.length() - 1);
		return result.toString();
	}

	public ArrayList getProblemList() {
		return fProblemList;
	}
}