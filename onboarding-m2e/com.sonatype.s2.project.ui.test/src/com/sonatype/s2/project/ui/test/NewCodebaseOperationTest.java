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
package com.sonatype.s2.project.ui.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;

import junit.framework.Assert;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.integration.tests.common.UIIntegrationTestCase;
import org.junit.Test;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.S2ProjectFacade;
import com.sonatype.s2.project.ui.codebase.NewCodebaseProjectOperation;

public class NewCodebaseOperationTest
    extends UIIntegrationTestCase
{
    protected static File root = new File( "" ).getAbsoluteFile();

    @Test
    public void testCodebaseImport()
        throws Exception
    {
        IProject project = importCodebase( "codebasewithicon" );

        assertTrue( "Icon file in project codebasewithicon does not exist",
                    project.exists( new Path( IS2Project.PROJECT_DESCRIPTOR_PATH + "/"
                        + IS2Project.PROJECT_ICON_FILENAME ) ) );
    }

    @Test
    public void testCodebaseImportNoIcon()
        throws Exception
    {
        importCodebase( "codebasenoicon" );
    }

    @Test
    public void testCodebaseImportTwoVersions()
        throws Exception
    {
        // "import" version 1.0
        IProject project = importCodebase( "codebaseTwoVersions", "1.0", "codebaseTwoVersions/1.0" );
        Assert.assertTrue( "Icon file in project codebaseTwoVersions does not exist",
                    project.exists( new Path( IS2Project.PROJECT_DESCRIPTOR_PATH + "/"
                        + IS2Project.PROJECT_ICON_FILENAME ) ) );

        // Delete the project, but not the project files
        project.delete( false /* deleteContent */, true /* force */, monitor );

        // "import" version 2.0
        project = importCodebase( "codebaseTwoVersions", "2.0", "codebaseTwoVersions/2.0" );
        Assert.assertFalse( "Icon file in project codebaseTwoVersions exists",
                     project.exists( new Path( IS2Project.PROJECT_DESCRIPTOR_PATH + "/"
                         + IS2Project.PROJECT_ICON_FILENAME ) ) );
    }

    public IProject importCodebase( String name )
        throws Exception
    {
        return importCodebase( name, "1.0", name );
    }

    public IProject importCodebase( String name, String version, String codebaseSourceFolder )
        throws Exception
    {
        String groupId = "com.sonatype";
        IS2Project codebase = S2ProjectFacade.createProject( groupId, name, version );
        codebase.setName( name );

        final NewCodebaseProjectOperation codebaseProjectOperation =
            new NewCodebaseProjectOperation( name, codebase,
                                             new File( root, "resources/import/" + codebaseSourceFolder ).toURI().toString() );

        Job job = new Job( "Creating project " + name )
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                try
                {
                    codebaseProjectOperation.createProject( monitor, false );
                }
                catch ( CoreException e )
                {
                    return e.getStatus();
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
        job.join();
        assertTrue( job.getResult().toString(), job.getResult().isOK() );

        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject( name );
        assertTrue( "Project " + name + " exists", project.exists() );
        Path codebaseDescriptorPath =
            new Path( IS2Project.PROJECT_DESCRIPTOR_PATH + "/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME );
        assertTrue( "Codebase file in project " + name + " exists", project.exists( codebaseDescriptorPath ) );

        IFile importedCodebaseFile = project.getFile( codebaseDescriptorPath );
        InputStream is = importedCodebaseFile.getContents();
        try
        {
            IS2Project importedCodebase = S2ProjectFacade.loadProject( is, false /* strict */);
            assertEquals( groupId, importedCodebase.getGroupId() );
            assertEquals( name, importedCodebase.getArtifactId() );
            assertEquals( version, importedCodebase.getVersion() );
        }
        finally
        {
            IOUtil.close( is );
        }
        
        return project;
    }
}
