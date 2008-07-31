/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package a.b.c;

/**
 * Test unsupported @noreference tag on private outer class constructors
 */
public class test32 {
	
}

class outer {
	/**
	 * Constructor
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	private outer() {
		
	}
	
	/**
	 * Constructor
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	private outer(int i) {
		
	}
}
