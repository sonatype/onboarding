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

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.internal.WorkspaceCodebase;

public class SourceTreeRemoveOperation
    implements IUpdateOperation
{

    private final IWorkspaceCodebase originalCodebase;
    private final IWorkspaceCodebase codebase;

    public SourceTreeRemoveOperation(IWorkspaceCodebase originalCodebase )
    {
        this.originalCodebase = originalCodebase;
        this.codebase = originalCodebase.getPending();

        if ( this.codebase == null )
        {
            throw new IllegalArgumentException();
        }

        if ( ( (WorkspaceCodebase) codebase ).getS2Project() == null )
        {
            throw new IllegalArgumentException();
        }
        
    }

    public void run( IProgressMonitor monitor )
        throws CoreException, InterruptedException
    {
        // TODO logic to verify it is safe to remove local source tree and actually remove it

        // replaceWorkspaceCodebase removes all REMOVED source trees. this is model-only operation.
        S2ProjectCore.getInstance().replaceWorkspaceCodebase( originalCodebase, codebase );
    }

}
