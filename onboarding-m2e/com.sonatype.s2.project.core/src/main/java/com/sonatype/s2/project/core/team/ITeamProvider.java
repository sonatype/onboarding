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
package com.sonatype.s2.project.core.team;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;

import com.sonatype.s2.project.core.IWorkspaceSourceTree;

public interface ITeamProvider
{
    /**
     * call into team provider to detect the status
     * <ul>
     * <li>for svn this is root folder rev check</li>
     * <li>for git this is fetch and local/remote branch HEAD checks</li>
     * <li>for cvs this performs a dry update</li>
     * </ul>
     */
    public TeamOperationResult getUpdateStatus( IWorkspaceSourceTree sourceTree, IProgressMonitor monitor ) throws CoreException;

    public TeamOperationResult updateFromRepository( IWorkspaceSourceTree sourceTree, IProgressMonitor monitor )
        throws CoreException;

    public void afterProjectsImport( List<IMavenProjectImportResult> projectImportResults, File location,
                                     IProgressMonitor monitor )
        throws CoreException;
}
