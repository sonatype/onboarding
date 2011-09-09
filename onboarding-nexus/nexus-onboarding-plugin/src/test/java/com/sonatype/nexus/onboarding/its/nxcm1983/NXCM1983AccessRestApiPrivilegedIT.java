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
package com.sonatype.nexus.onboarding.its.nxcm1983;

import java.util.List;

import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sonatype.nexus.onboarding.its.AbstractOnboardingPrivilegeIT;
import com.sonatype.nexus.onboarding.its.util.CatalogMessageUtil;
import com.sonatype.nexus.onboarding.rest.dto.CatalogDTO;
import com.sonatype.nexus.onboarding.rest.dto.CatalogEntryDTO;

public class NXCM1983AccessRestApiPrivilegedIT
    extends AbstractOnboardingPrivilegeIT
{

    @Test
    public void crudPrivs()
        throws Exception
    {
        resetTestUserPrivs();
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        // should fail no perms
        List<CatalogDTO> list = CatalogMessageUtil.list( false );
        Assert.assertNull( list );
        
        giveUserPrivilege( TEST_USER_NAME, "catalog-read" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        // should pass now has perm
        list = CatalogMessageUtil.list();
        Assert.assertNotNull( list );
        Assert.assertEquals( 0, list.size() );
        
        // now create should fail
        CatalogDTO catalog = new CatalogDTO();
        catalog.setId( "catalog-it" );
        catalog.setName( "catalog-it" );
        catalog.setRealm( "realm-it" );
        Response r = CatalogMessageUtil.create( catalog );
        Assert.assertEquals( 403, r.getStatus().getCode() );
        
        giveUserPrivilege( TEST_USER_NAME, "catalog-create-update" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        // now create should pass
        r = CatalogMessageUtil.create( catalog );
        Assert.assertTrue( r.getStatus().isSuccess() );
        list = CatalogMessageUtil.list();
        Assert.assertEquals( 1, list.size() );
        String catalogId = CatalogMessageUtil.getCatalog( r ).getId();
        
        resetTestUserPrivs();
        giveUserPrivilege( TEST_USER_NAME, "catalog-read" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        // now update should fail
        catalog = CatalogMessageUtil.getCatalog( catalogId );
        catalog.setName( "catalog-it-update" );
        catalog.setRealm( "realm-it-update" );
        r = CatalogMessageUtil.updateCatalog( catalog );
        Assert.assertEquals( 403, r.getStatus().getCode() );
        
        giveUserPrivilege( TEST_USER_NAME, "catalog-create-update" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        // now update should pass
        r = CatalogMessageUtil.updateCatalog( catalog );
        Assert.assertTrue( r.getStatus().isSuccess() );
        Assert.assertNotNull( catalog );
        Assert.assertEquals( catalogId, catalog.getId() );
        Assert.assertEquals( "catalog-it-update", catalog.getName() );
        Assert.assertEquals( "realm-it-update", catalog.getRealm() );
        
        // now delete should fail
        r = CatalogMessageUtil.delete( catalogId );
        Assert.assertEquals( 403, r.getStatus().getCode() );
        
        giveUserPrivilege( TEST_USER_NAME, "catalog-delete" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        r = CatalogMessageUtil.delete( catalogId );
        Assert.assertTrue( r.getStatus().isSuccess() );

        list = CatalogMessageUtil.list();
        Assert.assertEquals( 0, list.size() );
    }

    @Test
    public void entryCrudPerms()
        throws Exception
    {
        // first create the catalog
        TestContainer.getInstance().getTestContext().useAdminForRequests();
        
        CatalogDTO catalog = new CatalogDTO();
        catalog.setId( "catalog-it" );
        catalog.setName( "catalog-it" );
        catalog.setRealm( "realm-it" );
        Response r = CatalogMessageUtil.create( catalog );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error creating catalog:\n" + r.getStatus() );

        String catalogId = CatalogMessageUtil.getCatalog( r ).getId();
        Assert.assertNotNull( catalogId );
        
        resetTestUserPrivs();
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        // now for the entry perm tests
        // entry create should fail
        CatalogEntryDTO entry = new CatalogEntryDTO();
        entry.setName( "entry-name" );
        entry.setRealm( "realm-it" );
        entry.setType( "project" );
        entry.setUrl( "http://www.sonatype.org/" );
        r = CatalogMessageUtil.addEntryCatalog( catalogId, entry );
        Assert.assertEquals( 403, r.getStatus().getCode() );
        
        // now add the create/update perms
        giveUserPrivilege( TEST_USER_NAME, "catalog-create-update" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        // create should pass
        r = CatalogMessageUtil.addEntryCatalog( catalogId, entry );
        Assert.assertTrue( r.getStatus().isSuccess() );
        entry = CatalogMessageUtil.getEntry( r );
        Assert.assertNotNull( entry );
        final String entryId = entry.getId();
        Assert.assertNotNull( entryId );

        TestContainer.getInstance().getTestContext().useAdminForRequests();
        catalog = CatalogMessageUtil.getCatalog( catalogId );
        Assert.assertEquals( 1, catalog.getEntries().size() );
        Assert.assertEquals( "entry-name", entry.getName() );
        Assert.assertEquals( "realm-it", entry.getRealm() );
        Assert.assertEquals( "project", entry.getType() );
        Assert.assertEquals( "http://www.sonatype.org/", entry.getUrl() );

        entry.setName( "update-entry-name" );
        entry.setRealm( "update-realm-it" );
        entry.setType( "p2lineup" );
        entry.setUrl( "http://www.sonatype.org/update" );
        
        resetTestUserPrivs();
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        //update should fail
        r = CatalogMessageUtil.updateCatalogEntry( catalogId, entry );
        Assert.assertEquals( 403, r.getStatus().getCode() );
        
        giveUserPrivilege( TEST_USER_NAME, "catalog-create-update" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        //update should pass
        r = CatalogMessageUtil.updateCatalogEntry( catalogId, entry );
        Assert.assertTrue( r.getStatus().isSuccess() );
        entry = CatalogMessageUtil.getEntry( r );
        Assert.assertNotNull( entry );
        Assert.assertEquals( entry.getId(), entryId );
        Assert.assertEquals( entry.getName(), "update-entry-name" );
        Assert.assertEquals( entry.getRealm(), "update-realm-it" );
        Assert.assertEquals( entry.getType(), "p2lineup" );
        Assert.assertEquals( entry.getUrl(), "http://www.sonatype.org/update" );
        
        resetTestUserPrivs();
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // read should fail
        entry = CatalogMessageUtil.getEntry( catalogId, entryId, false );
        Assert.assertNull( entry );
        
        giveUserPrivilege( TEST_USER_NAME, "catalog-read" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        // read should pass
        entry = CatalogMessageUtil.getEntry( catalogId, entryId, false );
        Assert.assertNotNull( entry );
        Assert.assertEquals( entry.getId(), entryId );
        Assert.assertEquals( entry.getName(), "update-entry-name" );
        Assert.assertEquals( entry.getRealm(), "update-realm-it" );
        Assert.assertEquals( entry.getType(), "p2lineup" );
        Assert.assertEquals( entry.getUrl(), "http://www.sonatype.org/update" );
        
        resetTestUserPrivs();
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        //delete should fail
        r = CatalogMessageUtil.deleteEntry( catalogId, entryId );
        Assert.assertEquals( 403, r.getStatus().getCode() );
        
        giveUserPrivilege( TEST_USER_NAME, "catalog-delete" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        
        r = CatalogMessageUtil.deleteEntry( catalogId, entryId );
        Assert.assertTrue( r.getStatus().isSuccess() );

        TestContainer.getInstance().getTestContext().useAdminForRequests();
        catalog = CatalogMessageUtil.getCatalog( catalogId );
        Assert.assertEquals( 0, catalog.getEntries().size() );
    }
}
