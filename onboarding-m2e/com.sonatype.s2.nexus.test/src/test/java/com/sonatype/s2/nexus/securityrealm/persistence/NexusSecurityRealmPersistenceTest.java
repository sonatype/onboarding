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
package com.sonatype.s2.nexus.securityrealm.persistence;

import java.util.Collection;

import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.authentication.ISecurityRealmURLAssoc;

import com.sonatype.s2.nexus.AbstractNexusPersistenceTest;
import com.sonatype.s2.nexus.NexusFacade;

public class NexusSecurityRealmPersistenceTest
    extends AbstractNexusPersistenceTest
{
    public void testCrudRealms()
        throws Exception
    {
        if ( !enabled )
        {
            return;
        }

        NexusFacade.setMainNexusServerData( NEXUS_URL, NEXUS_USERNAME, NEXUS_PASSWORD, monitor );
        // NexusSecurityRealmPersistence persistence = new NexusSecurityRealmPersistence();

        // List
        Collection<IAuthRealm> realms = AuthFacade.getAuthRegistry().getRealms();
        assertNotNull( realms );
        int initialRealmCount = realms.size();

        // Add
        String newRealmId = "NexusSecurityRealmPersistenceTest" + System.currentTimeMillis();
        assertNull( AuthFacade.getAuthRegistry().getRealm( newRealmId ) );
        IAuthRealm realm =
            AuthFacade.getAuthRegistry().addRealm( newRealmId, "My realm name", "My realm desc",
                                                   AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD, monitor );
        AuthFacade.getAuthRegistry().reload( monitor );
        realms = AuthFacade.getAuthRegistry().getRealms();
        assertNotNull( realms );
        assertEquals( initialRealmCount + 1, realms.size() );
        realm = AuthFacade.getAuthRegistry().getRealm( newRealmId );
        assertNotNull( realm );
        assertEquals( newRealmId, realm.getId() );
        assertEquals( "My realm name", realm.getName() );
        assertEquals( "My realm desc", realm.getDescription() );
        assertEquals( AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD,
                      realm.getAuthenticationType() );

        // Update
        realm.setName( "My realm name updated" );
        AuthFacade.getAuthRegistry().updateRealm( realm, monitor );
        AuthFacade.getAuthRegistry().reload( monitor );
        realms = AuthFacade.getAuthRegistry().getRealms();
        assertNotNull( realms );
        assertEquals( initialRealmCount + 1, realms.size() );
        realm = AuthFacade.getAuthRegistry().getRealm( newRealmId );
        assertEquals( newRealmId, realm.getId() );
        assertEquals( "My realm name updated", realm.getName() );

        // Delete
        AuthFacade.getAuthRegistry().removeRealm( newRealmId, monitor );
        AuthFacade.getAuthRegistry().reload( monitor );
        realms = AuthFacade.getAuthRegistry().getRealms();
        assertNotNull( realms );
        assertEquals( initialRealmCount, realms.size() );
        assertNull( AuthFacade.getAuthRegistry().getRealm( newRealmId ) );
    }

    public void testCrudUrls()
        throws Exception
    {
        if ( !enabled )
        {
            return;
        }

        NexusFacade.setMainNexusServerData( NEXUS_URL, NEXUS_USERNAME, NEXUS_PASSWORD, monitor );
        // NexusSecurityRealmPersistence persistence = new NexusSecurityRealmPersistence();
        // Add a parent realm
        String realmId = "NexusSecurityRealmPersistenceTest.testCrudUrls" + System.currentTimeMillis();
        IAuthRealm realm =
            AuthFacade.getAuthRegistry().addRealm( realmId, "My realm name", "My realm desc",
                                                   AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD, monitor );
        
        // List
        Collection<ISecurityRealmURLAssoc> urlAssocs = AuthFacade.getAuthRegistry().getURLToRealmAssocs();
        assertNotNull( urlAssocs );
        int initialUrlCount = urlAssocs.size();

        // Add
        String newUrl = "http://foo/" + System.currentTimeMillis();
        ISecurityRealmURLAssoc urlAssoc =
            AuthFacade.getAuthRegistry().addURLToRealmAssoc( newUrl, realmId, AnonymousAccessType.NOT_ALLOWED, monitor );
        AuthFacade.getAuthRegistry().reload( monitor );
        String newUrlAssocId = urlAssoc.getId();
        assertNotNull( newUrlAssocId );
        urlAssocs = AuthFacade.getAuthRegistry().getURLToRealmAssocs();
        assertNotNull( urlAssocs );
        assertEquals( initialUrlCount + 1, urlAssocs.size() );
        urlAssoc = AuthFacade.getAuthRegistry().getURLToRealmAssoc( newUrlAssocId );
        assertNotNull( urlAssoc );
        assertEquals( newUrlAssocId, urlAssoc.getId() );
        assertEquals( newUrl, urlAssoc.getUrl() );
        assertEquals( realmId, urlAssoc.getRealmId() );
        assertEquals( AnonymousAccessType.NOT_ALLOWED, urlAssoc.getAnonymousAccess() );

        // Update
        newUrl = "http://foo1/" + System.currentTimeMillis();
        urlAssoc.setUrl( newUrl );
        urlAssoc.setAnonymousAccess( AnonymousAccessType.ALLOWED );
        AuthFacade.getAuthRegistry().updateURLToRealmAssoc( urlAssoc, monitor );
        AuthFacade.getAuthRegistry().reload( monitor );
        urlAssocs = AuthFacade.getAuthRegistry().getURLToRealmAssocs();
        assertNotNull( urlAssocs );
        assertEquals( initialUrlCount + 1, urlAssocs.size() );
        urlAssoc = AuthFacade.getAuthRegistry().getURLToRealmAssoc( newUrlAssocId );
        assertNotNull( urlAssoc );
        assertEquals( newUrlAssocId, urlAssoc.getId() );
        assertEquals( newUrl, urlAssoc.getUrl() );
        assertEquals( realmId, urlAssoc.getRealmId() );
        assertEquals( AnonymousAccessType.ALLOWED, urlAssoc.getAnonymousAccess() );

        // Delete
        AuthFacade.getAuthRegistry().removeURLToRealmAssoc( newUrlAssocId, monitor );
        AuthFacade.getAuthRegistry().reload( monitor );
        urlAssocs = AuthFacade.getAuthRegistry().getURLToRealmAssocs();
        assertNotNull( urlAssocs );
        assertEquals( initialUrlCount, urlAssocs.size() );
        assertNull( AuthFacade.getAuthRegistry().getURLToRealmAssoc( newUrlAssocId ) );
    }
}
