/**
 * Copyright (c) 2008-2010 Sonatype, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sonatype, Inc. - initial API and implementation
 */
package com.sonatype.s2.internal.extractor;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {
	public final static String PLUGIN_ID = "com.sonatype.s2.extractor.helios";

	private static BundleContext context = null;

	private static Activator plugin;

	
	public void start(BundleContext context) throws Exception {
		Activator.context = context;
		plugin = null;
	}

	public void stop(BundleContext context) throws Exception {
		Activator.context = null;
		context = null;
	}

	public static BundleContext getContext() {
		return context;
	}
	
	public static Activator getDefault() {
		return plugin;
	}
}
