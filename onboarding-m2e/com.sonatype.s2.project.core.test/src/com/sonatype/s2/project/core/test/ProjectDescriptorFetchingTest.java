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

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.authentication.internal.AuthData;

import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.model.IS2Project;

public class ProjectDescriptorFetchingTest
    extends TestCase
{

    private S2ProjectCore s2;

    private HttpServer server;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        s2 = new S2ProjectCore();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        s2 = null;

        if ( server != null )
        {
            server.stop();
            server = null;
        }

        super.tearDown();
    }

    protected HttpServer newServer()
        throws Exception
    {
        server = new HttpServer();
        return server;
    }

    protected IS2Project loadDescriptor( String url )
        throws Exception
    {
        return s2.loadProject( url, new NullProgressMonitor() );
    }

    public void testFile()
        throws Exception
    {
        IS2Project s2Project = loadDescriptor( new File( "resources/descriptor.xml" ).toURI().toString() );
        assertNotNull( s2Project );
    }

    public void testBadProtocol()
        throws Exception
    {
        try
        {
            loadDescriptor( "bad://host/descriptor" );
            fail();
        }
        catch ( CoreException e )
        {
            assertTrue( true );
        }
    }

    public void testHttpConnectionFailure()
        throws Exception
    {
        try
        {
            loadDescriptor( "http://localhost:43251/descriptor.xml" );
            fail();
        }
        catch ( CoreException e )
        {
            assertTrue( true );
        }
    }

    public void testHttpNotFound()
        throws Exception
    {
        newServer();
        server.start();

        try
        {
            loadDescriptor( server.getHttpUrl() + "/descriptor.xml" );
            fail();
        }
        catch ( CoreException e )
        {
            assertTrue( true );
        }
    }

    public void testHttpTimeout()
        throws Exception
    {
        newServer();
        server.setLatency( -1 );
        server.start();

        try
        {
            loadDescriptor( server.getHttpUrl() + "/descriptor.xml" );
            fail();
        }
        catch ( CoreException e )
        {
            assertTrue( true );
        }
    }

    public void testHttpUnauthorized()
        throws Exception
    {
        newServer();
        server.addSecuredRealm( "/*", "auth" );
        server.addUser( "testuser", "testpass", "auth" );
        server.addResources( "/", "resources" );
        server.start();

        try
        {
            loadDescriptor( server.getHttpUrl() + "/descriptor.xml" );
            fail();
        }
        catch ( CoreException e )
        {
            assertTrue( true );
        }
    }

    public void testHttpBasic()
        throws Exception
    {
        newServer();
        server.addResources( "/", "resources" );
        server.start();

        assertNotNull( loadDescriptor( server.getHttpUrl() + "/descriptor.xml" ) );
    }

    public void testHttpsBasic()
        throws Exception
    {
        newServer();
        server.addResources( "/", "resources" );
        server.start();

        assertNotNull( loadDescriptor( server.getHttpsUrl() + "/descriptor.xml" ) );
    }

    public void testHttpAuthentication()
        throws Exception
    {
        newServer();
        server.addSecuredRealm( "/*", "auth" );
        server.addUser( "testuser", "testpass", "auth" );
        server.addResources( "/", "resources" );
        server.start();

        setupAuth( server.getHttpUrl(), "testuser", "testpass" );
        assertNotNull( loadDescriptor( server.getHttpUrl() + "/descriptor.xml" ) );
    }

    public void testHttpsAuthentication()
        throws Exception
    {
        newServer();
        server.addSecuredRealm( "/*", "auth" );
        server.addUser( "testuser", "testpass", "auth" );
        server.addResources( "/", "resources" );
        server.start();

        setupAuth( server.getHttpsUrl(), "testuser", "testpass" );
        assertNotNull( loadDescriptor( server.getHttpsUrl() + "/descriptor.xml" ) );
    }

    public void testHttpToHttpsAuthentication()
        throws Exception
    {
        newServer();
        server.setRedirectToHttps( true );
        server.addSecuredRealm( "/*", "auth" );
        server.addUser( "testuser", "testpass", "auth" );
        server.addResources( "/", "resources" );
        server.start();

        setupAuth( server.getHttpUrl(), "testuser", "testpass" );
        assertNotNull( loadDescriptor( server.getHttpUrl() + "/descriptor.xml" ) );
    }

    private void setupAuth( String url, String username, String password )
        throws Exception
    {
        IProgressMonitor monitor = new NullProgressMonitor();

        IAuthData authData = null;

        String realmId = url;
        IAuthRealm realm = AuthFacade.getAuthRegistry().getRealm( realmId );
        if ( realm == null )
        {
            realm =
                AuthFacade.getAuthRegistry().addRealm( realmId, realmId, realmId, AuthenticationType.USERNAME_PASSWORD,
                                                       monitor );
            AuthFacade.getAuthRegistry().addURLToRealmAssoc( url, realmId, AnonymousAccessType.NOT_ALLOWED, monitor );
        }
        else
        {
            authData = AuthFacade.getAuthService().select( url );
        }

        if ( authData == null )
        {
            authData = new AuthData( username, password, AnonymousAccessType.NOT_ALLOWED );
        }
        else
        {
            authData.setUsernameAndPassword( username, password );
        }
        AuthFacade.getAuthService().save( url, authData );
    }
}
