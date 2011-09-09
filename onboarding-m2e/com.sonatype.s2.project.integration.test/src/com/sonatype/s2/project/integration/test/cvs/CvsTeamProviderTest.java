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
package com.sonatype.s2.project.integration.test.cvs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.operations.CheckoutSingleProjectOperation;
import org.eclipse.team.internal.ccvs.ui.operations.ShareProjectOperation;
import org.eclipse.team.internal.ccvs.ui.wizards.CommitWizard.AddAndCommitOperation;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;

import com.sonatype.m2e.cvs.internal.CVSURI;
import com.sonatype.m2e.cvs.internal.CvsTeamProvider;
import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.team.TeamOperationResult;
import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.integration.test.AbstractMavenProjectMaterializationTest;
import com.sonatype.s2.project.tests.common.Util;

@SuppressWarnings( "restriction" )
public class CvsTeamProviderTest
    extends AbstractMavenProjectMaterializationTest
{
    private static final String SRC = "/src/Main.java";

    private static final String PROJECT = "cvs-update-project";

    private static final String SERVER = "cvs://:pserver:scmtest.grid.sonatype.com:/cvs#mse/s2/automated-tests/";

    private static final String USER = "hudson";

    private static final String PASS = "hudson";

    private String TMP_PROJECT;

    private ICVSRepositoryLocation repository;

    private HttpServer httpServer;

    private File workspaceFile;

    private void checkoutTempProject()
        throws Exception
    {
        TMP_PROJECT = UUID.randomUUID().toString();

        CVSURI uri = CVSURI.fromUri( URI.create( SERVER + System.getProperty( "user.name" ) ) );
        repository = uri.getRepository();
        repository.setUsername( USER );
        repository.setPassword( PASS );

        ICVSRemoteFolder folder = repository.getRemoteFolder( uri.getPath().toString(), new CVSTag() );
        IProject tmpProject = workspace.getRoot().getProject( TMP_PROJECT );
        workspaceFile = workspace.getRoot().getLocation().toFile();
        if ( folder.exists( monitor ) )
        {
            checkout( folder, tmpProject );
            copyFiles( new File( "resources/scm/cvs/cvs-update-test" ).getAbsoluteFile(),
                       new File( workspaceFile, TMP_PROJECT ).getAbsoluteFile() );
            tmpProject.refreshLocal( IResource.DEPTH_INFINITE, monitor );
            commit( tmpProject );
        }
        else
        {
            tmpProject.create( monitor );

            copyFiles( new File( "resources/scm/cvs/cvs-update-test/" ).getAbsoluteFile(),
                       new File( workspaceFile, TMP_PROJECT ).getAbsoluteFile() );
            tmpProject.open( monitor );
            tmpProject.refreshLocal( IResource.DEPTH_INFINITE, monitor );

            shareProject( tmpProject, uri.getPath().toString() );
        }

        materialize( httpServer.getHttpUrl() + "/projects/cvs-codebase-update.xml" );
    }

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        httpServer = startHttpServer();
    }

    @Override
    public void tearDown()
        throws Exception
    {
        try
        {
            IProject tmpProject = getWorkspaceProject(TMP_PROJECT);
            if ( tmpProject != null )
            {
                for ( IResource resource : tmpProject.members() )
                {
                    if ( resource.getName().equals( "Main2.java" ) )
                    {
                        resource.delete( true, monitor );
                        break;
                    }
                }
                commit( getWorkspaceProject( TMP_PROJECT ) );
            }
            if ( httpServer != null )
            {
                httpServer.stop();
            }
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testIncomingChange()
        throws Exception
    {
        checkoutTempProject();

        setContents( getWorkspaceProject( TMP_PROJECT ), "" );
        commit( getWorkspaceProject( TMP_PROJECT ) );

        CvsTeamProvider provider = new CvsTeamProvider();
        IWorkspaceSourceTree sourceTree =
            S2ProjectCore.getInstance().getWorkspaceCodebases().get( 0 ).getSourceTrees().get( 0 );
        assertEquals( "Expected updates available", TeamOperationResult.RESULT_CHANGED.getStatus(),
                      provider.getUpdateStatus( sourceTree, monitor ).getStatus() );
    }

    // Need to figure out how to commit a deleted file
    // public void testIncomingFile()
    // throws Exception
    // {
    // writeToFile( "A new file", new File( workspaceFile, TMP_PROJECT + "/src/Main2.java" ) );
    // refresh( TMP_PROJECT );
    // commit( workspace.getRoot().getProject( TMP_PROJECT ) );
    //
    // CvsTeamProvider provider = new CvsTeamProvider();
    // IWorkspaceSourceTree sourceTree =
    // S2ProjectCore.getInstance().getWorkspaceCodebases().get( 0 ).getSourceTrees().get( 0 );
    // assertEquals( "Expected updates available", TeamOperationResult.RESULT_CHANGED.getStatus(),
    // provider.getUpdateStatus( sourceTree, monitor ).getStatus() );
    //
    // assertEquals( "Update should have succeeded", TeamOperationResult.RESULT_UPTODATE.getStatus(),
    // provider.updateFromRepository( sourceTree, monitor ) );
    //
    // assertEquals( "Codebase should be up to date", TeamOperationResult.RESULT_UPTODATE.getStatus(),
    // provider.getUpdateStatus( sourceTree, monitor ).getStatus() );
    // }

    public void testMergeConflict()
        throws Exception
    {
        checkoutTempProject();

        setContents( getWorkspaceProject( TMP_PROJECT ), "1" );
        setContents( getWorkspaceProject( PROJECT ), "2" );
        commit( getWorkspaceProject( TMP_PROJECT ) );

        CvsTeamProvider provider = new CvsTeamProvider();
        IWorkspaceSourceTree sourceTree =
            S2ProjectCore.getInstance().getWorkspaceCodebases().get( 0 ).getSourceTrees().get( 0 );
        assertEquals( "Expected updates available", TeamOperationResult.RESULT_CHANGED.getStatus(),
                      provider.getUpdateStatus( sourceTree, monitor ).getStatus() );

        assertEquals( "Codebase should have merge conflicts", IWorkspaceSourceTree.STATUS_NOT_SUPPORTED,
                      provider.updateFromRepository( sourceTree, monitor ).getStatus() );

        assertEquals( "Expected updates available", TeamOperationResult.RESULT_CHANGED.getStatus(),
                      provider.getUpdateStatus( sourceTree, monitor ).getStatus() );
    }

    public void testMergableConflict()
        throws Exception
    {
        checkoutTempProject();

        String file = FileUtils.fileRead( new File( workspaceFile, TMP_PROJECT + SRC ) );
        setContents( getWorkspaceProject( TMP_PROJECT ), file.substring( 5 ) );
        setContents( getWorkspaceProject( PROJECT ), file.substring( 0, file.length() - 5 ) );
        commit( getWorkspaceProject( TMP_PROJECT ) );

        CvsTeamProvider provider = new CvsTeamProvider();
        IWorkspaceSourceTree sourceTree =
            S2ProjectCore.getInstance().getWorkspaceCodebases().get( 0 ).getSourceTrees().get( 0 );
        assertEquals( "Expected updates available", TeamOperationResult.RESULT_CHANGED.getStatus(),
                      provider.getUpdateStatus( sourceTree, monitor ).getStatus() );

        assertEquals( "Codebase should be up to date", TeamOperationResult.RESULT_UPTODATE.getStatus(),
                      provider.updateFromRepository( sourceTree, monitor ).getStatus() );

        assertEquals( "Codebase should be up to date", TeamOperationResult.RESULT_UPTODATE.getStatus(),
                      provider.getUpdateStatus( sourceTree, monitor ).getStatus() );
    }

    public void testNoChanges()
        throws Exception
    {
        checkoutTempProject();

        CvsTeamProvider provider = new CvsTeamProvider();
        IWorkspaceSourceTree sourceTree =
            S2ProjectCore.getInstance().getWorkspaceCodebases().get( 0 ).getSourceTrees().get( 0 );
        assertEquals( "Expected up-to-date status", TeamOperationResult.RESULT_UPTODATE.getStatus(),
                      provider.getUpdateStatus( sourceTree, monitor ).getStatus() );
    }

    public void testOutgoingChange()
        throws Exception
    {
        checkoutTempProject();

        // some change
        setContents( getWorkspaceProject( PROJECT ), "2" );

        CvsTeamProvider provider = new CvsTeamProvider();
        IWorkspaceSourceTree sourceTree =
            S2ProjectCore.getInstance().getWorkspaceCodebases().get( 0 ).getSourceTrees().get( 0 );
        assertEquals( "Expected up-to-date status", TeamOperationResult.RESULT_UPTODATE.getStatus(),
                      provider.getUpdateStatus( sourceTree, monitor ).getStatus() );
    }

    private void shareProject( IProject project, String module )
        throws InterruptedException, InvocationTargetException
    {
        ShareProjectOperation op = new ShareProjectOperation( null, repository, project, module );
        op.run( monitor );
    }

    private void commit( IProject tmpProject )
        throws InvocationTargetException, InterruptedException, CoreException
    {
        Collection<IResource> resources = new ArrayList<IResource>();
        for ( IResource resource : tmpProject.getFolder( "src" ).members() )
        {
            resources.add( resource );
        }
        resources.add( tmpProject.getFolder( "META-INF" ).getFile( "MANIFEST.MF" ) );
        resources.add( tmpProject.getFile( "pom.xml" ) );
        if ( resources.size() == 0 )
        {
            return;
        }
        Collection<IResource> newResources = new ArrayList<IResource>();
        for ( IResource resource : resources )
        {
            if ( !isManaged( resource ) )
            {
                newResources.add( resource );
            }
        }
        try
        {
            AddAndCommitOperation op =
                new AddAndCommitOperation( null, resources.toArray( new IResource[resources.size()] ),
                                           newResources.toArray( new IResource[newResources.size()] ), "Test Commit" );
            op.run( monitor );
        }
        catch ( InvocationTargetException e )
        {
            if ( e.getCause() instanceof CVSException )
            {
                IStatus status = ( (CVSException) e.getCause() ).getStatus();
                if ( status.getSeverity() != IStatus.INFO )
                {
                    fail( status.getMessage() );
                }
            }
        }
    }

    private boolean isManaged( IResource resource )
        throws CVSException
    {
        final ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor( resource );
        if ( cvsResource.isFolder() )
        {
            return ( (ICVSFolder) cvsResource ).isCVSFolder();
        }
        return cvsResource.isManaged();
    }

    private void checkout( ICVSRemoteFolder folder, IProject project )
        throws InvocationTargetException, InterruptedException
    {
        CheckoutSingleProjectOperation op = new CheckoutSingleProjectOperation( null, folder, project, null, false );
        op.run( monitor );
    }

    private void setContents( IProject project, String contents )
        throws CoreException
    {
        IFile file = (IFile) project.findMember( "src/Main.java" );
        file.setContents( new ByteArrayInputStream( contents.getBytes() ), true, false, monitor );
    }

    @Override
    protected HttpServer newHttpServer()
    {
        HttpServer httpServer = super.newHttpServer();
        httpServer.setFilterToken( "@user.name@", System.getProperty( "user.name" ) );
        return httpServer;
    }

    protected void createRemote( IProject tmpProject )
        throws IOException, CoreException
    {

        FileUtils.copyDirectoryStructure( new File( "resources/scm/cvs/cvs-update-test/" ).getAbsoluteFile(),
                                          new File( workspace.getRoot().getLocation().toFile(), TMP_PROJECT ).getAbsoluteFile() );
        tmpProject.refreshLocal( IResource.DEPTH_INFINITE, monitor );
        tmpProject.open( monitor );
        CVSURI uri = CVSURI.fromUri( URI.create( SERVER + System.getProperty( "user.name" ) ) );
        repository = uri.getRepository();
        repository.setUsername( USER );
        repository.setPassword( PASS );
    }

    private void copyFiles( File source, File dest )
        throws IOException
    {
        String[] files = new String[] { "src/Main.java", ".classpath", ".project", "pom.xml", "META-INF/MANIFEST.MF" };

        for ( String f : files )
        {
            FileUtils.copyFile( new File( source, f ), new File( dest, f ) );
        }
    }

    public void testUpdateAfterPasswordChange()
        throws Exception
    {
        String cvsUrl = "cvs://:pserver:scmtest.grid.sonatype.com:/cvs";
        // Materialize a codebase and verify it
        addRealmAndURL( "testUpdateAfterPasswordChange", cvsUrl, "hudson", "hudson" );
        materializeProjects( httpServer.getHttpUrl() + "/catalogs/cvs", "Project-A" );
        assertMavenProject( "test", "cvs-test", "0.0.1-SNAPSHOT" );
        Util.waitForTeamSharingJobs();
        assertWorkspaceProjectShared( "Test" );

        // Try to update the materialized codebase - should succeed
        S2ProjectCore core = S2ProjectCore.getInstance();
        assertEquals( 1, core.getWorkspaceCodebases().size() );
        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 );
        CvsTeamProvider provider = new CvsTeamProvider();
        TeamOperationResult updateResult = provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UPTODATE, updateResult.getStatus() );
        updateResult = provider.updateFromRepository( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UPTODATE, updateResult.getStatus() );

        // Change the cvs credentials in the auth registry
        IAuthData authData = AuthFacade.getAuthService().select( cvsUrl );
        authData.setUsernameAndPassword( "hudson", "wrong pass" );
        AuthFacade.getAuthService().save( cvsUrl, authData );

        // Try to update the materialized codebase - should fail
        updateResult = provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UNAUTHORIZED, updateResult.getStatus() );
        updateResult = provider.updateFromRepository( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UNAUTHORIZED, updateResult.getStatus() );

        // Fix the cvs credentials in the auth registry
        authData = AuthFacade.getAuthService().select( cvsUrl );
        authData.setUsernameAndPassword( "hudson", "hudson" );
        AuthFacade.getAuthService().save( cvsUrl, authData );

        // Try to update the materialized codebase - should succeed
        updateResult = provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UPTODATE, updateResult.getStatus() );
        updateResult = provider.updateFromRepository( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UPTODATE, updateResult.getStatus() );
    }
}
