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
package com.sonatype.s2.project.ui.lineup.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISaveableFilter;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.Saveable;

import com.sonatype.s2.project.ui.lineup.Messages;

public class LineupValidateAction
    implements IEditorActionDelegate
{
    private LineupEditor lineupEditor;

    public void run( final IAction action )
    {
        assert lineupEditor != null;
        if ( !checkOpenEditors() )
        {
            return;
        }

        action.setEnabled( false );
        Job job = new Job( Messages.lineupValidateAction_job )
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                lineupEditor.validateLineup( monitor );
                return Status.OK_STATUS;
            }
        };
        job.setUser( true );
        job.schedule();
        job.addJobChangeListener( new JobChangeAdapter()
        {
            @Override
            public void done( IJobChangeEvent event )
            {
                action.setEnabled( true );
            }
        } );
    }

    public void selectionChanged( IAction action, ISelection selection )
    {
    }

    public void setActiveEditor( IAction action, IEditorPart targetEditor )
    {
        assert targetEditor instanceof LineupEditor;
        lineupEditor = (LineupEditor) targetEditor;
    }

    protected boolean checkOpenEditors()
    {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();

        if ( lineupEditor.isDirty() )
        {
            return workbench.saveAll( workbenchWindow, workbenchWindow, new ISaveableFilter()
            {
                public boolean select( Saveable saveable, IWorkbenchPart[] containingParts )
                {
                    for ( IWorkbenchPart part : containingParts )
                    {
                        if ( part.equals( lineupEditor ) )
                        {
                            return true;
                        }
                    }
                    return false;
                }
            }, true );
        }
        return true;
    }
}
