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
package com.sonatype.s2.project.integration.test.git;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.URIish;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;

import com.sonatype.m2e.egit.internal.EgitTeamProvider;
import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.internal.update.DetermineCodebaseUpdateStatusOperation;
import com.sonatype.s2.project.core.team.ITeamProvider;
import com.sonatype.s2.project.core.team.TeamOperationResult;
import com.sonatype.s2.project.core.test.HttpServer;

@SuppressWarnings( "restriction" )
public class EgitTeamProviderTest
    extends AbstractGitTest
{
    @Override
    protected void setUp()
        throws Exception
    {
        closeCachedGitRepositories();
        super.setUp();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            closeCachedGitRepositories();
            for ( Repository gitRepo : gitRepositoriesToClose )
            {
                gitRepo.close();
            }
            gitRepositoriesToClose.clear();
        }
        finally
        {
            super.tearDown();
        }
    }

    protected File clone( String srcPath, String dstPath )
        throws IOException, URISyntaxException, InvocationTargetException, InterruptedException
    {
        File repoOriginal = new File( srcPath ).getCanonicalFile();
        File repoDir = new File( dstPath ).getCanonicalFile();
        if ( repoDir.exists() )
        {
            FileUtils.deleteDirectory( repoDir );
        }
        URIish uri = new URIish( repoOriginal.getPath() );
        ListRemoteOperation ls = new ListRemoteOperation( new FileRepository( repoDir ), uri, 30 );
        ls.run( monitor );
        CloneOperation op =
            new CloneOperation( uri, true, null, repoDir, ls.getRemoteRef( "refs/heads/master" ), "origin", 30 );
        op.run( monitor );
        return repoDir;
    }

    private void closeCachedGitRepositories()
    {
        RepositoryCache repoCache = org.eclipse.egit.core.Activator.getDefault().getRepositoryCache();
        for ( Repository gitRepo : repoCache.getAllRepositories() )
        {
            gitRepo.close();
        }
        repoCache.clear();
    }

    private Repository newGitRepository( File repoDir )
        throws IOException
    {
        Repository repo = new FileRepository( new File( repoDir, ".git" ) );
        gitRepositoriesToClose.add( repo );
        return repo;
    }

    private List<Repository> gitRepositoriesToClose = new ArrayList<Repository>();

    public void testBasicStatus()
        throws Exception
    {
        EgitTeamProvider provider = new EgitTeamProvider();

        // create test "remote" repository
        File repoDir = clone( "resources/scm/git/simple", "target/git/simple" );

        // materialize
        materialize( "resources/projects/git-codebase-update.xml" );
        S2ProjectCore core = S2ProjectCore.getInstance();
        assertEquals( 1, core.getWorkspaceCodebases().size() );
        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 );

        assertEquals( IWorkspaceSourceTree.STATUS_UPTODATE,
                      provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );

        // introduce a change in the remote repository
        FileUtils.copyFile( new File( "resources/scm/git/simple-changes/pom-version-0.2.2.xml" ), //
                            new File( repoDir, "pom.xml" ) );
        Repository db = newGitRepository( repoDir );
        Git git = new Git( db );
        git.add().addFilepattern( "pom.xml" ).call();
        git.commit().setMessage( "commit 1" ).call();

        assertEquals( IWorkspaceSourceTree.STATUS_CHANGED,
                      provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );

        // introduce conflicting change to local repository
        IProject project = getWorkspaceProject( "git-test" );

        copyContent( project, new File( "resources/scm/git/simple-changes/pom-version-0.2.2.xml" ), "pom.xml" );
        RepositoryMapping repositoryMapping = RepositoryMapping.getMapping( project );
        db = repositoryMapping.getRepository();
        git = new Git( db );

        // staged local changes
        git.add().addFilepattern( "pom.xml" ).call();
        assertEquals( IWorkspaceSourceTree.STATUS_NOT_SUPPORTED,
                      provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );

        // local commits
        git.commit().setMessage( "commit 2" ).call();
        assertEquals( IWorkspaceSourceTree.STATUS_NOT_SUPPORTED,
                      provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor ).getStatus() );
    }

    public void testUpdate()
        throws Exception
    {
        EgitTeamProvider provider = new EgitTeamProvider();

        // create test "remote" repository
        File repoDir = clone( "resources/scm/git/simple", "target/git/simple" );

        // materialize
        materialize( "resources/projects/git-codebase-update.xml" );
        assertProject( "test", "git-test", "0.0.1-SNAPSHOT" );
        S2ProjectCore core = S2ProjectCore.getInstance();
        assertEquals( 1, core.getWorkspaceCodebases().size() );

        // introduce remote commit
        File modifiedPomFile = new File( "resources/scm/git/simple-changes/pom-version-0.2.2.xml" );

        FileUtils.copyFile( modifiedPomFile, new File( repoDir, "pom.xml" ) );
        Repository db = newGitRepository( repoDir );
        Git git = new Git( db );
        git.add().addFilepattern( "pom.xml" ).call();
        git.commit().setMessage( "commit 1" ).call();

        // update
        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        IWorkspaceSourceTree tree = core.getWorkspaceCodebases().get( 0 ).getPending().getSourceTrees().get( 0 );

        assertEquals( IWorkspaceSourceTree.STATUS_CHANGED, tree.getStatus() );

        assertEquals( IWorkspaceSourceTree.STATUS_UPTODATE, provider.updateFromRepository( tree, monitor ).getStatus() );

        IProject project = getWorkspaceProject( "git-test" );

        assertTrue( FileUtils.contentEquals( modifiedPomFile, project.getFile( "pom.xml" ).getLocation().toFile() ) );
        assertProject( "test", "git-test", "0.0.1-SNAPSHOT" );
    }

    public void testMergeConflicts()
        throws Exception
    {
        EgitTeamProvider provider = new EgitTeamProvider();

        // create test "remote" repository
        File repoDir = clone( "resources/scm/git/simple", "target/git/simple" );

        // materialize
        materialize( "resources/projects/git-codebase-update.xml" );
        S2ProjectCore core = S2ProjectCore.getInstance();
        assertEquals( 1, core.getWorkspaceCodebases().size() );

        // introduce remote commit
        File modifiedPomFile = new File( "resources/scm/git/simple-changes/pom-version-0.2.2.xml" );

        FileUtils.copyFile( modifiedPomFile, new File( repoDir, "pom.xml" ) );
        Repository db = newGitRepository( repoDir );
        Git git = new Git( db );
        git.add().addFilepattern( "pom.xml" ).call();
        git.commit().setMessage( "commit 1" ).call();

        // introduce local conflicting change
        IProject project = getWorkspaceProject( "git-test" );
        copyContent( project, new File( "resources/scm/git/simple-changes/pom-version-0.3.3.xml" ), "pom.xml" );

        // update
        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        IWorkspaceSourceTree tree = core.getWorkspaceCodebases().get( 0 ).getPending().getSourceTrees().get( 0 );

        assertEquals( IWorkspaceSourceTree.STATUS_CHANGED, tree.getStatus() );

        // git does not allow merge at all when local and remote changes conflict. so it looks like the only way
        // to get merge conflicts with git when merging/rebasing local commits, which we do not currently support

        assertEquals( IWorkspaceSourceTree.STATUS_NOT_SUPPORTED, provider.updateFromRepository( tree, monitor ).getStatus() );
    }

    public void testSourceTreeRoot()
        throws Exception
    {
        // create test "remote" repository
        File repoDir = clone( "resources/scm/git/roots", "target/git/roots" );
        assertTrue( repoDir.exists() );

        // materialize
        materialize( "resources/projects/git-codebase-with-roots-update.xml" );
        S2ProjectCore core = S2ProjectCore.getInstance();
        assertEquals( 1, core.getWorkspaceCodebases().size() );

        // get status
        new DetermineCodebaseUpdateStatusOperation( core.getWorkspaceCodebases().get( 0 ) ).run( monitor );

        IWorkspaceSourceTree tree = core.getWorkspaceCodebases().get( 0 ).getPending().getSourceTrees().get( 0 );

        assertEquals( IWorkspaceSourceTree.STATUS_UPTODATE, tree.getStatus() );
    }

    public void testUpdateAfterPasswordChange()
        throws Exception
    {
        startSshServer();
        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "testuser", "testpass" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( sshServer.getUrl(), "test", AnonymousAccessType.NOT_ALLOWED,
                                                         monitor );

        // Materialize a codebase and verify it
        materializeProject( httpServer.getHttpUrl() + "/catalogs/git-over-ssh", "Project-A" );

        assertTrue( knownHosts.isFile() );
        assertProject( "test", "git-test", "0.0.1-SNAPSHOT" );

        // Try to update the materialized codebase - should succeed
        S2ProjectCore core = S2ProjectCore.getInstance();
        assertEquals( 1, core.getWorkspaceCodebases().size() );
        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 );
        ITeamProvider provider = getTeamProvider();
        TeamOperationResult updateResult = provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UPTODATE, updateResult.getStatus() );
        updateResult = provider.updateFromRepository( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UPTODATE, updateResult.getStatus() );
        assertProject( "test", "git-test", "0.0.1-SNAPSHOT" );

        // Change the svn credentials in the auth registry
        IAuthData authData = AuthFacade.getAuthService().select( sshServer.getUrl() );
        authData.setUsernameAndPassword( "testuser", "wrong pass" );
        AuthFacade.getAuthService().save( sshServer.getUrl(), authData );

        // Try to update the materialized codebase - should fail
        updateResult = provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UNAUTHORIZED, updateResult.getStatus() );
        // The update operation does not access the remote repository, so we should not get STATUS_UNAUTHORIZED, but
        // STATUS_UPTODATE
        updateResult = provider.updateFromRepository( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UPTODATE, updateResult.getStatus() );

        // Change the password on the svn server
        sshServer.addUser( "testuser", "newtestpass" );

        // Fix the svn credentials in the auth registry
        authData = AuthFacade.getAuthService().select( sshServer.getUrl() );
        authData.setUsernameAndPassword( "testuser", "newtestpass" );
        AuthFacade.getAuthService().save( sshServer.getUrl(), authData );

        // Try to update the materialized codebase - should succeed
        updateResult = provider.getUpdateStatus( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UPTODATE, updateResult.getStatus() );
        updateResult = provider.updateFromRepository( codebase.getSourceTrees().get( 0 ), monitor );
        assertEquals( updateResult.toString(), IWorkspaceSourceTree.STATUS_UPTODATE, updateResult.getStatus() );
        assertProject( "test", "git-test", "0.0.1-SNAPSHOT" );
    }

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
                    if ( element.getAttribute( "type" ).equals( "git" ) )
                    {
                        return (ITeamProvider) element.createExecutableExtension( "class" );
                    }
                }
            }
        }
        fail( "Failed to obtain GIT TeamProvider" );
        return null;
    }
}
