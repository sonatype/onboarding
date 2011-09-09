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
package com.sonatype.s2.project.ui.materialization.update;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.S2ProjectCore;

public abstract class AbstractCodebaseUpdateJob
    extends WorkspaceJob
{

    protected final IWorkspaceCodebase codebase;

    public AbstractCodebaseUpdateJob( IWorkspaceCodebase codebase, String name )
    {
        this( codebase, name, true );
    }

    public AbstractCodebaseUpdateJob( IWorkspaceCodebase codebase, String name, boolean pendingRequired )
    {
        super( name );

        if ( pendingRequired && codebase.getPending() == null )
        {
            throw new IllegalArgumentException();
        }

        setRule( ResourcesPlugin.getWorkspace().getRoot() );
        setUser( true );

        this.codebase = codebase;
    }

    @Override
    public IStatus runInWorkspace( IProgressMonitor monitor )
    {
        try
        {
            IStatus status = doRun( monitor );

            if (codebase != null)
            {
            	// TODO not nice to have it here
            	S2ProjectCore.getInstance().replaceWorkspaceCodebase( codebase, codebase.getPending() );
            }

            if ( status.isOK() )
            {
                afterSuccessfullUpdate( status );
            }
            else
            {
                afterFailedUpdate( status );
            }
        }
        catch ( CoreException e )
        {
            afterFailedUpdate( e.getStatus() );
            return e.getStatus();
        }
        catch ( InterruptedException e )
        {
            return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
    }

    protected void afterFailedUpdate( IStatus status )
    {
        Display.getDefault().asyncExec( new Runnable()
        {
            public void run()
            {
                try
                {
                    CodebaseUpdateView.open();
                }
                catch ( PartInitException e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } );
    }

    protected void afterSuccessfullUpdate( IStatus status )
    {
        // TODO say something smart and encouraging
    }

    protected abstract IStatus doRun( IProgressMonitor monitor )
        throws CoreException, InterruptedException;
}
