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

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.svn.core.operation.local.RemoteStatusOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.AuthenticationType;

import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.integration.test.ConsoleProgressMonitor;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.tests.common.Util;

public class SvnTest
    extends AbstractSvnTest
{
    /**
     * Tests project materialization from a SVN repository via SVN protocol.
     */
    public void testSvnOverSvn()
        throws Exception
    {
        startSvnServer( "simple.dump", "conf-a" );

        HttpServer httpServer = startHttpServer();

        materializeProjects( httpServer.getHttpUrl() + "/catalogs/svn-over-svn", "Project-A" );

        assertMavenProject( "test", "svn-test", "0.0.1-SNAPSHOT" );
        Util.waitForTeamSharingJobs();
        assertWorkspaceProjectShared( "svn-test" );

        IRepositoryLocation location =
            SVNRemoteStorage.instance().getRepositoryLocation( getWorkspaceProject( "svn-test" ) );
        assertEquals( svnServer.getUrl() + "/simple", location.getUrl() );
    }

    /**
     * Tests project materialization from a SVN repository via SVN protocol and authentication. Besides the mere
     * materialization, this test also checks that the Subversion team provider has been enabled for the project and
     * persisted the credentials for the repository.
     */
    public void testSvnOverSvnWithAuthentication()
        throws Exception
    {
        startSvnServer( "simple.dump", "conf-b" );

        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "testuser", "testpass" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( svnServer.getUrl(), "test",
                                                         AnonymousAccessType.NOT_ALLOWED, monitor );

        materializeProjects( httpServer.getHttpUrl() + "/catalogs/svn-over-svn", "Project-A" );

        assertMavenProject( "test", "svn-test", "0.0.1-SNAPSHOT" );
        Util.waitForTeamSharingJobs();
        assertWorkspaceProjectShared( "svn-test" );
        assertAuthSet( getWorkspaceProject( "svn-test" ) );

        RemoteStatusOperation op = new RemoteStatusOperation( new IResource[] { getWorkspaceProject( "svn-test" ) } );
        op.run( monitor );
        assertTrue( op.getStatus().toString(), op.getStatus().isOK() );
    }

    /**
     * Tests project materialization from a SVN repository via SVN protocol and authentication when the password is
     * empty.
     */
    public void testSvnOverSvnWithAuthenticationUsingEmptyPassword()
        throws Exception
    {
        startSvnServer( "simple.dump", "conf-b" );

        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "guest", "" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( svnServer.getUrl(), "test", AnonymousAccessType.NOT_ALLOWED,
                                                         monitor );

        materializeProjects( httpServer.getHttpUrl() + "/catalogs/svn-over-svn", "Project-A" );

        assertMavenProject( "test", "svn-test", "0.0.1-SNAPSHOT" );
        Util.waitForTeamSharingJobs();
        assertWorkspaceProjectShared( "svn-test" );
        assertAuthSet( getWorkspaceProject( "svn-test" ) );
    }

    /**
     * Tests project materialization from a SVN repository when the URL includes the username (anonymous access).
     */
    public void testSvnOverSvnWithUsernameInUrl()
        throws Exception
    {
        startSvnServer( "simple.dump", "conf-b" );

        HttpServer httpServer = startHttpServer();

        materializeProjects( httpServer.getHttpUrl() + "/catalogs/svn-over-svn", "Project-B" );

        assertMavenProject( "test", "svn-test", "0.0.1-SNAPSHOT" );
        Util.waitForTeamSharingJobs();
        assertWorkspaceProjectShared( "svn-test" );
    }

    /**
     * Tests project materialization from a SVN repository after a previous failure. In particular, the reattempt must
     * not fail due to any remains of the failed attempt.
     */
    public void testSvnReattemptedMaterializationAfterPreviousFailure()
        throws Exception
    {
        startSvnServer( "simple.dump", "conf-b" );

        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "testuser", "wrongpass" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( svnServer.getUrl(), "test", AnonymousAccessType.NOT_ALLOWED,
                                                         monitor );

        try
        {
            materializeProjects( httpServer.getHttpUrl() + "/catalogs/svn-over-svn", "Project-A" );
            fail( "Invalid password did not fail checkout" );
        }
        catch ( Exception e )
        {
            AuthFacade.getAuthService().save( httpServer.getHttpUrl(), "testuser", "testpass" );
            materializeProjects( httpServer.getHttpUrl() + "/catalogs/svn-over-svn", "Project-A" );
        }

        assertMavenProject( "test", "svn-test", "0.0.1-SNAPSHOT" );
        Util.waitForTeamSharingJobs();
        assertWorkspaceProjectShared( "svn-test" );

        RemoteStatusOperation op = new RemoteStatusOperation( new IResource[] { getWorkspaceProject( "svn-test" ) } );
        op.run( monitor );
        assertTrue( op.getStatus().toString(), op.getStatus().isOK() );
    }

    /**
     * Tests that SCM validation with proper credentials succeeds.
     */
    public void testValidateGoodCredentials_SvnOverSvn()
        throws Exception
    {
        startSvnServer( "simple.dump", "conf-b" );

        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "testuser", "testpass" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( svnServer.getUrl(), "test", AnonymousAccessType.NOT_ALLOWED,
                                                         monitor );

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/svn-over-svn-credentials.xml" );
        assertTrue( status.toString(), status.isOK() );
    }

    /**
     * Tests that SCM validation with empty password succeeds.
     */
    public void testValidateAnonymousAccessViaEmptyPassword()
        throws Exception
    {
        startSvnServer( "simple.dump", "conf-b" );

        HttpServer httpServer = startHttpServer();

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/svn-anon-access.xml" );
        assertTrue( status.toString(), status.isOK() );
    }

    /**
     * Tests that SCM validation with wrong credentials fails.
     */
    public void testValidateBadCredentials_SvnOverSvn()
        throws Exception
    {
        startSvnServer( "simple.dump", "conf-b" );

        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "testuser", "wrongpass" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( svnServer.getUrl(), "test", AnonymousAccessType.NOT_ALLOWED,
                                                         monitor );

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/svn-over-svn-credentials.xml" );
        assertTrue( status.toString(), isUnauthorizedStatus( status ) );
    }

    /**
     * Tests that SCM validation with a bad protocol fails.
     */
    public void testValidateBadProtocol()
        throws Exception
    {
        IStatus status = validateScmAccess( getBaseUri() + "resources/projects/svn-bad-protocol.xml" );
        assertErrorStatus( status, "svn: URL protocol is not supported" );
    }

    /**
     * Tests that SCM validation with a bad host fails.
     */
    public void testValidateBadHost()
        throws Exception
    {
        String url = getBaseUri() + "resources/projects/svn-bad-host.xml";
        IStatus status = validateScmAccess( url );
        assertErrorStatus( status, "svn: Unknown host badhost.void.void" );
    }

    /**
     * Tests that SCM validation with a bad repository fails.
     */
    public void testValidateBadRepo()
        throws Exception
    {
        startSvnServer( "simple.dump", "conf-a" );

        HttpServer httpServer = startHttpServer();

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/svn-bad-repo.xml" );
        assertErrorStatus( status, "Path does not exist" );
    }

    public void testClientCertificateValidate()
        throws Exception
    {
        addRealmAndURL( "ssltest", getBaseUri(), new File( "resources/svn-with-client-certificate.p12" ),
                        "X9v.kNGbOgp5OnR?" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( "https://svntest.sonatype.org:444", "ssltest",
                                                         AnonymousAccessType.NOT_ALLOWED, monitor );
        IStatus status = validateScmAccess( getBaseUri() + "resources/projects/svn-with-client-certificate.xml" );
        assertTrue( status.toString(), status.isOK() );
    }

    public void testClientCertificateBadPassphrase()
        throws Exception
    {
        addRealmAndURL( "ssltest", getBaseUri(), new File( "resources/svn-with-client-certificate.p12" ), "boo!" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( "https://svntest.sonatype.org:444", "ssltest",
                                                         AnonymousAccessType.NOT_ALLOWED, monitor );
        IStatus status = validateScmAccess( getBaseUri() + "resources/projects/svn-with-client-certificate.xml" );
        assertTrue( status.toString(), isUnauthorizedStatus( status ) );
    }

    public void testClientCertificateMaterialization()
        throws Exception
    {
        File certificatePath = new File( "resources/svn-with-client-certificate.p12" ).getCanonicalFile();
        addRealmAndURL( "ssltest", getBaseUri(), certificatePath, "X9v.kNGbOgp5OnR?" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( "https://svntest.sonatype.org:444", "ssltest",
                                                         AnonymousAccessType.NOT_ALLOWED, monitor );

        String projectUrl = getBaseUri() + "resources/projects/svn-with-client-certificate.xml";

        IS2Project project = getProjectCore().loadProject( projectUrl, monitor );

        getProjectCore().materialize( project, false, new ConsoleProgressMonitor() );

        assertMavenProject( "test", "svn-with-client-certificate", "0.0.1-SNAPSHOT" );

        Util.waitForTeamSharingJobs();
        assertWorkspaceProjectShared( "svn-with-client-certificate" );

        IRepositoryLocation location =
            SVNRemoteStorage.instance().getRepositoryLocation( getWorkspaceProject( "svn-with-client-certificate" ) );
    
        assertEquals( certificatePath, new File( location.getSSLSettings().getCertificatePath() ).getCanonicalFile() );
    }

    /*
     * MECLIPSE-1567 - Verify SVN behaviour with a previously unused Security Realm requiring SSL certificate
     */
    public void testValidationClientCertificateWithSecurityRealm()
        throws Exception
    {
        addRealmAndURL( "ssltest2", "https://svntest.sonatype.org:444", AuthenticationType.CERTIFICATE,
                        AnonymousAccessType.ALLOWED );

        HttpServer httpServer = startHttpServer();

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/svn-with-client-certificate.xml" );

        assertTrue( status.toString(), isUnauthorizedStatus( status ) );
    }

    public void testValidateBadCredentials_SvnOverSvn_RepositoryAllowsAnonymous()
        throws Exception
    {
        startSvnServer( "simple.dump", "conf-anonymous-read" );

        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "testuser", "wrongpass" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( svnServer.getUrl(), "test", AnonymousAccessType.NOT_ALLOWED,
                                                         monitor );

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/svn-over-svn-credentials.xml" );
        assertTrue( status.toString(), isUnauthorizedStatus( status ) );
    }

    public void testValidateGoodCredentials_SvnOverSvn_RepositoryAllowsAnonymous()
        throws Exception
    {
        startSvnServer( "simple.dump", "conf-anonymous-read" );

        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "testuser", "testpass" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( svnServer.getUrl(), "test", AnonymousAccessType.NOT_ALLOWED,
                                                         monitor );

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/svn-over-svn-credentials.xml" );
        assertTrue( status.toString(), status.isOK() );
    }

    public void testValidateAnonymous_SvnOverSvn()
        throws Exception
    {
        startSvnServer( "simple.dump", "conf-b" );

        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "", "" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( svnServer.getUrl(), "test", AnonymousAccessType.ALLOWED,
                                                         monitor );

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/svn-over-svn-credentials.xml" );
        assertTrue( status.toString(), isUnauthorizedStatus( status ) );
    }

    public void testValidateAnonymous_SvnOverSvn_RepositoryAllowsAnonymous()
        throws Exception
    {
        startSvnServer( "simple.dump", "conf-anonymous-read" );

        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "", "" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( svnServer.getUrl(), "test", AnonymousAccessType.ALLOWED,
                                                         monitor );

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/svn-over-svn-credentials.xml" );
        assertTrue( status.toString(), status.isOK() );
    }

    public void testValidateAnonymous_SvnOverHttp_RepositoryAllowsAnonymous()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "", "" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( "http://scmtest.grid.sonatype.com", "test",
                                                         AnonymousAccessType.ALLOWED,
                                                         monitor );

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/svn-over-http-credentials.xml" );
        assertTrue( status.toString(), status.isOK() );
    }

    /*
     * This unit test is disabled because svn over http ignores the provided credentials on read operations if the
     * repository allows anonymous read access
     */
    public void ttestValidateBadCredentials_SvnOverHttp_RepositoryAllowsAnonymous()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "hudson", "wrongpass" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( "http://scmtest.grid.sonatype.com", "test",
                                                         AnonymousAccessType.NOT_ALLOWED, monitor );

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/svn-over-http-credentials.xml" );
        assertTrue( status.toString(), isUnauthorizedStatus( status ) );
    }

    public void testValidateGoodCredentials_SvnOverHttp_RepositoryAllowsAnonymous()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "test", httpServer.getHttpUrl(), "hudson", "hudson" );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( "http://scmtest.grid.sonatype.com", "test",
                                                         AnonymousAccessType.NOT_ALLOWED,
                                                         monitor );

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/svn-over-http-credentials.xml" );
        assertTrue( status.toString(), status.isOK() );
    }
}
