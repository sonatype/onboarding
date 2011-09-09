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
package com.sonatype.nexus.onboarding.its.meclipse1356SecurityRealmRestApi;

import java.util.List;

import org.restlet.data.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sonatype.nexus.onboarding.its.AbstractOnboardingIT;
import com.sonatype.nexus.onboarding.its.util.SecurityRealmMessageUtil;
import com.sonatype.nexus.onboarding.its.util.SecurityRealmURLAssocMessageUtil;
import com.sonatype.s2.securityrealm.model.S2AnonymousAccessType;
import com.sonatype.s2.securityrealm.model.S2SecurityRealm;
import com.sonatype.s2.securityrealm.model.S2SecurityRealmAuthenticationType;
import com.sonatype.s2.securityrealm.model.S2SecurityRealmURLAssoc;

public class meclipse1356SecurityRealmRestApiIT
    extends AbstractOnboardingIT
{
    @Test
    public void crudRealm()
        throws Exception
    {
        List<S2SecurityRealm> list = SecurityRealmMessageUtil.listRealms();
        Assert.assertEquals( 0, list.size() );

        // Create
        S2SecurityRealm realm = new S2SecurityRealm();
        realm.setId( "realm-it" );
        realm.setName( "realm-it name" );
        realm.setDescription( "realm-it description" );
        Response r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error creating realm:" + r.getStatus() );

        String realmId = realm.getId();
        Assert.assertNotNull( realmId );

        // List
        list = SecurityRealmMessageUtil.listRealms();
        Assert.assertEquals( 1, list.size() );
        realm = list.get( 0 );
        Assert.assertNotNull( realm );
        Assert.assertEquals( realmId, realm.getId() );
        Assert.assertEquals( "realm-it name", realm.getName() );
        Assert.assertEquals( "realm-it description", realm.getDescription() );
        Assert.assertEquals( S2SecurityRealmAuthenticationType.USERNAME_PASSWORD, realm.getAuthenticationType() );

        // Get
        realm = SecurityRealmMessageUtil.getRealm( realmId );
        Assert.assertNotNull( realm );
        Assert.assertEquals( realmId, realm.getId() );
        Assert.assertEquals( "realm-it name", realm.getName() );
        Assert.assertEquals( "realm-it description", realm.getDescription() );
        Assert.assertEquals( S2SecurityRealmAuthenticationType.USERNAME_PASSWORD, realm.getAuthenticationType() );

        // Update
        realm.setName( "realm-it name update" );
        realm.setDescription( "realm-it description update" );
        realm.setAuthenticationType( S2SecurityRealmAuthenticationType.CERTIFICATE );
        r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error updating realm:" + r.getStatus() );

        // Get
        realm = SecurityRealmMessageUtil.getRealm( realmId );
        Assert.assertNotNull( realm );
        Assert.assertEquals( realmId, realm.getId() );
        Assert.assertEquals( "realm-it name update", realm.getName() );
        Assert.assertEquals( "realm-it description update", realm.getDescription() );
        Assert.assertEquals( S2SecurityRealmAuthenticationType.CERTIFICATE, realm.getAuthenticationType() );

        // Delete
        r = SecurityRealmMessageUtil.deleteRealm( realmId );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error deleting catalog:\n" + r.getStatus() );
        Assert.assertEquals( 204, r.getStatus().getCode() );

        // List
        list = SecurityRealmMessageUtil.listRealms();
        Assert.assertEquals( 0, list.size() );
    }

    @Test( dependsOnMethods = { "crudRealm" } )
    public void createUpdateRealm_IdMismatch()
        throws Exception
    {
        List<S2SecurityRealm> list = SecurityRealmMessageUtil.listRealms();
        Assert.assertEquals( 0, list.size() );

        // Create
        S2SecurityRealm realm = new S2SecurityRealm();
        realm.setId( "realm-it" );
        realm.setName( "realm-it name" );
        realm.setDescription( "realm-it description" );
        Response r = SecurityRealmMessageUtil.createOrUpdateRealm( "fooId", realm );
        Assert.assertEquals( 400, r.getStatus().getCode() );
        r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error creating realm:" + r.getStatus() );

        // Update
        realm.setName( "realm-it name update" );
        r = SecurityRealmMessageUtil.createOrUpdateRealm( "fooId", realm );
        Assert.assertEquals( 400, r.getStatus().getCode() );
        r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error updating realm:" + r.getStatus() );
    }

    @Test( dependsOnMethods = "createUpdateRealm_IdMismatch" )
    public void crudRealmURLAssoc()
        throws Exception
    {
        S2SecurityRealm realm = new S2SecurityRealm();
        realm.setId( "realm-it" );
        realm.setName( "realm-it name" );
        realm.setDescription( "realm-it description" );
        Response r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error creating realm:" + r.getStatus() );

        List<S2SecurityRealmURLAssoc> list = SecurityRealmURLAssocMessageUtil.listUrls();
        Assert.assertEquals( 0, list.size() );

        // Create
        S2SecurityRealmURLAssoc urlAssoc = new S2SecurityRealmURLAssoc();
        urlAssoc.setRealmId( realm.getId() );
        urlAssoc.setUrl( "http://foo" );
        r = SecurityRealmURLAssocMessageUtil.createURL( urlAssoc );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error creating URL assoc:" + r.getStatus() );

        String urlId = SecurityRealmURLAssocMessageUtil.getURL( r ).getId();
        Assert.assertNotNull( urlId );

        // List
        list = SecurityRealmURLAssocMessageUtil.listUrls();
        Assert.assertEquals( 1, list.size() );
        urlAssoc = list.get( 0 );
        Assert.assertNotNull( urlAssoc );
        Assert.assertEquals( urlId, urlAssoc.getId() );
        Assert.assertEquals( realm.getId(), urlAssoc.getRealmId() );
        Assert.assertEquals( "http://foo", urlAssoc.getUrl() );
        Assert.assertEquals( S2AnonymousAccessType.NOT_ALLOWED, urlAssoc.getAnonymousAccess() );

        // Get
        urlAssoc = SecurityRealmURLAssocMessageUtil.getURL( urlId );
        Assert.assertNotNull( urlAssoc );
        Assert.assertEquals( urlId, urlAssoc.getId() );
        Assert.assertEquals( realm.getId(), urlAssoc.getRealmId() );
        Assert.assertEquals( "http://foo", urlAssoc.getUrl() );
        Assert.assertEquals( S2AnonymousAccessType.NOT_ALLOWED, urlAssoc.getAnonymousAccess() );

        // Update
        urlAssoc.setUrl( "http://foo/update" );
        urlAssoc.setAnonymousAccess( S2AnonymousAccessType.REQUIRED );
        r = SecurityRealmURLAssocMessageUtil.updateURL( urlAssoc );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error updating URL assoc:" + r.getStatus() );

        // Get
        urlAssoc = SecurityRealmURLAssocMessageUtil.getURL( urlId );
        Assert.assertNotNull( urlAssoc );
        Assert.assertEquals( urlId, urlAssoc.getId() );
        Assert.assertEquals( realm.getId(), urlAssoc.getRealmId() );
        Assert.assertEquals( "http://foo/update", urlAssoc.getUrl() );
        Assert.assertEquals( S2AnonymousAccessType.REQUIRED, urlAssoc.getAnonymousAccess() );

        r = SecurityRealmURLAssocMessageUtil.deleteURL( urlId );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error deleting URL assoc:\n" + r.getStatus() );
        Assert.assertEquals( 204, r.getStatus().getCode() );

        list = SecurityRealmURLAssocMessageUtil.listUrls();
        Assert.assertEquals( 0, list.size() );
    }

    @Test( dependsOnMethods = { "crudRealmURLAssoc" } )
    public void updateRealmURLAssoc_IdMismatch()
        throws Exception
    {
        S2SecurityRealm realm = new S2SecurityRealm();
        realm.setId( "realm-it" );
        realm.setName( "realm-it name" );
        realm.setDescription( "realm-it description" );
        Response r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error creating realm:" + r.getStatus() );

        // Create
        S2SecurityRealmURLAssoc urlAssoc = new S2SecurityRealmURLAssoc();
        urlAssoc.setRealmId( realm.getId() );
        urlAssoc.setUrl( "http://foo" );
        r = SecurityRealmURLAssocMessageUtil.createURL( urlAssoc );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error creating URL assoc:" + r.getStatus() );

        urlAssoc = SecurityRealmURLAssocMessageUtil.getURL( r );
        Assert.assertNotNull( urlAssoc );
        String urlId = urlAssoc.getId();
        Assert.assertNotNull( urlId );

        // Update
        urlAssoc.setUrl( "http://foo/update" );
        r = SecurityRealmURLAssocMessageUtil.updateURL( "fooId", urlAssoc );
        Assert.assertEquals( 400, r.getStatus().getCode() );
        r = SecurityRealmURLAssocMessageUtil.updateURL( urlAssoc );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error updating URL assoc:" + r.getStatus() );
    }
}
