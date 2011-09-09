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
package com.sonatype.s2.project.validator;

import java.io.File;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.authentication.AuthFacade;

import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.IUrlLocation;

public class P2LineupAccessValidatorTest
    extends AbstractValidatorTest
{
    private static final String USERNAME = "username";

    private static final String PASSWORD = "password";

    private static final String ROLE = "role";

    private HttpServer httpServer;

    private String baseUrl;

    @Override
    protected void tearDown()
        throws Exception
    {
        if ( httpServer != null )
        {
            httpServer.stop();
        }
        super.tearDown();
    }

    private void startHttpServer()
        throws Exception
    {
        httpServer = new HttpServer();
        httpServer.addResources( "/", "resources", "xml" );
        httpServer.addSecuredRealm( "/p2lineup/testaccess/*", ROLE );
        httpServer.addUser( USERNAME, PASSWORD, ROLE );
        httpServer.start();
        baseUrl = httpServer.getHttpUrl();
    }

    private void assertErrorStatus( IStatus status, String messagePrefix )
    {
        assertEquals( getMessage( status ), status.getSeverity(), IStatus.ERROR );
        assertTrue( getMessage( status ), status.getMessage().startsWith( messagePrefix ) );
        assertTrue( getMessage( status ), status instanceof AccessValidationStatus );
    }

    private String getMessage( IStatus status )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( status.getMessage() );
        if ( status.isMultiStatus() )
        {
            IStatus[] children = status.getChildren();
            for ( int i = 0; i < children.length; i++ )
            {
                sb.append( '\n' ).append( i ).append( ": " ).append( children[i].getMessage() );
            }
        }
        return sb.toString();
    }

    public void testNoP2Lineup()
        throws Exception
    {
        S2ProjectCore core = S2ProjectCore.getInstance();

        File file = new File( "resources/projects/descriptorWithoutP2Lineup.xml" );
        IS2Project s2Project = core.loadProject( file.toURI().toURL().toExternalForm(), new NullProgressMonitor() );
        assertNull( s2Project.getP2LineupLocation() );

        IStatus status = new P2LineupAccessValidator().validate( s2Project, new NullProgressMonitor() );
        assertNull( status );
    }

    public void testGoodCredentials()
        throws Exception
    {
        startHttpServer();

        String projectURL = baseUrl + "/projects/descriptorWithAuthenticatedP2LineupURL.xml";
        addRealmAndURL( "test.p2lineup.access.realmId", httpServer.getHttpUrl(), USERNAME, PASSWORD );

        IStatus status = validateAccess( projectURL, null /* securityRealmIdFilter */);
        assertTrue( getMessage( status ), status.isOK() );
        assertTrue( getMessage( status ), status instanceof AccessValidationStatus );

        status = validateAccess( projectURL, "test.p2lineup.access.realmId" );
        assertTrue( getMessage( status ), status.isOK() );
        assertTrue( getMessage( status ), status instanceof AccessValidationStatus );

        status = validateAccess( projectURL, "test.p2lineup.access.realmId.doesNotExist" );
        assertNull( status );
    }

    public void testBadCredentialsUserDoesNotExist()
        throws Exception
    {
        startHttpServer();

        String projectURL = baseUrl + "/projects/descriptorWithAuthenticatedP2LineupURL.xml";
        addRealmAndURL( "test.p2lineup.access.realmId", httpServer.getHttpUrl(), "baduser", PASSWORD );

        IStatus status = validateAccess( projectURL, null /* securityRealmIdFilter */);
        assertErrorStatus( status,
                           "Error validating project S2ProjectWithAuthenticatedP2Lineup: HTTP status code 401: Unauthorized: " );

        AuthFacade.getAuthService().save( httpServer.getHttpUrl(), USERNAME, PASSWORD );
        status = validateAccess( projectURL, "test.p2lineup.access.realmId.doesNotExist" );
        assertNull( status );
    }

    public void testBadCredentialsIncorrectPassword()
        throws Exception
    {
        startHttpServer();

        String projectURL = baseUrl + "/projects/descriptorWithAuthenticatedP2LineupURL.xml";
        addRealmAndURL( "test.p2lineup.access.realmId", httpServer.getHttpUrl(), USERNAME, "blabla" );

        IStatus status = validateAccess( projectURL, null /* securityRealmIdFilter */);
        assertErrorStatus( status,
                           "Error validating project S2ProjectWithAuthenticatedP2Lineup: HTTP status code 401: Unauthorized: " );

        AuthFacade.getAuthService().save( httpServer.getHttpUrl(), USERNAME, PASSWORD );
        status = validateAccess( projectURL, "test.p2lineup.access.realmId.doesNotExist" );
        assertNull( status );
    }

    private IStatus validateAccess( String projectURL, String securityRealmIdFilter )
        throws Exception
    {
        S2ProjectCore core = S2ProjectCore.getInstance();

        IS2Project s2Project = core.loadProject( projectURL, new NullProgressMonitor() );
        IUrlLocation location = s2Project.getP2LineupLocation();
        assertNotNull( location );

        P2LineupAccessValidator projectValidator = new P2LineupAccessValidator();
        IStatus status = projectValidator.validate( s2Project, securityRealmIdFilter, new NullProgressMonitor() );
        assertFalse( projectValidator.canRemediate( true ).isOK() );
        assertFalse( projectValidator.canRemediate( false ).isOK() );
        return status;
    }
}
