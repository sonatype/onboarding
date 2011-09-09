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
package com.sonatype.s2.project.core.internal.update;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.ide.IDEUpdater;
import com.sonatype.s2.project.core.ide.IIDEUpdater;

public class P2LineupUpdateOperation
    implements IUpdateOperation
{
    IWorkspaceCodebase codebase;

    public P2LineupUpdateOperation( IWorkspaceCodebase codebase )
    {
        this.codebase = codebase;
    }

    public void run( IProgressMonitor monitor )
        throws CoreException, InterruptedException
    {
        IIDEUpdater updater = IDEUpdater.getUpdater();
        IStatus status = updater.performUpdate( codebase.getP2LineupLocation(), monitor );

        if ( !status.isOK() )
        {
            throw new CoreException( status );
        }
    }
}
