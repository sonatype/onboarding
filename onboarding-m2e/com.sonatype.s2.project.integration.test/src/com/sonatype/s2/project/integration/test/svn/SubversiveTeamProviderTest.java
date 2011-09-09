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
package com.sonatype.s2.project.integration.test.svn;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.team.svn.core.connector.ISVNConnector.Depth;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.file.AddToSVNOperation;
import org.eclipse.team.svn.core.operation.file.CheckoutAsOperation;
import org.eclipse.team.svn.core.operation.file.CommitOperation;
import org.eclipse.team.svn.core.operation.file.SVNFileStorage;
import org.eclipse.team.svn.core.operation.local.RemoteStatusOperation;
import org.eclipse.team.svn.core.operation.remote.management.AddRepositoryLocationOperation;
import org.eclipse.team.svn.core.operation.remote.management.SaveRepositoryLocationsOperation;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.AbstractSVNStorage;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.ILoggedOperationFactory;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;

import com.sonatype.m2e.subversive.SubversiveHelper;
import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.team.ITeamProvider;
import com.sonatype.s2.project.core.team.TeamOperationResult;
import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.tests.common.SvnServer;
import com.sonatype.s2.project.tests.common.Util;

@SuppressWarnings( "restriction" )
public class SubversiveTeamProviderTest
    extends AbstractSvnTest
{
    private File tempProject;

    private File svndir;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        svndir = new File( "target/svn/simple" ).getAbsoluteFile();
        FileUtils.deleteDirectory( svndir );
        svndir.mkdirs();
        InputStream dumpStream = new FileInputStream( new File( "resources/scm/svn/simple.dump" ).getAbsoluteFile() );
        SvnServer.createSvnRepository( svndir );
        SvnServer.loadSvnRepository( dumpStream, svndir );
    }

    @Override
    public void tearDown()
        throws Exception
    {
        try
        {
            if ( tempProject != null )
            {
                FileUtils.deleteDirectory( tempProject );
                tempProject = null;
            }
        }
        finally
        {
            super.tearDown();
        }
    }

    /*
     * Test the responses when there are incoming changes
     */
    public void testIncomingChange()
        throws Exception
    {
        // materialize
        materialize( "resources/projects/svn-codebase-update.xml" );

        // Create incoming change
        createTempProject();
        writeToFile( "public class Main{\nlong v = 2;\n}", new File( tempProject, "src/main/java/Main.java" ) );
        commitTempProject();

        S2ProjectCore core = S2ProjectCore.getInstance();
        assertEquals( 1, core.getWorkspaceCodebases().size() );
        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 );
        ITeamProvider provider = getTeamProvider();

        assertEquals( "Incoming changes expected", IWorkspaceSourceTree.STATUS_CHANGED,
                      provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );
        assertEquals( "Project should be up to date", IWorkspaceSourceTree.STATUS_UPTODATE,
                      provider.updateFromRepository( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );
        assertEquals( "Up-to-date expected", IWorkspaceSourceTree.STATUS_UPTODATE,
                      provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );
    }

    /*
     * Test the responses for a new incoming file
     */
    public void testNewIncomingFile()
        throws Exception
    {
        // materialize
        materialize( "resources/projects/svn-codebase-update.xml" );

        // Create new remote file
        createTempProject();
        // modify files
        File newFile = new File( tempProject, "src/main/java/NewFile.java" );
        newFile.createNewFile();
        writeToFile( "public class NewFile{\nlong v = 2;\n}", newFile );
        SubversiveHelper.installNonInteractiveOptionProvider();
        try
        {
            AddToSVNOperation addOp = new AddToSVNOperation( new File[] { newFile }, true );
            addOp.run( monitor );
        }
        finally
        {
            SubversiveHelper.restoreOptionProvider();
        }
        // commit
        commitTempProject();

        S2ProjectCore core = S2ProjectCore.getInstance();
        assertEquals( 1, core.getWorkspaceCodebases().size() );
        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 );
        ITeamProvider provider = getTeamProvider();

        assertEquals( "Update should be available", IWorkspaceSourceTree.STATUS_CHANGED,
                      provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );
        assertEquals( "Project should be up to date", IWorkspaceSourceTree.STATUS_UPTODATE,
                      provider.updateFromRepository( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );
        assertEquals( "Incoming changes expected", IWorkspaceSourceTree.STATUS_UPTODATE,
                      provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );
    }

    /*
     * Test the responses when there are local changes, but no remote changes
     */
    public void testOutgoingChange()
        throws Exception
    {
        // materialize
        materialize( "resources/projects/svn-codebase-update.xml" );

        S2ProjectCore core = S2ProjectCore.getInstance();
        assertEquals( 1, core.getWorkspaceCodebases().size() );
        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 );
        writeToFile( "public class Main{\nlong v = 2;\n}", new File( codebase.getSourceTrees().get( 0 ).getLocation(),
                                                                     "src/main/java/Main.java" ) );

        ITeamProvider provider = getTeamProvider();

        assertEquals( "Project should be up to date", IWorkspaceSourceTree.STATUS_UPTODATE,
                      provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );
        assertEquals( "Project should be up to date", IWorkspaceSourceTree.STATUS_UPTODATE,
                      provider.updateFromRepository( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );
    }

    /*
     * Test the response when there are no local or incoming changes
     */
    public void testNoChange()
        throws Exception
    {
        // materialize
        materialize( "resources/projects/svn-codebase-update.xml" );

        S2ProjectCore core = S2ProjectCore.getInstance();
        assertEquals( 1, core.getWorkspaceCodebases().size() );
        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 );

        ITeamProvider provider = getTeamProvider();
        assertEquals( "Project should be up to date", IWorkspaceSourceTree.STATUS_UPTODATE,
                      provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );
    }

    /*
     * Test the response when there is a conflicting change
     */
    public void testConflict()
        throws Exception
    {
        // materialize
        materialize( "resources/projects/svn-codebase-update.xml" );

        // Update repository from temporary location
        createTempProject();
        writeToFile( "public class Main{\nlong v = 2;\n}", new File( tempProject, "src/main/java/Main.java" ) );
        commitTempProject();

        S2ProjectCore core = S2ProjectCore.getInstance();
        assertEquals( 1, core.getWorkspaceCodebases().size() );
        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 );

        writeToFile( "public class Main{\nlong v = 3;\n}", new File( codebase.getSourceTrees().get( 0 ).getLocation(),
                                                                     "src/main/java/Main.java" ) );

        ITeamProvider provider = getTeamProvider();
        assertEquals( "Merge conflicts expected", IWorkspaceSourceTree.STATUS_NOT_SUPPORTED,
                      provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );
        assertEquals( "Merge conflicts expected", IWorkspaceSourceTree.STATUS_NOT_SUPPORTED,
                      provider.updateFromRepository( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );
        assertEquals( "Incoming changes expected", IWorkspaceSourceTree.STATUS_NOT_SUPPORTED,
                      provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );
    }

    public void testUpdateAfterPasswordChange()
        throws Exception
    {
        startSvnServer( "simple.dump", "conf-b" );

        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "testuser", "testpass" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( svnServer.getUrl(), "test", AnonymousAccessType.NOT_ALLOWED,
                                                         monitor );

        // Materialize a codebase and verify it
        materializeProjects( httpServer.getHttpUrl() + "/catalogs/svn-over-svn", "Project-A" );

        assertMavenProject( "test", "svn-test", "0.0.1-SNAPSHOT" );
        Util.waitForTeamSharingJobs();
        assertWorkspaceProjectShared( "svn-test" );
        assertAuthSet( getWorkspaceProject( "svn-test" ) );
        IAuthData authData = AuthFacade.getAuthService().select( svnServer.getUrl() );
        assertSvnLocationsAuth( SVNRemoteStorage.instance(), authData );

        RemoteStatusOperation op = new RemoteStatusOperation( new IResource[] { getWorkspaceProject( "svn-test" ) } );
        op.run( monitor );
        assertTrue( op.getStatus().toString(), op.getStatus().isOK() );

        // Try to update the materialized codebase - should succeed
        S2ProjectCore core = S2ProjectCore.getInstance();
        assertEquals( 1, core.getWorkspaceCodebases().size() );
        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 );
        ITeamProvider provider = getTeamProvider();
        TeamOperationResult updateResult = provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UPTODATE, updateResult.getStatus() );
        updateResult = provider.updateFromRepository( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UPTODATE, updateResult.getStatus() );

        // Change the svn credentials in the auth registry
        authData = AuthFacade.getAuthService().select( svnServer.getUrl() );
        authData.setUsernameAndPassword( "testuser", "wrong pass" );
        AuthFacade.getAuthService().save( svnServer.getUrl(), authData );

        // Try to update the materialized codebase - should fail
        updateResult = provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UNAUTHORIZED, updateResult.getStatus() );
        updateResult = provider.updateFromRepository( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UNAUTHORIZED, updateResult.getStatus() );
        assertSvnLocationsAuth( SVNFileStorage.instance(), authData );
        assertSvnLocationsAuth( SVNRemoteStorage.instance(), authData );

        // Change the password on the svn server
        svnServer.setPort( svnServer.getPort() );
        svnServer.stop();
        svnServer.setConfDir( SCM_SVN_PATH + "conf-c" );
        svnServer.start();

        // Fix the svn credentials in the auth registry
        authData = AuthFacade.getAuthService().select( svnServer.getUrl() );
        authData.setUsernameAndPassword( "testuser", "newtestpass" );
        AuthFacade.getAuthService().save( svnServer.getUrl(), authData );

        // Try to update the materialized codebase - should succeed
        updateResult = provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UPTODATE, updateResult.getStatus() );
        updateResult = provider.updateFromRepository( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UPTODATE, updateResult.getStatus() );
        assertSvnLocationsAuth( SVNFileStorage.instance(), authData );
        assertSvnLocationsAuth( SVNRemoteStorage.instance(), authData );

        resetSvnLocationsAuth( SVNFileStorage.instance() );
        resetSvnLocationsAuth( SVNRemoteStorage.instance() );

        // Try to update the materialized codebase - should succeed
        updateResult = provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UPTODATE, updateResult.getStatus() );
        updateResult = provider.updateFromRepository( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UPTODATE, updateResult.getStatus() );

        assertSvnLocationsAuth( SVNFileStorage.instance(), authData );
        assertSvnLocationsAuth( SVNRemoteStorage.instance(), authData );
    }

    private void assertSvnLocationsAuth( AbstractSVNStorage svnStorage, IAuthData authData )
        throws Exception
    {
        IRepositoryLocation[] locations = svnStorage.getRepositoryLocations();
        assertNotNull( locations );
        assertTrue( locations.length > 0 );
        for ( IRepositoryLocation location : locations )
        {
            assertEquals( "Bad username for svn location " + location.getUrl(), authData.getUsername(),
                          location.getUsername() );
            assertEquals( "Bad password for svn location " + location.getUrl(), authData.getPassword(),
                          location.getPassword() );
        }
    }

    private void resetSvnLocationsAuth( AbstractSVNStorage svnStorage )
        throws Exception
    {
        IRepositoryLocation[] locations = svnStorage.getRepositoryLocations();
        assertNotNull( locations );
        assertTrue( locations.length > 0 );
        for ( IRepositoryLocation location : locations )
        {
            location.setUsername( null );
            location.setPassword( null );
            location.setPasswordSaved( true );
        }
    }

    private void createTempProject()
        throws Exception
    {
        tempProject = File.createTempFile( "svn", "tmp" );
        tempProject.delete();
        tempProject.mkdirs();
        tempProject.deleteOnExit();
        String uri = new File( svndir, "simple/trunk" ).toURI().toString();
        uri = uri.substring( 0, 6 ) + "//" + uri.substring( 6 );
        // Temp location
        checkoutProject( tempProject, uri, "hudson", "hudson" );
        Util.waitForTeamSharingJobs();
    }

    private void commitTempProject()
    {
        SubversiveHelper.installNonInteractiveOptionProvider();
        try
        {
            CommitOperation op = new CommitOperation( new File[] { tempProject }, "Commit", true, false );
            op.run( new NullProgressMonitor() );
            assertTrue( "Commit failed: " + op.getStatus().getMessage(), op.getStatus().isOK() );
        }
        finally
        {
            SubversiveHelper.restoreOptionProvider();
        }
    }

    /*
     * Write content to the file
     */
    protected void writeToFile( String content, File file )
        throws CoreException, IOException
    {
        FileOutputStream destination = new FileOutputStream( file );
        try
        {
            FileUtil.transferStreams( new ByteArrayInputStream( content.getBytes() ), destination, file.toString(),
                                      new NullProgressMonitor() );
        }
        finally
        {
            IOUtil.close( destination );
        }

    }

    /*
     * Checkout from the remote URL to the file location
     */
    private void checkoutProject( File location, String url, String username, String password )
        throws Exception
    {
        SubversiveHelper.installNonInteractiveOptionProvider();
        try
        {
            IRepositoryContainer container = getRepositoryResource( url, username, password );
            CheckoutAsOperation checkout = new CheckoutAsOperation( location, container, Depth.INFINITY, false, true );

            AddRepositoryLocationOperation add = new AddRepositoryLocationOperation( container.getRepositoryLocation() );

            SaveRepositoryLocationsOperation save = new SaveRepositoryLocationsOperation();

            CompositeOperation op = new CompositeOperation( checkout.getOperationName(), checkout.getMessagesClass() );
            op.add( checkout );
            op.add( add, new IActionOperation[] { checkout } );
            op.add( save, new IActionOperation[] { add } );

            ProgressMonitorUtility.doTaskExternal( op, monitor, ILoggedOperationFactory.EMPTY );

            assertTrue( "Checkout failed: " + op.getStatus().getMessage(), op.getStatus().isOK() );
        }
        finally
        {
            SubversiveHelper.restoreOptionProvider();
        }
    }

    protected IRepositoryContainer getRepositoryResource( String url, String username, String password )
        throws MalformedURLException
    {
        IRepositoryContainer container = (IRepositoryContainer) SVNUtility.asRepositoryResource( url, true );
        container.setSelectedRevision( SVNRevision.HEAD );
        IRepositoryLocation repositoryLocation = container.getRepositoryLocation();

        fixRepositoryUrl( repositoryLocation );

        if ( username != null )
            repositoryLocation.setUsername( username );
        if ( password != null )
            repositoryLocation.setPassword( password );

        return container;
    }

    private void fixRepositoryUrl( IRepositoryLocation location )
    {
        // workaround for MECLIPSE-637, Subversive collapses all double slashes, thereby screwing the scheme part of the
        // URL
        String url = location.getUrlAsIs();
        int idx = url.indexOf( ":/" );
        if ( idx > 0 && !url.substring( idx ).startsWith( "://" ) )
        {
            url = url.substring( 0, idx ) + "://" + url.substring( idx + 2 );
            location.setUrl( url );
        }
    }

    /*
     * Get the SVN ITeamProvider
     */
    protected static ITeamProvider getTeamProvider()
        throws CoreException
    {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint extensionPoint = registry.getExtensionPoint( "com.sonatype.s2.project.core.teamProviders" );
        if ( extensionPoint != null )
        {
            for ( IExtension extension : extensionPoint.getExtensions() )
            {
                for ( IConfigurationElement element : extension.getConfigurationElements() )
                {
                    if ( element.getAttribute( "type" ).equals( "svn" ) )
                    {
                        return (ITeamProvider) element.createExecutableExtension( "class" );
                    }
                }
            }
        }
        fail( "Failed to obtain SVN TeamProvider" );
        return null;
    }
}
