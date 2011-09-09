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

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.sonatype.s2.project.core.S2ProjectCore;

public class CodebaseUpdateAction
    implements IWorkbenchWindowActionDelegate
{
    public void run( IAction action )
    {
        Job job = new CodebaseUpdateJob( S2ProjectCore.getInstance().getWorkspaceCodebases().get( 0 ) );
        job.setUser( true );
        job.schedule();
    }

    public void selectionChanged( IAction action, ISelection selection )
    {
    }

    public void dispose()
    {
    }

    public void init( IWorkbenchWindow window )
    {
    }
}
