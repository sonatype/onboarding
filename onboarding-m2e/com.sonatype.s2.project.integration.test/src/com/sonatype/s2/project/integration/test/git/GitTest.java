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

import org.eclipse.core.runtime.IStatus;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;

import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.tests.common.SshServer;

/**
 * Note: The SSH related tests in here require an external Git installation (1.7.0.2+) to be present in your
 * {@code PATH}.
 */
public class GitTest
    extends AbstractGitTest
{
    /**
     * Tests project materialization from a GIT repository via file: protocol.
     */
    public void testGitOverFile()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        materializeProject( httpServer.getHttpUrl() + "/catalogs/git-over-file", "Project-A" );

        assertProject( "test", "git-test", "0.0.1-SNAPSHOT" );
    }

    /**
     * Tests project materialization from a GIT repository via file: protocol when the SCM location is associated to a
     * security realm with credentials. In this case, the resulting file: URL used for the clone operation must not
     * carry any username/password info.
     */
    public void testGitOverFileWithCredentialsForAssociatedSecurityRealm()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "testuser", "testpass" );

        materializeProject( httpServer.getHttpUrl() + "/catalogs/git-over-file", "Project-A" );

        assertProject( "test", "git-test", "0.0.1-SNAPSHOT" );
    }

    /**
     * Tests project materialization from a GIT repository via http: protocol.
     */
    public void testGitOverHttp()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        materializeProject( httpServer.getHttpUrl() + "/catalogs/git-over-http", "Project-A" );

        assertProject( "test", "git-test", "0.0.1-SNAPSHOT" );
    }

    /**
     * Tests project materialization from a GIT repository via https: protocol.
     */
    public void testGitOverHttps()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        materializeProject( httpServer.getHttpsUrl() + "/catalogs/git-over-http", "Project-A" );

        assertProject( "test", "git-test", "0.0.1-SNAPSHOT" );
    }

    /**
     * Tests project materialization from a GIT repository via ssh: protocol.
     */
    public void testGitOverSshUsingRegularUrl()
        throws Exception
    {
        startSshServer();
        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "testuser", "testpass" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( sshServer.getUrl(), "test", AnonymousAccessType.NOT_ALLOWED,
                                                         monitor );

        materializeProject( httpServer.getHttpUrl() + "/catalogs/git-over-ssh", "Project-A" );

        assertTrue( knownHosts.isFile() );
        assertProject( "test", "git-test", "0.0.1-SNAPSHOT" );
    }

    /**
     * Tests project materialization from a GIT repository via SCP style URL.
     */
    public void testGitOverSshUsingScpStyleUrl()
        throws Exception
    {
        sshServer = new SshServer();
        try
        {
            sshServer.setPort( 22 ).start();
            sshServer.addUser( "testuser", "testpass" );
        }
        catch ( Exception e )
        {
            System.out.println( "WARNING: Port 22 already in use, skipping " + getName() );
            return;
        }
        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "testuser", "testpass" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( sshServer.getUrl().replace( ":22", "" ), "test",
                                                         AnonymousAccessType.NOT_ALLOWED, monitor );

        materializeProject( httpServer.getHttpUrl() + "/catalogs/git-over-ssh", "Project-B" );

        assertTrue( knownHosts.isFile() );
        assertProject( "test", "git-test", "0.0.1-SNAPSHOT" );
    }

    /**
     * Tests project materialization from a GIT repository via SSH when the URL includes the username. The username in
     * the URL should take precedence over anything the user might have erroneously entered interactively.
     */
    public void testGitOverSshWithUsernameInUrl()
        throws Exception
    {
        SshServer sshServer = startSshServer();
        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", "scm:git:ssh://anon@localhost:" + sshServer.getPort() + "/" + getBasedir()
            + "/resources/scm/git/simple", "wronguser", "wrongpass" );

        materializeProject( httpServer.getHttpUrl() + "/catalogs/git-over-ssh", "Project-C" );

        assertTrue( knownHosts.isFile() );
        assertProject( "test", "git-test", "0.0.1-SNAPSHOT" );
    }

    /**
     * Tests project materialization from a GIT repository after a previous failure. In particular, the reattempt must
     * not fail due to any remains of the failed attempt.
     */
    public void testGitReattemptedMaterializationAfterPreviousFailure()
        throws Exception
    {
        startSshServer();
        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "testuser", "wrongpass" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( sshServer.getUrl(), "test", AnonymousAccessType.NOT_ALLOWED,
                                                         monitor );

        try
        {
            materializeProject( httpServer.getHttpUrl() + "/catalogs/git-over-ssh", "Project-A" );
            fail( "Invalid password did not fail checkout" );
        }
        catch ( Exception e )
        {
            AuthFacade.getAuthService().save( httpServer.getHttpUrl(), "testuser", "testpass" );
            materializeProject( httpServer.getHttpUrl() + "/catalogs/git-over-ssh", "Project-A" );
        }

        assertProject( "test", "git-test", "0.0.1-SNAPSHOT" );
    }

    /**
     * Tests project materialization from a GIT repository when a specific branch has to be checked out.
     */
    public void testGitCheckoutOfSpecificBranch()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        materializeProject( httpServer.getHttpUrl() + "/catalogs/git-over-file", "Project-B" );

        assertProject( "test", "git-branch-a", "0.0.1-SNAPSHOT" );
    }

    public void testGitCheckoutOfSpecificTag()
        throws Exception
    {
        materialize( "resources/projects/git-from-refs-tags.xml" );

        assertProject( "test", "git-tag-b", "0.0.1-SNAPSHOT" );
    }

    /**
     * Tests that SCM validation with proper credentials succeeds.
     */
    public void testValidateGoodCredentials()
        throws Exception
    {
        for ( int i = 0; i < 10; i++ )
        {
            startSshServer();
            HttpServer httpServer = startHttpServer();

            if ( i == 0 )
            {
                addRealmAndURL( "test", httpServer.getHttpUrl(), "testuser", "testpass" );
                AuthFacade.getAuthRegistry().addURLToRealmAssoc( sshServer.getUrl(), "test",
                                                                 AnonymousAccessType.NOT_ALLOWED, monitor );
            }

            IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/git-credentials.xml" );
            /*
             * MECLIPSE-551: For unknown reasons, this test occasionally suffers from
             * "TransportException: Read timed out" because the network communication dies. So we retry a couple of
             * times.
             */
            if ( !status.isOK() && status.toString().contains( "timed out" ) )
            {
                System.err.println( "WARNING: Communication with SSH server timed out, retrying test" );
                sshServer.stop();
                httpServer.stop();
                continue;
            }
            assertTrue( status.toString(), status.isOK() );
            break;
        }
    }

    /**
     * Tests that SCM validation with wrong credentials fails.
     */
    public void testValidateBadCredentials()
        throws Exception
    {
        startSshServer();
        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "testuser", "wrongpass" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( sshServer.getUrl(), "test", AnonymousAccessType.NOT_ALLOWED,
                                                         monitor );

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/git-credentials.xml" );
        assertTrue( status.toString(), isUnauthorizedStatus( status ) );
    }

    public void testValidateAndMaterializeGitOverFileWithoutProtocol()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        validateScmAccessAndMaterialize( httpServer.getHttpUrl() + "/projects/git-over-file-without-protocol.xml" );

        assertProject( "test", "git-test", "0.0.1-SNAPSHOT" );
    }

    public void testValidateAndMaterializeGitOverFileWithProtocol()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        validateScmAccessAndMaterialize( httpServer.getHttpUrl() + "/projects/git-over-file-with-protocol.xml" );

        assertProject( "test", "git-test", "0.0.1-SNAPSHOT" );
    }

    /**
     * Tests that SCM validation with a bad protocol fails.
     */
    public void testValidateBadProtocol()
        throws Exception
    {
        IStatus status = validateScmAccess( getBaseUri() + "resources/projects/git-bad-protocol.xml" );
        assertFalse( status.isOK() );
    }

    /**
     * Tests that SCM validation with a bad host fails.
     */
    public void testValidateBadHost()
        throws Exception
    {
        IStatus status = validateScmAccess( getBaseUri() + "resources/projects/git-bad-host.xml" );
        assertFalse( status.isOK() );
    }

    /**
     * Tests that SCM validation with a bad repository fails.
     */
    public void testValidateBadRepo()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/git-bad-repo.xml" );
        assertFalse( status.isOK() );
    }
}
