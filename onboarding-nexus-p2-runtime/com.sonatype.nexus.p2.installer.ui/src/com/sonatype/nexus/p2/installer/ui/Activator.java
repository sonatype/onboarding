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
package com.sonatype.nexus.p2.installer.ui;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * The activator class controls the plug-in life cycle
 */
@SuppressWarnings( "restriction" )
public class Activator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.sonatype.nexus.p2.installer.ui";

    private ServiceRegistration certificateUIRegistration;

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
    public void start( BundleContext context )
        throws Exception
    {
		super.start(context);
		plugin = this;
        certificateUIRegistration =
            context.registerService( IServiceUI.class.getName(), new ValidationDialogServiceUI(), null );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
        try
        {
            certificateUIRegistration.unregister();
        }
        finally
        {
            plugin = null;
            super.stop( context );
        }
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
