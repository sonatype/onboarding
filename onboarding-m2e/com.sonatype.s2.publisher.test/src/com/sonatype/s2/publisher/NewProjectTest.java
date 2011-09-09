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
package com.sonatype.s2.publisher;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.codebase.NewCodebaseProjectOperation;

public class NewProjectTest
    extends AbstractMavenProjectTestCase
{

    public void testDefaultLocation()
        throws Exception
    {
        createAndAssertProject( "foo" );
    }

    protected IProject createAndAssertProject( String projectName )
        throws CoreException
    {
        new NewCodebaseProjectOperation( projectName, "group.id", projectName, "1.2.3", null ).createProject( monitor );

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        IProject project = root.getProject( projectName );

        assertTrue( project.isOpen() );

        IFile pmdFile = project.getFile( S2PublisherConstants.PMD_PATH + "/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME );

        assertTrue( pmdFile.isAccessible() );

        return project;
    }
}
