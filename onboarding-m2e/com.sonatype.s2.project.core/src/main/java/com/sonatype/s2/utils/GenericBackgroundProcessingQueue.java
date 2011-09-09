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
package com.sonatype.s2.utils;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.core.jobs.IBackgroundProcessingQueue;

import com.sonatype.s2.project.core.internal.S2ProjectPlugin;

public abstract class GenericBackgroundProcessingQueue<R>
    extends Job
    implements IBackgroundProcessingQueue
{

    public static final long DEFAULT_SCHEDULE_DELAY = 1000L;

    public GenericBackgroundProcessingQueue( String name )
    {
        super( name );
    }

    private final ArrayList<R> queue = new ArrayList<R>();

    public boolean isEmpty()
    {
        synchronized ( queue )
        {
            return queue.isEmpty();
        }
    }

    public IStatus run( IProgressMonitor monitor )
    {
        ArrayList<R> requests = getRequests();

        return doRun( requests, monitor );
    }

    protected IStatus doRun( ArrayList<R> requests, IProgressMonitor monitor )
    {
        ArrayList<IStatus> problems = null;

        for ( R request : requests )
        {
            if ( monitor.isCanceled() )
            {
                return Status.CANCEL_STATUS;
            }

            try
            {
                process( request, monitor );
            }
            catch ( CoreException e )
            {
                if ( problems == null )
                {
                    problems = new ArrayList<IStatus>();
                }
                problems.add( e.getStatus() );
            }
        }

        if ( problems == null )
        {
            return Status.OK_STATUS;
        }

        return new MultiStatus( S2ProjectPlugin.PLUGIN_ID, IStatus.ERROR,
                                problems.toArray( new IStatus[problems.size()] ), null, null );
    }

    protected ArrayList<R> getRequests()
    {
        ArrayList<R> requests;
        synchronized ( queue )
        {
            requests = new ArrayList<R>( queue );
            queue.clear();
        }
        return requests;
    }

    protected abstract void process( R request, IProgressMonitor monitor )
        throws CoreException;

    public void schedule( R request )
    {
        schedule( request, DEFAULT_SCHEDULE_DELAY );
    }

    public void schedule( R request, long delay )
    {
        synchronized ( queue )
        {
            queue.add( request );
        }
        schedule( delay );
    }
}
