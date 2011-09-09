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

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;

import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.team.ITeamProvider;
import com.sonatype.s2.project.core.team.TeamOperationResult;

public class NullTeamProvider
    implements ITeamProvider
{
    private static final TeamOperationResult RESULT_UNKNOWN =
        new TeamOperationResult( IWorkspaceSourceTree.STATUS_NOT_SUPPORTED, null, null );

    public TeamOperationResult getUpdateStatus( IWorkspaceSourceTree sourceTree, IProgressMonitor monitor )
    {
        return RESULT_UNKNOWN;
    }

    public TeamOperationResult updateFromRepository( IWorkspaceSourceTree tree, IProgressMonitor monitor )
    {
        return RESULT_UNKNOWN;
    }

    public void afterProjectsImport( List<IMavenProjectImportResult> projectImportResults, File location,
                                     IProgressMonitor monitor )
        throws CoreException
    {
    }

}
