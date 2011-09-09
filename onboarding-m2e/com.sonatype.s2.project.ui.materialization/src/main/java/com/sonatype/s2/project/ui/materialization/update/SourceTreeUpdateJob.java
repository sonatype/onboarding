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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.internal.update.SourceTreeUpdateOperation;

@SuppressWarnings( "restriction" )
public class SourceTreeUpdateJob
    extends AbstractCodebaseUpdateJob
{

    private final IWorkspaceSourceTree sourceTree;

    public SourceTreeUpdateJob( IWorkspaceCodebase codebase, IWorkspaceSourceTree sourceTree )
    {
        super( codebase, "Source tree update" );
        this.sourceTree = sourceTree;
    }

    @Override
    protected IStatus doRun( IProgressMonitor monitor )
        throws CoreException, InterruptedException
    {
        SourceTreeUpdateOperation op = new SourceTreeUpdateOperation( sourceTree );
        op.run( monitor );
        return Status.OK_STATUS;
    }

}
