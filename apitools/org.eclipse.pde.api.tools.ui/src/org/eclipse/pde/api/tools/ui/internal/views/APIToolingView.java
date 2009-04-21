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
package org.eclipse.pde.api.tools.ui.internal.views;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ISession;
import org.eclipse.pde.api.tools.internal.provisional.ISessionListener;
import org.eclipse.pde.api.tools.internal.provisional.ISessionManager;
import org.eclipse.pde.api.tools.internal.provisional.ITreeModel;
import org.eclipse.pde.api.tools.internal.provisional.ITreeNode;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.ui.internal.ApiImageDescriptor;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.actions.ExportSessionAction;
import org.eclipse.pde.api.tools.ui.internal.actions.RemoveActiveSessionAction;
import org.eclipse.pde.api.tools.ui.internal.actions.RemoveAllSessionsAction;
import org.eclipse.pde.api.tools.ui.internal.actions.SelectSessionAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * The class is used to show api tooling task results in a tree view.
 */

public class APIToolingView extends ViewPart implements ISessionListener {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "org.eclipse.pde.api.tools.ui.views.apitooling.views.apitoolingview"; //$NON-NLS-1$

	public TreeViewer viewer;
	private IAction removeActiveSessionAction;
	private IAction removeAllSessionsAction;
	private IAction selectSessionAction;
	private Action doubleClickAction;
	private ExportSessionAction exportSessionAction;

