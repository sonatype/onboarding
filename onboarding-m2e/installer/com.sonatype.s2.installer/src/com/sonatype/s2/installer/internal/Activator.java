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
package com.sonatype.s2.installer.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator
    implements BundleActivator
{
    // The plug-in ID
    public static final String PLUGIN_ID = "com.sonatype.s2.installer";

    private static BundleContext bundleContext;

    public static BundleContext getContext()
    {
        return bundleContext;
    }

    public void start( BundleContext context )
        throws Exception
    {
        bundleContext = context;
    }

    public void stop( BundleContext context )
        throws Exception
    {
        bundleContext = null;
    }
}
