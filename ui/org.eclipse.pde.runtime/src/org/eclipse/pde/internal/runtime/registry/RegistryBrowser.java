/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.views.properties.*;
import org.osgi.framework.*;
public class RegistryBrowser extends ViewPart
implements
BundleListener,
IRegistryChangeListener {
	public static final String KEY_REFRESH_LABEL = "RegistryView.refresh.label";
	public static final String KEY_REFRESH_TOOLTIP = "RegistryView.refresh.tooltip";
	public static final String SHOW_RUNNING_PLUGINS = "RegistryView.showRunning.label";
	public static final String KEY_COLLAPSE_ALL_LABEL = "RegistryView.collapseAll.label";
	public static final String KEY_COLLAPSE_ALL_TOOLTIP = "RegistryView.collapseAll.tooltip";
	private TreeViewer treeViewer;
	private Action refreshAction;
	private Action showPluginsAction;
	private Action collapseAllAction;
	private DrillDownAdapter drillDownAdapter;
	private IMemento memento;

	//attributes view
	private SashForm fSashForm;
	private Label fPropertyLabel;
	private Label fPropertyImage;
	private PropertySheetPage fPropertySheet;
	public RegistryBrowser() {
		super();
		Platform.getExtensionRegistry().addRegistryChangeListener(this);
		PDERuntimePlugin.getDefault().getBundleContext()
		.addBundleListener(this);
	}
	public void makeActions() {
		refreshAction = new Action("refresh") {
			public void run() {
				BusyIndicator.showWhile(treeViewer.getTree().getDisplay(),
						new Runnable() {
					public void run() {
						treeViewer.refresh();
					}
				});
			}
		};
		refreshAction.setText(PDERuntimePlugin
				.getResourceString(KEY_REFRESH_LABEL));
		refreshAction.setToolTipText(PDERuntimePlugin
				.getResourceString(KEY_REFRESH_TOOLTIP));
		refreshAction.setImageDescriptor(PDERuntimePluginImages.DESC_REFRESH);
		refreshAction
		.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_REFRESH_DISABLED);
		refreshAction
		.setHoverImageDescriptor(PDERuntimePluginImages.DESC_REFRESH_HOVER);
		
		showPluginsAction = new Action(PDERuntimePlugin.getResourceString(SHOW_RUNNING_PLUGINS)){
			public void run(){
				((RegistryBrowserContentProvider) treeViewer.getContentProvider())
			.setShowRunning(showPluginsAction.isChecked());
				handleShowRunningPlugins(showPluginsAction.isChecked());
			}
		};
		showPluginsAction.setChecked(memento.getString(SHOW_RUNNING_PLUGINS).equals("true"));
		
		collapseAllAction = new Action("collapseAll"){
			public void run(){
				treeViewer.collapseAll();
			}
		};
		collapseAllAction.setText(PDERuntimePlugin.getResourceString(KEY_COLLAPSE_ALL_LABEL));
		collapseAllAction.setImageDescriptor(PDERuntimePluginImages.DESC_COLLAPSE_ALL);
		collapseAllAction.setHoverImageDescriptor(PDERuntimePluginImages.DESC_COLLAPSE_ALL_HOVER);
		collapseAllAction.setToolTipText(PDERuntimePlugin.getResourceString(KEY_COLLAPSE_ALL_TOOLTIP));
	}
	public void dispose() {
		Platform.getExtensionRegistry().removeRegistryChangeListener(this);
		PDERuntimePlugin.getDefault().getBundleContext().removeBundleListener(
				this);
		super.dispose();
	}
	public void setFocus() {
	}
	public void createPartControl(Composite parent) {
		// create the sash form that will contain the tree viewer & text viewer
		fSashForm = new SashForm(parent, SWT.HORIZONTAL);
		fSashForm.setLayout(new GridLayout());
		fSashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		setSashForm(fSashForm);
		makeActions();
		createTreeViewer();
		createAttributesViewer();
		fillToolBar();
		treeViewer.refresh();
	}
	private void createTreeViewer() {
		Tree tree = new Tree(getSashForm(), SWT.FLAT);
		treeViewer = new TreeViewer(tree);
		boolean showRunning = memento.getString(SHOW_RUNNING_PLUGINS).equals("true") ? true : false;
		treeViewer.setContentProvider(new RegistryBrowserContentProvider(treeViewer, showRunning));
		treeViewer
		.setLabelProvider(new RegistryBrowserLabelProvider(treeViewer));
		treeViewer.setUseHashlookup(true);
		treeViewer.setSorter(new ViewerSorter() {
		});
		treeViewer.setInput(new PluginObjectAdapter(Platform.getPluginRegistry()));
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				Object selection = ((IStructuredSelection) event.getSelection())
				.getFirstElement();
				updateAttributesView(selection);
			}
		});
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				Object selection = ((IStructuredSelection) event.getSelection())
				.getFirstElement();
				updateAttributesView(selection);
				if (selection != null && treeViewer.isExpandable(selection))
					treeViewer.setExpandedState(selection, !treeViewer
							.getExpandedState(selection));
			}
		});

		WorkbenchHelp.setHelp(treeViewer.getControl(),
				IHelpContextIds.REGISTRY_VIEW);
		
		getViewSite().setSelectionProvider(treeViewer);
		
		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				fillContextMenu(mng);
			}
		};
		popupMenuManager.setRemoveAllWhenShown(true);
		popupMenuManager.addMenuListener(listener);
		Menu menu = popupMenuManager.createContextMenu(tree);
		tree.setMenu(menu);
	}
	private void fillToolBar(){
		drillDownAdapter = new DrillDownAdapter(treeViewer);
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager mng = bars.getToolBarManager();
		drillDownAdapter.addNavigationActions(mng);
		mng.add(refreshAction);
		mng.add(new Separator());
		mng.add(collapseAllAction);
		IMenuManager mgr = bars.getMenuManager();
		mgr.add(showPluginsAction);
	}
	public void fillContextMenu(IMenuManager manager) {
		manager.add(refreshAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
	}
	public TreeViewer getTreeViewer() {
		return treeViewer;
	}
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		if (memento == null)
			this.memento = XMLMemento.createWriteRoot("REGISTRYVIEW");
		else
			this.memento = memento;
		initializeMemento();
	}
	private void initializeMemento() {
		if (memento.getString(SHOW_RUNNING_PLUGINS) == null)
			memento.putString(SHOW_RUNNING_PLUGINS, "true");
	}
	public void saveState(IMemento memento) {
		boolean showRunning = ((RegistryBrowserContentProvider) treeViewer
				.getContentProvider()).isShowRunning();
		if (showRunning)
			this.memento.putString(SHOW_RUNNING_PLUGINS, "true");
		else
			this.memento.putString(SHOW_RUNNING_PLUGINS, "false");
		memento.putMemento(this.memento);
	}
	/* add attributes viewer */
	protected void createAttributesViewer() {
		Composite composite = new Composite(getSashForm(), SWT.FLAT);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		fPropertyImage = new Label(composite, SWT.NONE);
		GridData gd = new GridData(GridData.FILL);
		gd.widthHint = 20;
		fPropertyImage.setLayoutData(gd);
		fPropertyLabel = new Label(composite, SWT.NULL);
		fPropertyLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createPropertySheet(composite);
	}	
	
	protected void createPropertySheet(Composite parent) {
		Composite composite = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		composite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		composite.setLayoutData(gd);
		fPropertySheet = new PropertySheetPage();
		fPropertySheet.createControl(composite);
		gd = new GridData(GridData.FILL_BOTH);
		fPropertySheet.getControl().setLayoutData(gd);
		fPropertySheet.makeContributions(new MenuManager(),
				new ToolBarManager(), null);
	}
	public void updateAttributesView(Object selection) {
		fPropertyImage.setImage(((RegistryBrowserLabelProvider) treeViewer
				.getLabelProvider()).getImage(selection));
		fPropertyLabel.setText(((RegistryBrowserLabelProvider) treeViewer
				.getLabelProvider()).getText(selection));
		if (selection != null)
			fPropertySheet.selectionChanged(null, new StructuredSelection(
					selection));
		else
			fPropertySheet.selectionChanged(null, new StructuredSelection(
					new Object()));
	}
	protected SashForm getSashForm() {
		return fSashForm;
	}
	private void setSashForm(SashForm sashForm) {
		fSashForm = sashForm;
	}
	/*
	 * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
	 */
	public void bundleChanged(BundleEvent event) {
		final RegistryBrowserContentProvider provider = ((RegistryBrowserContentProvider) treeViewer
				.getContentProvider());
		final IPluginDescriptor descriptor = Platform.getPluginRegistry()
		.getPluginDescriptor(event.getBundle().getGlobalName());
		if (descriptor == null)
			return;
		final PluginObjectAdapter adapter = new PluginObjectAdapter(descriptor);
		treeViewer.getTree().getDisplay().asyncExec(new Runnable() {
			public void run() {
				TreeItem[] items = treeViewer.getTree().getItems();
				if (items != null) {
					for (int i = 0; i < items.length; i++) {
						PluginObjectAdapter plugin = (PluginObjectAdapter) items[i]
																				 .getData();
						if (plugin != null) {
							Object object = plugin.getObject();
							if (object instanceof IPluginDescriptor) {
								IPluginDescriptor desc = (IPluginDescriptor) object;
								if (desc.equals(descriptor)) {
									treeViewer.remove(plugin);
									break;
								}
							}
						}
					}
				}
				if (provider.isShowRunning() && descriptor.isPluginActivated())
					treeViewer.add(treeViewer.getInput(), adapter);
				else
					treeViewer.add(treeViewer.getInput(), adapter);
			}
		});
	}
	/*
	 * @see org.eclipse.core.runtime.IRegistryChangeListener#registryChanged(org.eclipse.core.runtime.IRegistryChangeEvent)
	 */
	public void registryChanged(IRegistryChangeEvent event) {
		final IExtensionDelta[] deltas = event.getExtensionDeltas();
		treeViewer.getTree().getDisplay().syncExec(new Runnable() {
			public void run() {
				for (int i = 0; i < deltas.length; i++) {
					IExtension ext = deltas[i].getExtension();
					IExtensionPoint extPoint = deltas[i].getExtensionPoint();
					IPluginDescriptor descriptor = extPoint
					.getDeclaringPluginDescriptor();
					PluginObjectAdapter adapter = new PluginObjectAdapter(
							descriptor);
					if (deltas[i].getKind() == IExtensionDelta.ADDED) {
						if (ext != null)
							treeViewer.add(adapter, ext);
						if (extPoint != null)
							treeViewer.add(adapter, extPoint);
					} else {
						if (ext != null)
							treeViewer.remove(ext);
						if (extPoint != null)
							treeViewer.remove(extPoint);
						treeViewer.refresh();
					}
				}
			}
		});
	}
	protected void handleShowRunningPlugins(boolean showRunning){
		if (showRunning){
			TreeItem[] items = treeViewer.getTree().getItems();
			if (items == null)
				return;
			
			for (int i = 0; i < items.length; i++) {
				PluginObjectAdapter plugin = (PluginObjectAdapter) items[i].getData();
				if (plugin != null) {
					Object object = plugin.getObject();
					if (object instanceof IPluginDescriptor) {
						IPluginDescriptor desc = (IPluginDescriptor) object;
						if (!desc.isPluginActivated())
							treeViewer.remove(plugin);
					}
				}
			}
			
		} else {
			IPluginDescriptor[] descriptors = Platform.getPluginRegistry().getPluginDescriptors();
			for (int i = 0; i<descriptors.length; i++){
				if (!descriptors[i].isPluginActivated())
					treeViewer.add(treeViewer.getInput(), new PluginObjectAdapter(descriptors[i]));
			}
		}
	}
}