	class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {
		ITreeModel model;
		
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			this.model = null;
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if (parent instanceof ISession) {
				ISession session = (ISession) parent;
				if (this.model == null) {
					this.model = session.getModel();
				}
				return getChildren(this.model.getRoot());
			}
			return getChildren(parent);
		}
		public Object getParent(Object child) {
			return null;
		}
		public Object [] getChildren(Object parent) {
			if (parent instanceof ITreeNode) {
				return ((ITreeNode) parent).getChildren();
			}
			return new Object[0];
		}
		public boolean hasChildren(Object parent) {
			if (parent instanceof ITreeNode) {
				return ((ITreeNode) parent).hasChildren();
			}
			return false;
		}
	}
	class ViewLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			return obj.toString();
		}

		public Image getImage(Object obj) {
			if (obj instanceof ITreeNode) {
				ITreeNode treeNode = (ITreeNode) obj;
				switch(treeNode.getId()) {
					case ITreeNode.CLASS :
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CLASS);
					case ITreeNode.INTERFACE :
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_INTERFACE);
					case ITreeNode.ANNOTATION :
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_ANNOTATION);
					case ITreeNode.ENUM :
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_ENUM);
					case ITreeNode.PACKAGE :
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PACKAGE);
				}
				Object data = treeNode.getData();
				if (data instanceof IDelta) {
					IDelta delta = (IDelta) data;
					Image image = getDeltaElementImage(delta);
					if (image != null) {
						int flags = (DeltaProcessor.isCompatible(delta) ? ApiImageDescriptor.SUCCESS : ApiImageDescriptor.ERROR);
						ImageDescriptor descriptor = ImageDescriptor.createFromImage(image);
						ApiImageDescriptor desc = new ApiImageDescriptor(descriptor, flags);
						return ApiUIPlugin.getImage(desc);
					}
				}
			}
			return null;
		}

		private Image getDeltaElementImage(IDelta delta) {
			switch(delta.getFlags()) {
				case IDelta.API_FIELD :
				case IDelta.FIELD : {
					int modifiers = delta.getNewModifiers();
					switch(delta.getKind()) {
						case IDelta.REMOVED : {
							modifiers = delta.getOldModifiers();
						}
					}
					if (Flags.isPublic(modifiers)) {
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_FIELD_PUBLIC);
					} else if (Flags.isProtected(modifiers)) {
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_FIELD_PROTECTED);
					} else if (Flags.isPrivate(modifiers)){
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_FIELD_PRIVATE);
					} else {
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_FIELD_DEFAULT);
					}
				}
				case IDelta.METHOD :
				case IDelta.METHOD_MOVED_DOWN :
				case IDelta.METHOD_MOVED_UP :
				case IDelta.METHOD_WITH_DEFAULT_VALUE :
				case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
				case IDelta.CONSTRUCTOR :
				case IDelta.CLINIT : {
					int modifiers = delta.getNewModifiers();
					switch(delta.getKind()) {
						case IDelta.REMOVED : {
							modifiers = delta.getOldModifiers();
						}
					}
					if (Flags.isPublic(modifiers)) {
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PUBLIC);
					} else if (Flags.isProtected(modifiers)) {
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PROTECTED);
					} else if (Flags.isPrivate(modifiers)){
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PRIVATE);
					} else {
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CLASS_DEFAULT);
					}
				}
			}
			switch(delta.getElementType()) {
				case IDelta.ANNOTATION_ELEMENT_TYPE : return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_ANNOTATION);
				case IDelta.ENUM_ELEMENT_TYPE : return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_ENUM);
				case IDelta.CLASS_ELEMENT_TYPE : return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CLASS);
				case IDelta.INTERFACE_ELEMENT_TYPE : return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_INTERFACE);
				case IDelta.FIELD_ELEMENT_TYPE : {
					int modifiers = delta.getNewModifiers();
					switch(delta.getKind()) {
						case IDelta.REMOVED : {
							modifiers = delta.getOldModifiers();
						}
					}
					if (Flags.isPublic(modifiers)) {
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_FIELD_PUBLIC);
					} else if (Flags.isProtected(modifiers)) {
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_FIELD_PROTECTED);
					} else if (Flags.isPrivate(modifiers)){
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_FIELD_PRIVATE);
					} else {
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_FIELD_DEFAULT);
					}
				}
				case IDelta.METHOD_ELEMENT_TYPE :
				case IDelta.CONSTRUCTOR_ELEMENT_TYPE : {
					int modifiers = delta.getNewModifiers();
					switch(delta.getKind()) {
						case IDelta.REMOVED : {
							modifiers = delta.getOldModifiers();
						}
					}
					if (Flags.isPublic(modifiers)) {
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PUBLIC);
					} else if (Flags.isProtected(modifiers)) {
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PROTECTED);
					} else if (Flags.isPrivate(modifiers)){
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PRIVATE);
					} else {
						return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CLASS_DEFAULT);
					}
				}
				case IDelta.TYPE_PARAMETER_ELEMENT_TYPE : return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PUBLIC);
				case IDelta.API_PROFILE_ELEMENT_TYPE : return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_ECLIPSE_PROFILE);
				case IDelta.API_COMPONENT_ELEMENT_TYPE : {
					String componentVersionId = delta.getComponentVersionId();
					IApiComponent component = null;
					if (componentVersionId != null) {
						int indexOfOpen = componentVersionId.lastIndexOf('(');
						String componentID = componentVersionId.substring(0, indexOfOpen);
						IApiBaseline baseline = ApiBaselineManager.getManager().getWorkspaceBaseline();
						if (baseline != null) {
							IApiComponent apiComponent = baseline.getApiComponent(componentID);
							if (apiComponent != null) {
								component = apiComponent;
							}
						}
					}
					if (component != null) {
						if(component.isSystemComponent()) {
							return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_API_SYSTEM_LIBRARY);
						}
						try {
							if (component.isFragment()) {
								return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_FRAGMENT);
							}
						} catch (CoreException e) {
							ApiPlugin.log(e);
						}
						return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_BUNDLE);
					}
				}
			}
			return null;
		}
	}

	/**
	 * The constructor.
	 */
	public APIToolingView() {
	}
	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		this.viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		this.viewer.setContentProvider(new ViewContentProvider());
		this.viewer.setLabelProvider(new ViewLabelProvider());
		this.viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				// TODO Auto-generated method stub
				
			}
		});
		ISession[] sessions = ApiPlugin.getDefault().getSessionManager().getSessions();
		if (sessions.length > 0) {
			this.viewer.setInput(sessions[0]);
		}
		createActions();
		updateActions();
		configureToolbar();
		hookDoubleClickAction();

		ApiPlugin.getDefault().getSessionManager().addSessionListener(this);
		
		getSite().setSelectionProvider(this.viewer);
	}
	private void hookDoubleClickAction() {
		this.viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				APIToolingView.this.doubleClickAction.run();
			}
		});
	}
	public void dispose() {
		ApiPlugin.getDefault().getSessionManager().removeSessionListener(this);
	}
	protected void configureToolbar() {
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(new Separator());
		tbm.add(this.removeActiveSessionAction);
		tbm.add(this.removeAllSessionsAction);
		tbm.add(this.selectSessionAction);
		tbm.add(new Separator());
		tbm.add(this.exportSessionAction);
//		tbm.add(new CollapseAllAction(this.viewer));

//		IMenuManager mm = getViewSite().getActionBars().getMenuManager();
//		mm.add(this.exportSessionAction);
	}
	private void createActions() {
		final IActionBars actionBars = getViewSite().getActionBars();

		this.removeActiveSessionAction = new RemoveActiveSessionAction();
		actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), removeActiveSessionAction);

		this.removeAllSessionsAction = new RemoveAllSessionsAction();
		this.selectSessionAction = new SelectSessionAction();
		this.doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				if (selection instanceof IStructuredSelection) {
					Object item = ((IStructuredSelection) selection).getFirstElement();
					if (APIToolingView.this.viewer.getExpandedState(item)) {
						APIToolingView.this.viewer.collapseToLevel(item, 1);
					} else {
						APIToolingView.this.viewer.expandToLevel(item, 1);
					}
					if (item instanceof ITreeNode) {
						ITreeNode node = (ITreeNode) item;
						if (node.getData() != null) {
							// show in Properties view
						}
					}
				}
			}
		};
		this.exportSessionAction = new ExportSessionAction(this);
	}
	private void updateActions() {
		this.viewer.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				ISessionManager sessionManager = ApiPlugin.getDefault().getSessionManager();
				ISession active = sessionManager.getActiveSession();
				setContentDescription(active == null ? "" : active.getTimestamp()); //$NON-NLS-1$
				ISession[] sessions =  sessionManager.getSessions();
				boolean atLeastOne = sessions.length >= 1;
				APIToolingView.this.removeActiveSessionAction.setEnabled(atLeastOne);
				APIToolingView.this.removeAllSessionsAction.setEnabled(atLeastOne);
				APIToolingView.this.selectSessionAction.setEnabled(atLeastOne);
				APIToolingView.this.exportSessionAction.setEnabled(active != null);
			}
		});
	}
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		this.viewer.getControl().setFocus();
	}
	public void sessionAdded(final ISession session) {
		this.viewer.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				APIToolingView.this.viewer.setInput(session);
			}
		});
		updateActions();
	}
	public void sessionRemoved(ISession session) {
		this.viewer.setInput(null);
		updateActions();
	}
	
	public void sessionActivated(final ISession session) {
		this.viewer.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				APIToolingView.this.viewer.setInput(session);
			}
		});
		updateActions();
	}
	/**
	 * Returns the property sheet.
	 */
	protected IPropertySheetPage getPropertySheet() {
		return new PropertySheetPage();
	}
	/* (non-Javadoc)
	 * Method declared on IAdaptable
	 */
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IPropertySheetPage.class)) {
			return getPropertySheet();
		}
		return super.getAdapter(adapter);
	}
}