/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.core.plugin;

import org.eclipse.pde.core.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;

/**
 * @version 	1.0
 * @author
 */
public class AttributeChangedEvent extends ModelChangedEvent {
	public static final String P_ATTRIBUTE_VALUE = "att_value";
	private Object attribute;
	public AttributeChangedEvent(Object element, Object attribute, String oldValue, String newValue) {
		super(element, P_ATTRIBUTE_VALUE, oldValue, newValue);
		this.attribute = attribute;
	}
	
	public Object getChagedAttribute() {
		return attribute;
	}
}
