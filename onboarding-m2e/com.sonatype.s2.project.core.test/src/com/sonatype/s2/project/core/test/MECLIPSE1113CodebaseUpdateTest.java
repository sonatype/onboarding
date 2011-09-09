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
package com.sonatype.s2.project.core.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.internal.update.AbstractSourceTreeOperation;
import com.sonatype.s2.project.core.internal.update.CodebaseUpdateOperation;
import com.sonatype.s2.project.core.internal.update.DetermineCodebaseUpdateStatusOperation;
import com.sonatype.s2.project.core.test.scm.FileTeamProvider;
import com.sonatype.s2.project.model.IS2Project;

public class MECLIPSE1113CodebaseUpdateTest
    extends AbstractMavenProjectMaterializationTest
{
    private static final String CODEBASES_FOLDER = "target/codebases";

    private S2ProjectCore core;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        FileUtils.deleteDirectory( new File( CODEBASES_FOLDER ) );

        core = S2ProjectCore.getInstance();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            core = null;

            FileUtils.deleteDirectory( new File( CODEBASES_FOLDER ) );
        }
        finally
        {
            super.tearDown();
        }
    }

    /**
     * @param targetPath must match mse-codebase.xml!
     */
    private File getBasedir( String sourcePath, String targetPath )
        throws IOException
    {
        File src = new File( sourcePath );

        File dst = new File( CODEBASES_FOLDER, targetPath );
        FileUtils.deleteDirectory( dst );

        FileUtils.copyDirectoryStructure( src, dst );

        filterCodebaseDescriptor( new File( sourcePath, IS2Project.PROJECT_DESCRIPTOR_FILENAME ),
                                  new File( dst, IS2Project.PROJECT_DESCRIPTOR_FILENAME ) );

        return dst;
    }

    private List<IProject> getProjects( IWorkspaceSourceTree tree )
    {
        Map<IPath, IProject> projectsMap = AbstractSourceTreeOperation.getWorkspaceProjects( tree );

        SortedMap<String, IProject> sortedProjects = new TreeMap<String, IProject>();

        for ( IProject project : projectsMap.values() )
        {
            sortedProjects.put( project.getName(), project );
        }

        return new ArrayList<IProject>( sortedProjects.values() );
    }

    public void testWorkspaceCodebaseRegistry()
        throws Exception
    {
        File basedir =
            getBasedir( "resources/projects/MECLIPSE-1113-codebase-update/basic", "MECLIPSE-1113-codebase-update/basic" );

        materialize( new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ).getCanonicalPath(), false );

        List<IWorkspaceCodebase> codebases = core.getWorkspaceCodebases();
        assertEquals( 1, codebases.size() );

        IWorkspaceCodebase codebase = codebases.get( 0 );

        assertNotNull( codebase.getDescriptorUrl() );
        assertNotNull( codebase.getS2Project() );

        List<IWorkspaceSourceTree> trees = codebase.getSourceTrees();
        assertEquals( 1, trees.size() );

        IWorkspaceSourceTree tree = trees.get( 0 );

        List<IProject> projects = getProjects( tree );

        assertEquals( 2, projects.size() );

        assertEquals( "project01", projects.get( 0 ).getName() );
        assertEquals( "tree01", projects.get( 1 ).getName() );
        assertNull( tree.getStatus() );

        assertEquals( projects.get( 1 ).getLocation().toFile().getCanonicalPath(), tree.getLocation() );
    }

    public void testNoChangeCodebaseStatusCalculation()
        throws Exception
    {
        File basedir =
            getBasedir( "resources/projects/MECLIPSE-1113-codebase-update/basic", "MECLIPSE-1113-codebase-update/basic" );

        materialize( new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ).getCanonicalPath(), false );

        assertEquals( 1, core.getWorkspaceCodebases().get( 0 ).getSourceTrees().size() );

        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 ).getPending();
        assertEquals( 1, codebase.getSourceTrees().size() );
        assertEquals( IWorkspaceSourceTree.STATUS_UPTODATE, codebase.getSourceTrees().get( 0 ).getStatus() );
    }

    public void testNewSourceTreeCodebaseStatusCalculation()
        throws Exception
    {
        File basedir =
            getBasedir( "resources/projects/MECLIPSE-1113-codebase-update/basic", "MECLIPSE-1113-codebase-update/basic" );

        materialize( new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ).getCanonicalPath(), false );

        // add new source tree

        filterCodebaseDescriptor( new File(
                                            "resources/projects/MECLIPSE-1113-codebase-update/basic/mse-codebase-add-tree02.xml" ),
                                  new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ) );

        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 );
        assertEquals( 1, codebase.getSourceTrees().size() );

        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        codebase = core.getWorkspaceCodebases().get( 0 ).getPending();
        assertEquals( 2, codebase.getSourceTrees().size() );
        assertEquals( IWorkspaceSourceTree.STATUS_UPTODATE, codebase.getSourceTrees().get( 0 ).getStatus() );
        assertEquals( IWorkspaceSourceTree.STATUS_ADDED, codebase.getSourceTrees().get( 1 ).getStatus() );

        // restore original codebase descriptor

        filterCodebaseDescriptor( new File( "resources/projects/MECLIPSE-1113-codebase-update/basic/mse-codebase.xml" ),
                                  new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ) );

        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        codebase = core.getWorkspaceCodebases().get( 0 ).getPending();
        assertEquals( 1, codebase.getSourceTrees().size() );
    }

    public void testRemoveSourceTreeCodebaseStatusCalculation()
        throws Exception
    {
        File basedir =
            getBasedir( "resources/projects/MECLIPSE-1113-codebase-update/basic", "MECLIPSE-1113-codebase-update/basic" );
        filterCodebaseDescriptor( new File(
                                            "resources/projects/MECLIPSE-1113-codebase-update/basic/mse-codebase-add-tree02.xml" ),
                                  new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ) );

        materialize( new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ).getCanonicalPath(), false );

        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 );

        assertEquals( 2, codebase.getSourceTrees().size() );

        filterCodebaseDescriptor( new File( "resources/projects/MECLIPSE-1113-codebase-update/basic/mse-codebase.xml" ),
                                  new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ) );

        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        codebase = core.getWorkspaceCodebases().get( 0 ).getPending();
        assertEquals( 2, codebase.getSourceTrees().size() );
        assertEquals( IWorkspaceSourceTree.STATUS_UPTODATE, codebase.getSourceTrees().get( 0 ).getStatus() );
        assertEquals( IWorkspaceSourceTree.STATUS_NOT_SUPPORTED, codebase.getSourceTrees().get( 1 ).getStatus() );
    }

    public void testRemoveSourceTree()
        throws Exception
    {
        File basedir =
            getBasedir( "resources/projects/MECLIPSE-1113-codebase-update/basic", "MECLIPSE-1113-codebase-update/basic" );
        filterCodebaseDescriptor( new File(
                                            "resources/projects/MECLIPSE-1113-codebase-update/basic/mse-codebase-add-tree02.xml" ),
                                  new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ) );

        materialize( new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ).getCanonicalPath(), false );

        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 );

        assertEquals( 2, codebase.getSourceTrees().size() );

        filterCodebaseDescriptor( new File( "resources/projects/MECLIPSE-1113-codebase-update/basic/mse-codebase.xml" ),
                                  new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ) );

        getWorkspaceProject( "tree02" ).delete( true, true, monitor );
        waitForJobsToComplete();

        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        codebase = core.getWorkspaceCodebases().get( 0 ).getPending();
        assertEquals( 2, codebase.getSourceTrees().size() );
        assertEquals( IWorkspaceSourceTree.STATUS_UPTODATE, codebase.getSourceTrees().get( 0 ).getStatus() );
        assertEquals( IWorkspaceSourceTree.STATUS_REMOVED, codebase.getSourceTrees().get( 1 ).getStatus() );

        new CodebaseUpdateOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );
        waitForJobsToComplete();

        codebase = core.getWorkspaceCodebases().get( 0 );
        assertEquals( 1, codebase.getSourceTrees().size() );
        assertNull( codebase.getSourceTrees().get( 0 ).getStatus() );
    }

    public void testImportNewSourceTree()
        throws Exception
    {
        File basedir =
            getBasedir( "resources/projects/MECLIPSE-1113-codebase-update/basic", "MECLIPSE-1113-codebase-update/basic" );

        materialize( new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ).getCanonicalPath(), false );

        filterCodebaseDescriptor( new File(
                                            "resources/projects/MECLIPSE-1113-codebase-update/basic/mse-codebase-add-tree02.xml" ),
                                  new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ) );

        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        new CodebaseUpdateOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        assertWorkspaceProjects( 3 );

        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 );
        assertEquals( 2, codebase.getSourceTrees().size() );
        assertNull( codebase.getSourceTrees().get( 0 ).getStatus() );
        assertNull( codebase.getSourceTrees().get( 1 ).getStatus() );

        FileTeamProvider.assertTeamProviderEnabled( getWorkspaceProject( "tree02" ) );
    }

    public void testArtifactIdChange()
        throws Exception
    {
        File basedir =
            getBasedir( "resources/projects/MECLIPSE-1113-codebase-update/basic", "MECLIPSE-1113-codebase-update/basic" );

        materialize( new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ).getCanonicalPath(), false );
        waitForJobsToComplete();

        FileUtils.copyFile( new File(
                                      "resources/projects/MECLIPSE-1113-codebase-update/basic/tree01/pom-artifactId-change.xml" ),
                            new File( basedir, "tree01/pom.xml" ) );
        FileUtils.copyFile( new File(
                                      "resources/projects/MECLIPSE-1113-codebase-update/basic/tree01/project01/pom-artifactId-change.xml" ),
                            new File( basedir, "tree01/project01/pom.xml" ) );

        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 ).getPending();
        assertEquals( IWorkspaceSourceTree.STATUS_CHANGED, codebase.getSourceTrees().get( 0 ).getStatus() );

        new CodebaseUpdateOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );
        waitForJobsToComplete();

        codebase = core.getWorkspaceCodebases().get( 0 );
        assertNull( codebase.getPending() );
        assertNull( codebase.getSourceTrees().get( 0 ).getStatus() );
        assertEquals( 2, getProjects( codebase.getSourceTrees().get( 0 ) ).size() );

        assertWorkspaceProjects( 2 );
        assertMavenProject( "MECLIPSE-1113-basic", "tree01-changed", "0.0.1-SNAPSHOT" );
        assertMavenProject( "MECLIPSE-1113-basic", "project01-changed", "0.0.1-SNAPSHOT" );
    }

    public void testProjectRemoved()
        throws Exception
    {
        File basedir =
            getBasedir( "resources/projects/MECLIPSE-1113-codebase-update/basic", "MECLIPSE-1113-codebase-update/basic" );

        materialize( new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ).getCanonicalPath(), false );
        waitForJobsToComplete();

        FileUtils.copyFile( new File(
                                      "resources/projects/MECLIPSE-1113-codebase-update/basic/tree01/pom-modules-removed.xml" ),
                            new File( basedir, "tree01/pom.xml" ) );
        FileUtils.deleteDirectory( new File( basedir, "tree01/project01" ) );

        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 );

        new DetermineCodebaseUpdateStatusOperation( codebase ).run( monitor );

        codebase = core.getWorkspaceCodebases().get( 0 ).getPending();

        assertEquals( IWorkspaceSourceTree.STATUS_CHANGED, codebase.getSourceTrees().get( 0 ).getStatus() );

        new CodebaseUpdateOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );
        waitForJobsToComplete();

        codebase = core.getWorkspaceCodebases().get( 0 );
        assertNull( codebase.getPending() );

        assertWorkspaceProjects( 1 );
        assertMavenProject( "MECLIPSE-1113-basic", "tree01", "0.0.1-SNAPSHOT" );

        assertEquals( 1, getProjects( codebase.getSourceTrees().get( 0 ) ).size() );
    }

    public void testSourceTreeProfilesChange()
        throws Exception
    {
        File basedir =
            getBasedir( "resources/projects/MECLIPSE-1113-codebase-update/profiles",
                        "MECLIPSE-1113-codebase-update/profiles" );

        materialize( new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ).getCanonicalPath(), false );

        assertWorkspaceProjects( 1 );

        filterCodebaseDescriptor( new File(
                                            "resources/projects/MECLIPSE-1113-codebase-update/profiles/mse-codebase-add-profile.xml" ),
                                  new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ) );

        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 ).getPending();
        assertEquals( IWorkspaceSourceTree.STATUS_CHANGED, codebase.getSourceTrees().get( 0 ).getStatus() );

        new CodebaseUpdateOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );
        waitForJobsToComplete();

        codebase = core.getWorkspaceCodebases().get( 0 );
        assertNull( codebase.getPending() );
        assertNull( codebase.getSourceTrees().get( 0 ).getStatus() );

        assertWorkspaceProjects( 2 );

        assertMavenProject( "MECLIPSE-1113-profiles", "tree01", "0.0.1-SNAPSHOT" );
        assertMavenProject( "MECLIPSE-1113-profiles", "project01", "0.0.1-SNAPSHOT" );

        FileTeamProvider.assertTeamProviderEnabled( getWorkspaceProject( "project01" ) );
    }

    public void testSourceTreeRootsChange()
        throws Exception
    {
        File basedir =
            getBasedir( "resources/projects/MECLIPSE-1113-codebase-update/roots", "MECLIPSE-1113-codebase-update/roots" );

        materialize( new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ).getCanonicalPath(), false );

        assertWorkspaceProjects( 1 );

        filterCodebaseDescriptor( new File(
                                            "resources/projects/MECLIPSE-1113-codebase-update/roots/mse-codebase-add-profile.xml" ),
                                  new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ) );

        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 ).getPending();
        assertEquals( IWorkspaceSourceTree.STATUS_CHANGED, codebase.getSourceTrees().get( 0 ).getStatus() );

        new CodebaseUpdateOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );
        waitForJobsToComplete();

        codebase = core.getWorkspaceCodebases().get( 0 );
        assertNull( codebase.getPending() );
        assertNull( codebase.getSourceTrees().get( 0 ).getStatus() );

        assertWorkspaceProjects( 2 );

        assertMavenProject( "MECLIPSE-1113-roots", "project01", "0.0.1-SNAPSHOT" );
        assertMavenProject( "MECLIPSE-1113-roots", "project01", "0.0.1-SNAPSHOT" );
    }

    public void testCheckoutRootRemoved()
        throws Exception
    {
        File basedir =
            getBasedir( "resources/projects/MECLIPSE-1113-codebase-update/basic", "MECLIPSE-1113-codebase-update/basic" );

        materialize( new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ).getCanonicalPath(), false );

        assertEquals( 1, core.getWorkspaceCodebases().get( 0 ).getSourceTrees().size() );

        getWorkspaceProject( "tree01" ).delete( true, monitor );
        waitForJobsToComplete();

        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 ).getPending();
        assertEquals( IWorkspaceSourceTree.STATUS_ADDED, codebase.getSourceTrees().get( 0 ).getStatus() );
    }

    public void testUpdateUnsupported()
        throws Exception
    {
        File basedir =
            getBasedir( "resources/projects/MECLIPSE-1113-codebase-update/basic", "MECLIPSE-1113-codebase-update/basic" );

        materialize( new File( basedir, "mse-codebase-add-tree02.xml" ).getCanonicalPath(), false );

        FileUtils.copyFile( new File( "resources/projects/MECLIPSE-1113-codebase-update/basic/mse-codebase.xml" ),
                            new File( basedir, "mse-codebase-add-tree02.xml" ) );

        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 ).getPending();

        assertEquals( IWorkspaceSourceTree.STATUS_NOT_SUPPORTED, codebase.getSourceTrees().get( 1 ).getStatus() );

        try
        {
            new CodebaseUpdateOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );
            fail();
        }
        catch ( CoreException e )
        {
            // expected
        }
    }

    public void _testAddBadModule()
        throws Exception
    {
        File basedir =
            getBasedir( "resources/projects/MECLIPSE-1113-codebase-update/bad-module",
                        "MECLIPSE-1113-codebase-update/bad-module" );

        materialize( new File( basedir, "mse-codebase.xml" ).getCanonicalPath(), false );

        assertWorkspaceProjects( 1 );

        FileUtils.copyFile( new File(
                                      "resources/projects/MECLIPSE-1113-codebase-update/bad-module/tree01/pom-add-module.xml" ),
                            new File( basedir, "tree01/pom.xml" ) );

        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        new CodebaseUpdateOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );
    }

    public void testUpdateOverlapsWithLocalDirectory()
        throws Exception
    {
        File basedir =
            getBasedir( "resources/projects/MECLIPSE-1113-codebase-update/basic", "MECLIPSE-1113-codebase-update/basic" );

        materialize( new File( basedir, "mse-codebase.xml" ).getCanonicalPath(), false );

        File workspaceBasedir = workspace.getRoot().getLocation().toFile();

        FileUtils.copyDirectoryStructure( new File( "resources/projects/MECLIPSE-1113-codebase-update/basic/tree02" ),
                                          new File( workspaceBasedir, "tree02" ) );

        filterCodebaseDescriptor( new File(
                                            "resources/projects/MECLIPSE-1113-codebase-update/basic/mse-codebase-add-tree02.xml" ),
                                  new File( basedir, "mse-codebase.xml" ) );

        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 ).getPending();

        assertEquals( 2, codebase.getSourceTrees().size() );
        assertEquals( IWorkspaceSourceTree.STATUS_NOT_SUPPORTED, codebase.getSourceTrees().get( 1 ).getStatus() );
    }
}
