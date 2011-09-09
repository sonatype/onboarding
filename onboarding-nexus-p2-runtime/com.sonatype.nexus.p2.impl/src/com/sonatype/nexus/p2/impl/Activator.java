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
package com.sonatype.nexus.p2.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sonatype.nexus.p2.facade.internal.P2FacadeInternal;

public class Activator
    implements BundleActivator
{
    public static final String ID = "com.sonatype.nexus.p2.impl";
    private static Activator instance;

    private BundleContext context;

    public Activator()
    {
        this.instance = this;
    }

    public void start( BundleContext context )
        throws Exception
    {
        this.context = context;
        context.registerService( P2FacadeInternal.class.getName(), new P2FacadeInternalImpl(), null );
    }

    public void stop( BundleContext context )
        throws Exception
    {
    }

    public static BundleContext getContext()
    {
        return instance.context;
    }
}
