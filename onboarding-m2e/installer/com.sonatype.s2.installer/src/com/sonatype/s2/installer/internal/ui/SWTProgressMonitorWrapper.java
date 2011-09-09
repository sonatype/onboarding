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
package com.sonatype.s2.installer.internal.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;

public class SWTProgressMonitorWrapper
    implements IProgressMonitor
{

    private final Display display;

    private final IProgressMonitor monitor;

    public SWTProgressMonitorWrapper( Display display, IProgressMonitor monitor )
    {
        this.display = display;
        this.monitor = monitor;
    }

    public void beginTask( final String name, final int totalWork )
    {
        display.syncExec( new Runnable()
        {
            public void run()
            {
                monitor.beginTask( name, totalWork );
            }
        } );
    }

    public void done()
    {
        display.syncExec( new Runnable()
        {
            public void run()
            {
                monitor.done();
            }
        } );
    }

    public void internalWorked( final double work )
    {
        display.syncExec( new Runnable()
        {
            public void run()
            {
                monitor.internalWorked( work );
            }
        } );
    }

    public boolean isCanceled()
    {
        final boolean[] result = new boolean[1];

        display.syncExec( new Runnable()
        {
            public void run()
            {
                result[0] = monitor.isCanceled();
            }
        } );

        return result[0];
    }

    public void setCanceled( final boolean value )
    {
        display.syncExec( new Runnable()
        {
            public void run()
            {
                monitor.setCanceled( value );
            }
        } );
    }

    public void setTaskName( final String name )
    {
        display.syncExec( new Runnable()
        {
            public void run()
            {
                monitor.setTaskName( name );
            }
        } );
    }

    public void subTask( final String name )
    {
        display.syncExec( new Runnable()
        {
            public void run()
            {
                monitor.subTask( name );
            }
        } );
    }

    public void worked( final int work )
    {
        display.syncExec( new Runnable()
        {
            public void run()
            {
                monitor.worked( work );
            }
        } );
    }

}
