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
package com.sonatype.s2.project.ui.codebase;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator
    extends AbstractUIPlugin
{
    public static final String PLUGIN_ID = "com.sonatype.s2.project.ui.codebase";

    private static Activator plugin;

    public Activator()
    {
    }

    public void start( BundleContext context )
        throws Exception
    {
        super.start( context );
        plugin = this;
    }

    public void stop( BundleContext context )
        throws Exception
    {
        plugin = null;
        super.stop( context );
    }

    public static Activator getDefault()
    {
        return plugin;
    }
}
