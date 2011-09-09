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
package com.sonatype.s2.nexus;

import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.IAuthRegistry;
import org.maven.ide.eclipse.authentication.internal.AuthRegistry;

import com.sonatype.s2.nexus.securityrealm.persistence.NexusSecurityRealmPersistence;

public class NexusFacadeTest
    extends AbstractNexusPersistenceTest
{
    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            NexusFacade.removeMainNexusServerURL();
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testGetSetNexusMainNexusServer()
        throws Exception
    {
        assertFalse( NexusSecurityRealmPersistence.isInUse() );

        assertNull( NexusFacade.getMainNexusServerURL() );

        String nexusURL = "http://foo/testGetSetNexusMainNexusServer";
        NexusFacade.setMainNexusServerData( nexusURL, "user", "pass", monitor );
        assertEquals( nexusURL, NexusFacade.getMainNexusServerURL() );
        assertFalse( NexusSecurityRealmPersistence.isInUse() );
        IAuthData authData = simpleAuthService.select( new URI( nexusURL ) );
        assertEquals( "user", authData.getUsername() );
        assertEquals( "pass", authData.getPassword() );
        simpleAuthService.removeURI( new URI( nexusURL ) );

        // Test end slash is removed
        NexusFacade.setMainNexusServerData( "http://foo/testGetSetNexusMainNexusServer/", "user", "pass", monitor );
        assertEquals( "http://foo/testGetSetNexusMainNexusServer", NexusFacade.getMainNexusServerURL() );
        assertFalse( NexusSecurityRealmPersistence.isInUse() );
        authData = simpleAuthService.select( new URI( nexusURL ) );
        assertEquals( "user", authData.getUsername() );
        assertEquals( "pass", authData.getPassword() );
        simpleAuthService.removeURI( new URI( nexusURL ) );
    }

    public void testLoadAuthRegistry_InvalidNexusURL()
        throws Exception
    {
        NexusFacade.setMainNexusServerData( "http://foo/", "user", "pass", monitor );
        // Should not fail
        IAuthRegistry authRegistry = new AuthRegistry();

        authRegistry.clear();
    }

    public void testSetNexusMainNexusServer_RealNexusServer()
        throws Exception
    {
        if ( !enabled )
        {
            return;
        }

        NexusFacade.setMainNexusServerData( NEXUS_URL, NEXUS_USERNAME, NEXUS_PASSWORD, monitor );
        assertEquals( NEXUS_URL, NexusFacade.getMainNexusServerURL() );
        assertFalse( NexusSecurityRealmPersistence.isInUse() );
        IAuthData authData = simpleAuthService.select( new URI( NEXUS_URL ) );
        assertEquals( NEXUS_USERNAME, authData.getUsername() );
        assertEquals( NEXUS_PASSWORD, authData.getPassword() );

        AuthRegistry authRegistry = (AuthRegistry) AuthFacade.getAuthRegistry();
        assertTrue( NexusSecurityRealmPersistence.isInUse() );
        int loadCount = authRegistry.getLoadCount();
        assertEquals( 1, loadCount );

        authRegistry.reload( monitor );
        assertTrue( NexusSecurityRealmPersistence.isInUse() );
        loadCount = authRegistry.getLoadCount();
        assertEquals( 2, loadCount );

        // Assert the registry is not reloaded when not needed
        NexusFacade.setMainNexusServerData( NEXUS_URL, NEXUS_USERNAME, NEXUS_PASSWORD, monitor );
        loadCount = authRegistry.getLoadCount();
        assertEquals( 2, loadCount );

        // Now set it to a bogus URL
        try
        {
            NexusFacade.setMainNexusServerData( "http://foo", NEXUS_USERNAME, NEXUS_PASSWORD, monitor );
        }
        catch ( Exception expected )
        {
            expected.printStackTrace();
        }
        loadCount = authRegistry.getLoadCount();
        assertEquals( 3, loadCount );

        authRegistry.clear();
    }

    /*
     * Verify the error message when we connect to an unresolved repository
     */
    public void testUnresolvedNexusAddress()
        throws Exception
    {
        IStatus result =
            NexusFacade.validateCredentials( "http://badhost.void.void", "username",
                                             "password", AnonymousAccessType.NOT_ALLOWED,
                                             new NullProgressMonitor() );
        assertFalse( "Validate credentials should have failed", result.isOK() );
        assertEquals( "Unexpected status message", "Connection failed, unresolved address", result.getMessage() );
    }
    
    /*
     * Verify the message when connecting to an address where nothing responds
     */
    public void testServerUnavailable() 
    	throws Exception
    {
        IStatus result =
            NexusFacade.validateCredentials( "http://localhost", "username", "password", AnonymousAccessType.NOT_ALLOWED,
                                             new NullProgressMonitor() );
    	assertTrue("Unexpected status message " + result.getMessage(), result.getMessage().startsWith("Connection refused"));
    }
}
