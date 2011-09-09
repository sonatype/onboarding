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
package com.sonatype.s2.project.integration.test;

import org.eclipse.core.runtime.IProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleProgressMonitor
    implements IProgressMonitor
{

    private final Logger log = LoggerFactory.getLogger( ConsoleProgressMonitor.class );

    private String taskName;

    private double totalWorked;

    private long lastUpdate;

    private boolean cancelled;

    public void beginTask( String name, int totalWork )
    {
        log.info( "TASK: " + name + " (" + totalWork + ")" );
        lastUpdate = System.currentTimeMillis();
        taskName = name;
        totalWorked = 0;
    }

    public void setTaskName( String name )
    {
        taskName = name;
    }

    public void subTask( String name )
    {
        long now = System.currentTimeMillis();
        if ( now - lastUpdate > 10 * 1000 )
        {
            lastUpdate = now;
            log.info( "SUB-TASK: " + name );
        }
    }

    public void done()
    {
        log.info( "DONE: " + taskName );
        lastUpdate = System.currentTimeMillis();
    }

    public void worked( int work )
    {
        internalWorked( work );
    }

    public void internalWorked( double work )
    {
        totalWorked += work;
        long now = System.currentTimeMillis();
        if ( now - lastUpdate > 10 * 1000 )
        {
            lastUpdate = now;
            log.info( "WORKED: " + totalWorked );
        }
    }

    public boolean isCanceled()
    {
        return cancelled;
    }

    public void setCanceled( boolean value )
    {
        if ( value )
        {
            log.info( "CANCELLED: " + taskName );
        }
        cancelled = value;
    }

}
