/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text.cheatsheet.simple;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;

/**
 * SimpleCSAction
 *
 */
public class SimpleCSAction extends SimpleCSObject implements ISimpleCSAction {

	private static final long serialVersionUID = 1L;

	private static final int F_MAX_PARAMS = 9;

	// TODO: MP: TEO: Verify translate of paramaters on write is okay - no translate before
	
	/**
	 * @param model
	 */
	public SimpleCSAction(ISimpleCSModel model) {
		super(model, ELEMENT_ACTION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getClazz()
	 */
	public String getClazz() {
		return getXMLAttributeValue(ATTRIBUTE_CLASS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getParam(int)
	 */
	public String getParam(int index) {
		// Ensure in valid range
		if ((index < 1) ||
				(index > F_MAX_PARAMS)) {
			return null;
		}
		StringBuffer buffer = new StringBuffer(ATTRIBUTE_PARAM);
		buffer.append(index);
		// Get paramN
		return getXMLAttributeValue(buffer.toString());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getParams()
	 */
	public String[] getParams() {
		ArrayList list = new ArrayList();
		// Get all set parameters
		for (int i = 1; i <= F_MAX_PARAMS; i++) {
			String parameter = getParam(i);
			if (parameter == null) {
				break;
			}
			list.add(parameter);
		}
		return (String[])list.toArray(new String[list.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getPluginId()
	 */
	public String getPluginId() {
		return getXMLAttributeValue(ATTRIBUTE_PLUGINID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#setClazz(java.lang.String)
	 */
	public void setClazz(String clazz) {
		setXMLAttribute(ATTRIBUTE_CLASS, clazz);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#setParam(java.lang.String, int)
	 */
	public void setParam(String param, int index) {
		// Ensure proper index
		if ((index < 1) ||
				(index > F_MAX_PARAMS)) {
			return;
		}
		StringBuffer buffer = new StringBuffer(ATTRIBUTE_PARAM);
		buffer.append(index);
		setXMLAttribute(buffer.toString(), param);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#setPluginId(java.lang.String)
	 */
	public void setPluginId(String pluginId) {
		setXMLAttribute(ATTRIBUTE_PLUGINID, pluginId);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#getConfirm()
	 */
	public boolean getConfirm() {
		return getBooleanAttributeValue(ATTRIBUTE_CONFIRM, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#getTranslate()
	 */
	public String getTranslate() {
		return getXMLAttributeValue(ATTRIBUTE_TRANSLATE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#getWhen()
	 */
	public String getWhen() {
		return getXMLAttributeValue(ATTRIBUTE_WHEN);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#setConfirm(boolean)
	 */
	public void setConfirm(boolean confirm) {
		setBooleanAttributeValue(ATTRIBUTE_CONFIRM, confirm);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#setTranslate(java.lang.String)
	 */
	public void setTranslate(String translate) {
		setXMLAttribute(ATTRIBUTE_TRANSLATE, translate);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#setWhen(java.lang.String)
	 */
	public void setWhen(String when) {
		setXMLAttribute(ATTRIBUTE_WHEN, when);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		return new ArrayList();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		// Leave as is.  Not a separate node in tree view
		return ELEMENT_ACTION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_ACTION;
	}

}
