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
package com.sonatype.s2.project.core.internal;


import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class S2ProjectPlugin
    extends Plugin
{

    // The plug-in ID
    public static final String PLUGIN_ID = "com.sonatype.s2.project.core";

    // The shared instance
    private static S2ProjectPlugin plugin;

    private BundleContext context;

    public BundleContext getContext()
    {
        return context;
    }

    public S2ProjectPlugin()
    {
    }

    public void start( BundleContext context )
        throws Exception
    {
        super.start( context );
        plugin = this;
        this.context = context;
    }

    public void stop( BundleContext context )
        throws Exception
    {
        this.context = null;
        plugin = null;

        super.stop( context );
    }

    public static S2ProjectPlugin getDefault()
    {
        return plugin;
    }
}
