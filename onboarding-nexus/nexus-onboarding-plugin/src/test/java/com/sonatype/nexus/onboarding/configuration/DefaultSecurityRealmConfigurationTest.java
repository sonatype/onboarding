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
package com.sonatype.nexus.onboarding.configuration;

import org.junit.Test;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.nexus.AbstractNexusTestCase;

import com.sonatype.nexus.onboarding.persist.model.CSecurityRealm;
import com.sonatype.nexus.onboarding.persist.model.CSecurityRealmURLAssoc;
import com.sonatype.s2.securityrealm.model.S2AnonymousAccessType;
import com.sonatype.s2.securityrealm.model.S2SecurityRealmAuthenticationType;

public class DefaultSecurityRealmConfigurationTest
    extends AbstractNexusTestCase
{
    private SecurityRealmConfiguration cfg;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        this.cfg = lookup( SecurityRealmConfiguration.class );
    }

    @Test
    public void testCRUDRealm()
        throws Exception
    {
        CSecurityRealm realm = new CSecurityRealm();
        realm.setId( "testRealm" );
        realm.setName( "test-realm" );
        realm.setDescription( "test realm description" );
        cfg.createOrUpdateRealm( realm );

        CSecurityRealmURLAssoc urlAssoc = new CSecurityRealmURLAssoc();
        urlAssoc.setRealmId( realm.getId() );
        urlAssoc.setUrl( "http://foo" );
        cfg.createURL( urlAssoc );

        realm = cfg.readRealm( realm.getId() );
        assertNotNull( realm );
        assertEquals( "test-realm", realm.getName() );
        assertEquals( "test realm description", realm.getDescription() );
        assertEquals( S2SecurityRealmAuthenticationType.USERNAME_PASSWORD.toString(), realm.getAuthenticationType() );

        realm.setName( "test-update-realm" );
        realm.setDescription( "test update realm description" );
        realm.setAuthenticationType( S2SecurityRealmAuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD );
        cfg.createOrUpdateRealm( realm );
        urlAssoc = cfg.readURL( urlAssoc.getId() );
        assertNotNull( urlAssoc );

        realm = cfg.readRealm( realm.getId() );
        assertNotNull( realm );
        assertEquals( "test-update-realm", realm.getName() );
        assertEquals( "test update realm description", realm.getDescription() );
        assertEquals( S2SecurityRealmAuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD.toString(),
                      realm.getAuthenticationType() );

        cfg.deleteRealm( realm.getId() );
        try
        {
            realm = cfg.readRealm( realm.getId() );
            fail( "Expected InvalidConfigurationException" );
        }
        catch ( InvalidConfigurationException expected )
        {
            if ( !"No realm found with id: 'testRealm'.".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
        // Assert realm delete is cascaded to the urls associated with it
        try
        {
            urlAssoc = cfg.readURL( urlAssoc.getId() );
            fail( "Expected InvalidConfigurationException" );
        }
        catch ( InvalidConfigurationException expected )
        {
            if ( !( "No URL found for id: '" + urlAssoc.getId() + "'." ).equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testCRUDURL()
        throws Exception
    {
        CSecurityRealm realm = new CSecurityRealm();
        realm.setId( "test-realm" );
        realm.setName( "test-realm" );
        cfg.createOrUpdateRealm( realm );
        CSecurityRealm realm1 = new CSecurityRealm();
        realm1.setId( "test-realm1" );
        realm1.setName( "test-realm1" );
        cfg.createOrUpdateRealm( realm1 );

        // Create
        CSecurityRealmURLAssoc urlAssoc = new CSecurityRealmURLAssoc();
        urlAssoc.setRealmId( realm.getId() );
        urlAssoc.setUrl( "http://foo" );
        cfg.createURL( urlAssoc );
        assertNotNull( urlAssoc.getId() );

        // Get
        urlAssoc = cfg.readURL( urlAssoc.getId() );
        assertNotNull( urlAssoc );
        assertEquals( realm.getId(), urlAssoc.getRealmId() );
        assertEquals( "http://foo", urlAssoc.getUrl() );
        assertEquals( S2AnonymousAccessType.NOT_ALLOWED.toString(), urlAssoc.getAnonymousAccess() );

        // Update
        urlAssoc.setRealmId( realm1.getId() );
        urlAssoc.setUrl( "http://foo1" );
        urlAssoc.setAnonymousAccess( S2AnonymousAccessType.REQUIRED );
        cfg.updateURL( urlAssoc );

        // Get
        urlAssoc = cfg.readURL( urlAssoc.getId() );
        assertNotNull( urlAssoc );
        assertEquals( realm1.getId(), urlAssoc.getRealmId() );
        assertEquals( "http://foo1", urlAssoc.getUrl() );
        assertEquals( S2AnonymousAccessType.REQUIRED.toString(), urlAssoc.getAnonymousAccess() );

        // Delete
        cfg.deleteURL( urlAssoc.getId() );
        try
        {
            urlAssoc = cfg.readURL( urlAssoc.getId() );
            fail( "Expected InvalidConfigurationException" );
        }
        catch ( InvalidConfigurationException expected )
        {
            if ( !( "No URL found for id: '" + urlAssoc.getId() + "'." ).equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testCU_URL_InvalidRealmId()
        throws Exception
    {
        CSecurityRealmURLAssoc urlAssoc = new CSecurityRealmURLAssoc();
        urlAssoc.setRealmId( "foo" );
        urlAssoc.setUrl( "http://foo" );
        try
        {
            cfg.createURL( urlAssoc );
            fail( "Expected InvalidConfigurationException" );
        }
        catch ( InvalidConfigurationException expected )
        {
            if ( !( "No realm found with id: 'foo'." ).equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }

        CSecurityRealm realm = new CSecurityRealm();
        realm.setId( "test-realm" );
        realm.setName( "test-realm" );
        cfg.createOrUpdateRealm( realm );

        urlAssoc.setRealmId( realm.getId() );
        cfg.createURL( urlAssoc );

        urlAssoc.setRealmId( "foo" );
        try
        {
            cfg.updateURL( urlAssoc );
            fail( "Expected InvalidConfigurationException" );
        }
        catch ( InvalidConfigurationException expected )
        {
            if ( !( "No realm found with id: 'foo'." ).equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testRequiredFieldsForRealm()
        throws InvalidConfigurationException
    {
        CSecurityRealm realm = new CSecurityRealm();
        realm.setName( "test-realm" );
        try
        {
            cfg.createOrUpdateRealm( realm );
            fail( "Expected InvalidConfigurationException" );
        }
        catch ( InvalidConfigurationException expected )
        {
            if ( !expected.getMessage().contains( "Realm ID not defined" ) )
            {
                throw expected;
            }
        }

        realm = new CSecurityRealm();
        realm.setId( "test-realm" );
        try
        {
            cfg.createOrUpdateRealm( realm );
            fail( "Expected InvalidConfigurationException" );
        }
        catch ( InvalidConfigurationException expected )
        {
            if ( !expected.getMessage().contains( "Realm name not defined" ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testRequiredFieldsForURL()
        throws InvalidConfigurationException
    {
        CSecurityRealm realm = new CSecurityRealm();
        realm.setId( "test-realm" );
        realm.setName( "test-realm" );
        cfg.createOrUpdateRealm( realm );

        CSecurityRealmURLAssoc urlAssoc = new CSecurityRealmURLAssoc();
        try
        {
            cfg.createURL( urlAssoc );
            fail( "Expected InvalidConfigurationException" );
        }
        catch ( InvalidConfigurationException expected )
        {
            if ( !expected.getMessage().contains( "Realm id not defined" ) )
            {
                throw expected;
            }
        }

        urlAssoc.setRealmId( realm.getId() );
        try
        {
            cfg.createURL( urlAssoc );
            fail( "Expected InvalidConfigurationException" );
        }
        catch ( InvalidConfigurationException expected )
        {
            if ( !expected.getMessage().contains( "URL not defined" ) )
            {
                throw expected;
            }
        }

        urlAssoc.setUrl( "http://foo" );
        cfg.createURL( urlAssoc );

        String urlId = urlAssoc.getId();

        urlAssoc = new CSecurityRealmURLAssoc();
        try
        {
            cfg.updateURL( urlAssoc );
            fail( "Expected InvalidConfigurationException" );
        }
        catch ( InvalidConfigurationException expected )
        {
            if ( !expected.getMessage().contains( "Id not defined" ) )
            {
                throw expected;
            }
        }

        urlAssoc.setId( urlId );
        try
        {
            cfg.updateURL( urlAssoc );
            fail( "Expected InvalidConfigurationException" );
        }
        catch ( InvalidConfigurationException expected )
        {
            if ( !expected.getMessage().contains( "Realm id not defined" ) )
            {
                throw expected;
            }
        }

        urlAssoc.setRealmId( realm.getId() );
        try
        {
            cfg.updateURL( urlAssoc );
            fail( "Expected InvalidConfigurationException" );
        }
        catch ( InvalidConfigurationException expected )
        {
            if ( !expected.getMessage().contains( "URL not defined" ) )
            {
                throw expected;
            }
        }

        urlAssoc.setUrl( "http://foo" );
        cfg.updateURL( urlAssoc );
    }

    @Test
    public void testInvalidDeleteRealm()
        throws Exception
    {
        try
        {
            cfg.deleteRealm( "AAbbCC" );
            fail( "Expected InvalidConfigurationException" );
        }
        catch ( InvalidConfigurationException expected )
        {
            if ( !"Unable to delete realm 'AAbbCC'. Realm not found.".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    @Test
    public void testInvalidDeleteUrl()
        throws Exception
    {
        try
        {
            cfg.deleteURL( "AAbbCC" );
            fail( "Expected InvalidConfigurationException" );
        }
        catch ( InvalidConfigurationException expected )
        {
            if ( !"Unable to delete URL with id 'AAbbCC'. URL id not found.".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }
}
