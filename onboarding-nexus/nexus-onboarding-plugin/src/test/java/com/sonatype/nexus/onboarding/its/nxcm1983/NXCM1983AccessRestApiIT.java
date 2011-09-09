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

import java.io.File;
import java.net.URL;
import java.util.List;

import org.restlet.data.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sonatype.nexus.onboarding.its.AbstractOnboardingIT;
import com.sonatype.nexus.onboarding.its.util.CatalogMessageUtil;
import com.sonatype.nexus.onboarding.rest.dto.CatalogDTO;
import com.sonatype.nexus.onboarding.rest.dto.CatalogEntryDTO;

public class NXCM1983AccessRestApiIT
    extends AbstractOnboardingIT
{
    @BeforeMethod
    public void cleanEnv()
        throws Exception
    {
        for ( CatalogDTO dto : CatalogMessageUtil.list() )
        {
            CatalogMessageUtil.delete( dto.getId() );
        }
    }

    @Test
    public void crud()
        throws Exception
    {
        CatalogDTO catalog = new CatalogDTO();
        catalog.setId( "catalog-it" );
        catalog.setName( "catalog-it" );
        catalog.setRealm( "realm-it" );
        Response r = CatalogMessageUtil.create( catalog );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error creating catalog:\n" + r.getStatus() );

        String catalogId = CatalogMessageUtil.getCatalog( r ).getId();
        Assert.assertNotNull( catalogId );

        List<CatalogDTO> list = CatalogMessageUtil.list();
        Assert.assertEquals( 1, list.size() );

        catalog = CatalogMessageUtil.getCatalog( catalogId );
        Assert.assertNotNull( catalog );
        Assert.assertEquals( catalogId, catalog.getId() );
        Assert.assertEquals( "catalog-it", catalog.getName() );
        Assert.assertEquals( "realm-it", catalog.getRealm() );

        catalog.setName( "catalog-it-update" );
        catalog.setRealm( "realm-it-update" );

        r = CatalogMessageUtil.updateCatalog( catalog );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error updating catalog:\n" + r.getStatus() );
        catalog = CatalogMessageUtil.getCatalog( r );
        Assert.assertNotNull( catalog );
        Assert.assertEquals( catalogId, catalog.getId() );
        Assert.assertEquals( "catalog-it-update", catalog.getName() );
        Assert.assertEquals( "realm-it-update", catalog.getRealm() );

        catalog = CatalogMessageUtil.getCatalog( catalogId );
        Assert.assertNotNull( catalog );
        Assert.assertEquals( catalogId, catalog.getId() );
        Assert.assertEquals( "catalog-it-update", catalog.getName() );
        Assert.assertEquals( "realm-it-update", catalog.getRealm() );

        r = CatalogMessageUtil.delete( catalogId );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error deleting catalog:\n" + r.getStatus() );
        Assert.assertEquals( 204, r.getStatus().getCode() );

        list = CatalogMessageUtil.list();
        Assert.assertEquals( 0, list.size() );
    }

    @Test
    public void entryCrud()
        throws Exception
    {
        CatalogDTO catalog = new CatalogDTO();
        catalog.setId( "catalog-it" );
        catalog.setName( "catalog-it" );
        catalog.setRealm( "realm-it" );
        Response r = CatalogMessageUtil.create( catalog );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error creating catalog:\n" + r.getStatus() );

        String catalogId = CatalogMessageUtil.getCatalog( r ).getId();
        Assert.assertNotNull( catalogId );

        CatalogEntryDTO entry = new CatalogEntryDTO();
        entry.setName( "entry-name" );
        entry.setRealm( "realm-it" );
        entry.setType( "project" );
        entry.setUrl( "http://www.sonatype.org/" );

        r = CatalogMessageUtil.addEntryCatalog( catalogId, entry );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error creating entry catalog:\n" + r.getStatus() );

        entry = CatalogMessageUtil.getEntry( r );
        Assert.assertNotNull( entry );

        final String entryId = entry.getId();
        Assert.assertNotNull( entryId );

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

        r = CatalogMessageUtil.updateCatalogEntry( catalogId, entry );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error updating entry catalog:\n" + r.getStatus() );

        entry = CatalogMessageUtil.getEntry( r );
        Assert.assertNotNull( entry );

        Assert.assertEquals( entry.getId(), entryId );
        Assert.assertEquals( entry.getName(), "update-entry-name" );
        Assert.assertEquals( entry.getRealm(), "update-realm-it" );
        Assert.assertEquals( entry.getType(), "p2lineup" );
        Assert.assertEquals( entry.getUrl(), "http://www.sonatype.org/update" );

        entry = CatalogMessageUtil.getEntry( catalogId, entryId );
        Assert.assertNotNull( entry );

        Assert.assertEquals( entry.getId(), entryId );
        Assert.assertEquals( entry.getName(), "update-entry-name" );
        Assert.assertEquals( entry.getRealm(), "update-realm-it" );
        Assert.assertEquals( entry.getType(), "p2lineup" );
        Assert.assertEquals( entry.getUrl(), "http://www.sonatype.org/update" );

        r = CatalogMessageUtil.deleteEntry( catalogId, entryId );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error deleting entry catalog:\n" + r.getStatus() );

        catalog = CatalogMessageUtil.getCatalog( catalogId );
        Assert.assertEquals( 0, catalog.getEntries().size() );
    }

    @Test
    public void jnlp()
        throws Exception
    {
        CatalogDTO catalog = new CatalogDTO();
        catalog.setId( "catalog-it" );
        catalog.setName( "catalog-it" );
        catalog.setRealm( "realm-it" );
        Response r = CatalogMessageUtil.create( catalog );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error creating catalog:\n" + r.getStatus() );

        String catalogId = CatalogMessageUtil.getCatalog( r ).getId();
        Assert.assertNotNull( catalogId );

        CatalogEntryDTO entry = new CatalogEntryDTO();
        entry.setName( "entry-name" );
        entry.setRealm( "realm-it" );
        entry.setType( "project" );
        entry.setUrl( "http://www.sonatype.org/" );

        r = CatalogMessageUtil.addEntryCatalog( catalogId, entry );
        Assert.assertTrue( r.getStatus().isSuccess(), "Error creating entry catalog:\n" + r.getStatus() );

        entry = CatalogMessageUtil.getEntry( r );
        Assert.assertNotNull( entry );

        File jnlp =
            downloadFile( new URL( nexusBaseUrl + CatalogMessageUtil.URI + "/" + catalogId + "/catalog.jnlp" ),
                "target/nxcm1983/catalog.jnlp" );

        Assert.assertTrue( jnlp.exists() );
    }
}
