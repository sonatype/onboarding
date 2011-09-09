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
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.security.rest.model.UserResource;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sonatype.nexus.onboarding.its.AbstractOnboardingPrivilegeIT;
import com.sonatype.nexus.onboarding.its.util.SecurityRealmMessageUtil;
import com.sonatype.nexus.onboarding.its.util.SecurityRealmURLAssocMessageUtil;
import com.sonatype.s2.securityrealm.model.S2SecurityRealm;
import com.sonatype.s2.securityrealm.model.S2SecurityRealmURLAssoc;

public class meclipse1356SecurityRealmRestApiPrivilegedIT
    extends AbstractOnboardingPrivilegeIT
{
    @Test
    public void crudRealmPrivs()
        throws Exception
    {
        resetTestUserPrivs();
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        // should fail no perms
        List<S2SecurityRealm> list = SecurityRealmMessageUtil.listRealms( false );
        Assert.assertNull( list );
        
        giveUserPrivilege( TEST_USER_NAME, "onboarding-security-realm-read" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        // should pass now has perm
        list = SecurityRealmMessageUtil.listRealms();
        Assert.assertNotNull( list );
        int initialRealmCount = list.size();
        
        String realmId = "realm-it-crudRealmPrivs";

        // now create should fail
        S2SecurityRealm realm = new S2SecurityRealm();
        realm.setId( realmId );
        realm.setName( "realm-it name" );
        realm.setDescription( "realm-it description" );
        Response r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertEquals( 403, r.getStatus().getCode() );
        
        giveUserPrivilege( TEST_USER_NAME, "onboarding-security-realm-create-update" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        // now create should pass
        r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error:" + r.getStatus() );
        list = SecurityRealmMessageUtil.listRealms();
        Assert.assertEquals( initialRealmCount + 1, list.size() );
        
        resetTestUserPrivs();
        giveUserPrivilege( TEST_USER_NAME, "onboarding-security-realm-read" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // now update should fail
        realm = SecurityRealmMessageUtil.getRealm( realmId );
        realm.setName( "realm-it name update" );
        r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertEquals( 403, r.getStatus().getCode() );

        giveUserPrivilege( TEST_USER_NAME, "onboarding-security-realm-create-update" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        // now update should pass
        realm.setName( "realm-it name update" );
        r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error:" + r.getStatus() );
        
        // now delete should fail
        r = SecurityRealmMessageUtil.deleteRealm( realmId );
        Assert.assertEquals( 403, r.getStatus().getCode() );
        
        giveUserPrivilege( TEST_USER_NAME, "onboarding-security-realm-delete" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        // now delete should pass
        r = SecurityRealmMessageUtil.deleteRealm( realmId );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error:" + r.getStatus() );

        list = SecurityRealmMessageUtil.listRealms();
        Assert.assertEquals( initialRealmCount, list.size() );
    }

    @Test
    public void crudURLPrivs()
        throws Exception
    {
        // first create a realm
        TestContainer.getInstance().getTestContext().useAdminForRequests();

        String realmId = "realm-it-crudURLPrivs";
        S2SecurityRealm realm = new S2SecurityRealm();
        realm.setId( realmId );
        realm.setName( "realm-it name" );
        realm.setDescription( "realm-it description" );
        Response r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error creating realm:" + r.getStatus() );

        resetTestUserPrivs();
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // now create should fail
        S2SecurityRealmURLAssoc urlAssoc = new S2SecurityRealmURLAssoc();
        urlAssoc.setId( "url-it" );
        urlAssoc.setRealmId( realm.getId() );
        urlAssoc.setUrl( "http://foo" );
        r = SecurityRealmURLAssocMessageUtil.createURL( urlAssoc );
        Assert.assertEquals( 403, r.getStatus().getCode() );

        giveUserPrivilege( TEST_USER_NAME, "onboarding-security-realm-url-create" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // create should pass
        r = SecurityRealmURLAssocMessageUtil.createURL( urlAssoc );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error:" + r.getStatus() );
        giveUserPrivilege( TEST_USER_NAME, "onboarding-security-realm-url-read" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        urlAssoc = SecurityRealmURLAssocMessageUtil.getURL( r );
        Assert.assertNotNull( urlAssoc );
        final String urlId = urlAssoc.getId();
        Assert.assertNotNull( urlId );

        resetTestUserPrivs();
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // update should fail
        urlAssoc.setUrl( "http://foo/update" );
        r = SecurityRealmURLAssocMessageUtil.updateURL( urlAssoc );
        Assert.assertEquals( 403, r.getStatus().getCode() );

        giveUserPrivilege( TEST_USER_NAME, "onboarding-security-realm-url-update" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // update should pass
        r = SecurityRealmURLAssocMessageUtil.updateURL( urlAssoc );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error:" + r.getStatus() );
        giveUserPrivilege( TEST_USER_NAME, "onboarding-security-realm-url-read" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        urlAssoc = SecurityRealmURLAssocMessageUtil.getURL( urlId );
        Assert.assertNotNull( urlAssoc );
        Assert.assertEquals( urlId, urlAssoc.getId() );
        Assert.assertEquals( realm.getId(), urlAssoc.getRealmId() );
        Assert.assertEquals( "http://foo/update", urlAssoc.getUrl() );

        resetTestUserPrivs();
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // read should fail
        urlAssoc = SecurityRealmURLAssocMessageUtil.getURL( urlId, false /* validate */);
        Assert.assertNull( urlAssoc );

        resetTestUserPrivs();
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // delete should fail
        r = SecurityRealmURLAssocMessageUtil.deleteURL( urlId );
        Assert.assertEquals( 403, r.getStatus().getCode() );

        giveUserPrivilege( TEST_USER_NAME, "onboarding-security-realm-url-delete" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        r = SecurityRealmURLAssocMessageUtil.deleteURL( urlId );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error:" + r.getStatus() );

        TestContainer.getInstance().getTestContext().useAdminForRequests();
        List<S2SecurityRealmURLAssoc> list = SecurityRealmURLAssocMessageUtil.listUrls();
        Assert.assertEquals( 0, list.size() );
    }

    @Test
    public void crudRealmRoles()
        throws Exception
    {
        resetTestUserPrivs();
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // should fail no perms
        List<S2SecurityRealm> list = SecurityRealmMessageUtil.listRealms( false );
        Assert.assertNull( list );

        giveUserRole( TEST_USER_NAME, "onboarding-security-realm-developer" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // should pass now has perm
        list = SecurityRealmMessageUtil.listRealms();
        Assert.assertNotNull( list );
        int initialRealmCount = list.size();

        resetTestUserPrivs();
        giveUserRole( TEST_USER_NAME, "onboarding-security-realm-administrator" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        list = SecurityRealmMessageUtil.listRealms();
        Assert.assertNotNull( list );

        String realmId = "realm-it-crudRealmRoles";

        resetTestUserPrivs();
        giveUserRole( TEST_USER_NAME, "onboarding-security-realm-developer" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        // now create should fail
        S2SecurityRealm realm = new S2SecurityRealm();
        realm.setId( realmId );
        realm.setName( "realm-it name" );
        realm.setDescription( "realm-it description" );
        Response r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertEquals( 403, r.getStatus().getCode() );

        giveUserRole( TEST_USER_NAME, "onboarding-security-realm-developer" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        // should still fail
        r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertEquals( 403, r.getStatus().getCode() );

        giveUserRole( TEST_USER_NAME, "onboarding-security-realm-administrator" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // now create should pass
        r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error:" + r.getStatus() );
        list = SecurityRealmMessageUtil.listRealms();
        Assert.assertEquals( initialRealmCount + 1, list.size() );

        resetTestUserPrivs();
        giveUserRole( TEST_USER_NAME, "onboarding-security-realm-developer" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // now update should fail
        realm = SecurityRealmMessageUtil.getRealm( realmId );
        realm.setName( "realm-it name update" );
        r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertEquals( 403, r.getStatus().getCode() );

        giveUserRole( TEST_USER_NAME, "onboarding-security-realm-administrator" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // now update should pass
        realm.setName( "realm-it name update" );
        r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error:" + r.getStatus() );

        resetTestUserPrivs();
        giveUserRole( TEST_USER_NAME, "onboarding-security-realm-developer" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // now delete should fail
        r = SecurityRealmMessageUtil.deleteRealm( realmId );
        Assert.assertEquals( 403, r.getStatus().getCode() );

        giveUserRole( TEST_USER_NAME, "onboarding-security-realm-administrator" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // now delete should pass
        r = SecurityRealmMessageUtil.deleteRealm( realmId );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error:" + r.getStatus() );

        list = SecurityRealmMessageUtil.listRealms();
        Assert.assertEquals( initialRealmCount, list.size() );
    }

    @Test
    public void crudURLRoles()
        throws Exception
    {
        // first create a realm
        TestContainer.getInstance().getTestContext().useAdminForRequests();

        String realmId = "realm-it-crudURLRoles";
        S2SecurityRealm realm = new S2SecurityRealm();
        realm.setId( realmId );
        realm.setName( "realm-it name" );
        realm.setDescription( "realm-it description" );
        Response r = SecurityRealmMessageUtil.createOrUpdateRealm( realm );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error creating realm:" + r.getStatus() );

        resetTestUserPrivs();
        giveUserRole( TEST_USER_NAME, "onboarding-security-realm-developer" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // now create should fail
        S2SecurityRealmURLAssoc urlAssoc = new S2SecurityRealmURLAssoc();
        urlAssoc.setId( "url-it" );
        urlAssoc.setRealmId( realm.getId() );
        urlAssoc.setUrl( "http://foo" );
        r = SecurityRealmURLAssocMessageUtil.createURL( urlAssoc );
        Assert.assertEquals( 403, r.getStatus().getCode() );

        giveUserRole( TEST_USER_NAME, "onboarding-security-realm-administrator" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // create should pass
        r = SecurityRealmURLAssocMessageUtil.createURL( urlAssoc );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error:" + r.getStatus() );
        urlAssoc = SecurityRealmURLAssocMessageUtil.getURL( r );
        Assert.assertNotNull( urlAssoc );
        final String urlId = urlAssoc.getId();
        Assert.assertNotNull( urlId );

        resetTestUserPrivs();
        giveUserRole( TEST_USER_NAME, "onboarding-security-realm-developer" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // update should fail
        urlAssoc.setUrl( "http://foo/update" );
        r = SecurityRealmURLAssocMessageUtil.updateURL( urlAssoc );
        Assert.assertEquals( 403, r.getStatus().getCode() );

        giveUserRole( TEST_USER_NAME, "onboarding-security-realm-administrator" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // update should pass
        r = SecurityRealmURLAssocMessageUtil.updateURL( urlAssoc );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error:" + r.getStatus() );
        urlAssoc = SecurityRealmURLAssocMessageUtil.getURL( urlId );
        Assert.assertNotNull( urlAssoc );
        Assert.assertEquals( urlId, urlAssoc.getId() );
        Assert.assertEquals( realm.getId(), urlAssoc.getRealmId() );
        Assert.assertEquals( "http://foo/update", urlAssoc.getUrl() );

        resetTestUserPrivs();
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // read should fail
        urlAssoc = SecurityRealmURLAssocMessageUtil.getURL( urlId, false /* validate */);
        Assert.assertNull( urlAssoc );

        resetTestUserPrivs();
        giveUserRole( TEST_USER_NAME, "onboarding-security-realm-developer" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // delete should fail
        r = SecurityRealmURLAssocMessageUtil.deleteURL( urlId );
        Assert.assertEquals( 403, r.getStatus().getCode() );

        giveUserRole( TEST_USER_NAME, "onboarding-security-realm-administrator" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        r = SecurityRealmURLAssocMessageUtil.deleteURL( urlId );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error:" + r.getStatus() );

        TestContainer.getInstance().getTestContext().useAdminForRequests();
        List<S2SecurityRealmURLAssoc> list = SecurityRealmURLAssocMessageUtil.listUrls();
        Assert.assertEquals( 0, list.size() );
    }

    @Override
    public void resetTestUserPrivs()
        throws Exception
    {
        super.resetTestUserPrivs();

        UserResource testUser = userUtil.getUser( TEST_USER_NAME );
        testUser.removeRole( "anonymous" );

        // Give user a role because nexus does not allow users without roles
        testUser.addRole( "repository-any-read" );
        userUtil.updateUser( testUser );
    }
}
