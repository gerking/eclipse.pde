package org.eclipse.pde.internal.core.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.core.build.*;
import org.eclipse.core.runtime.model.*;
import java.net.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.*;
import java.io.File;

public abstract class ExternalPluginModelBase extends AbstractPluginModelBase {
	private String installLocation;
	private IPath eclipseHomeRelativePath;
	private IBuildModel buildModel;
	private long timeStamp;

	public ExternalPluginModelBase() {
		super();
	}
	protected NLResourceHelper createNLResourceHelper() {
		String name = isFragmentModel() ? "fragment" : "plugin";
		return new NLResourceHelper(name, getNLLookupLocations());
	}
	
	public URL getNLLookupLocation() {
		String installLocation = getInstallLocation();
		if (installLocation.startsWith("file:") == false)
			installLocation = "file:" + installLocation;
		try {
			URL url = new URL(installLocation + "/");
			return url;
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public IBuildModel getBuildModel() {
		if (buildModel == null) {
			buildModel = new ExternalBuildModel(getInstallLocation());
			((ExternalBuildModel) buildModel).load();
		}
		return buildModel;
	}
	public org.eclipse.core.runtime.IPath getEclipseHomeRelativePath() {
		return eclipseHomeRelativePath;
	}
	public String getInstallLocation() {
		return installLocation;
	}
	public boolean isEditable() {
		return false;
	}
	public void load() {
	}
	public void load(PluginModel descriptorModel) {
		PluginBase pluginBase = (PluginBase) getPluginBase();
		if (pluginBase == null) {
			pluginBase = (PluginBase) createPluginBase();
			this.pluginBase = pluginBase;
		} else {
			pluginBase.reset();
		}
		pluginBase.load(descriptorModel);
		updateTimeStamp();
		loaded = true;
	}

	public boolean isInSync() {
		return isInSync(getLocalFile());
	}

	private File getLocalFile() {
		String manifest = isFragmentModel() ? "fragment.xml" : "plugin.xml";
		String prefix = getInstallLocation();
		if (prefix.startsWith("file:"))
			prefix = prefix.substring(5);
		return new File(prefix + File.separator + manifest);
	}

	protected void updateTimeStamp() {
		updateTimeStamp(getLocalFile());
	}

	public void setEclipseHomeRelativePath(IPath newEclipseHomeRelativePath) {
		eclipseHomeRelativePath = newEclipseHomeRelativePath;
	}
	public void setInstallLocation(String newInstallLocation) {
		installLocation = newInstallLocation;
	}
}