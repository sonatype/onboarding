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

import org.junit.Assert;
import org.junit.Test;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.nexus.AbstractNexusTestCase;

import com.sonatype.nexus.onboarding.persist.model.CCatalog;
import com.sonatype.nexus.onboarding.persist.model.CCatalogEntry;

public class DefaultCatalogConfigurationTest
    extends AbstractNexusTestCase
{

    private CatalogConfiguration cfg;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        this.cfg = lookup( CatalogConfiguration.class );
    }

    @Test
    public void testCRUD()
        throws Exception
    {
        CCatalog c = new CCatalog();
        c.setId( "testCatalog" );
        c.setName( "test-catalog" );
        cfg.createCatalog( c );
        assertNotNull( c.getId() );

        c = cfg.readCatalog( c.getId() );
        assertNotNull( c );
        assertEquals( "test-catalog", c.getName() );

        c.setName( "test-update-catalog" );
        cfg.createOrUpdateCatalog( c );

        c = cfg.readCatalog( c.getId() );
        assertEquals( "test-update-catalog", c.getName() );

        cfg.deleteCatalog( c.getId() );
    }

    @Test
    public void testRequireFields()
        throws InvalidConfigurationException
    {
        CCatalog c = new CCatalog();
        c.setName( "test-catalog" );

        try
        {
            cfg.createCatalog( c );
            Assert.fail();
        }
        catch ( InvalidConfigurationException e )
        {
            // expected
        }

        c = new CCatalog();
        c.setId( "testCatalog" );

        try
        {
            cfg.createCatalog( c );
            Assert.fail();
        }
        catch ( InvalidConfigurationException e )
        {
            // expected
        }

        c = new CCatalog();

        CCatalogEntry e1 = new CCatalogEntry();
        e1.setId( "e1" );
        c.addEntry( e1 );

        CCatalogEntry e2 = new CCatalogEntry();
        e2.setName( "e2" );
        c.addEntry( e2 );

        try
        {
            cfg.createOrUpdateCatalog( c );
            Assert.fail();
        }
        catch ( InvalidConfigurationException e )
        {
            Assert.assertEquals( 4, e.getValidationResponse().getValidationErrors().size() );
        }
    }

    @Test
    public void testInvalidDelete()
    {
        try
        {
            cfg.deleteCatalog( "AAbbCC" );
            Assert.fail();
        }
        catch ( InvalidConfigurationException e )
        {
            // expected
        }
    }

    @Test
    public void testInvalidRemoveEntry()
        throws InvalidConfigurationException
    {
        CCatalog c = new CCatalog();
        c.setId( "testCatalog" );
        c.setName( "test-catalog" );
        cfg.createCatalog( c );

        try
        {
            cfg.removeCatalogEntry( c.getId(), "aaBBcc" );
            Assert.fail();
        }
        catch ( InvalidConfigurationException e )
        {
            // expected
        }
        finally
        {
            cfg.deleteCatalog( c.getId() );
        }
    }

    @Test
    public void testBrokenUpdate()
        throws InvalidConfigurationException
    {
        CCatalog c = new CCatalog();
        c.setId( "testCatalog" );
        c.setName( "test-catalog" );
        cfg.createCatalog( c );

        c.setName( null );

        try
        {
            cfg.createOrUpdateCatalog( c );
            Assert.fail();
        }
        catch ( InvalidConfigurationException e )
        {
            // expected
        }
        finally
        {
            cfg.deleteCatalog( "testCatalog" );
        }
    }

    @Test
    public void testMultipleCatalogs()
        throws InvalidConfigurationException
    {
        CCatalog c1 = new CCatalog();
        c1.setId( "1" );
        c1.setName( "test-catalog" + c1.getId() );
        cfg.createCatalog( c1 );

        CCatalog c2 = new CCatalog();
        c2.setId( "2" );
        c2.setName( "test-catalog" + c2.getId() );
        cfg.createCatalog( c2 );

        CCatalog c3 = new CCatalog();
        c3.setId( "3" );
        c3.setName( "test-catalog" + c3.getId() );
        cfg.createCatalog( c3 );

        CCatalogEntry e1 = new CCatalogEntry();
        e1.setId( "e1" );
        e1.setName( "name-" + e1.getId() );
        cfg.addCatalogEntry( c3.getId(), e1 );

        try
        {
            cfg.deleteCatalog( "8" );
            Assert.fail();
        }
        catch ( InvalidConfigurationException e )
        {
            // expected
        }

        Assert.assertEquals( c2, cfg.readCatalog( "2" ) );
        cfg.deleteCatalog( "2" );
        try
        {
            cfg.readCatalog( "2" );
            Assert.fail();
        }
        catch ( InvalidConfigurationException e )
        {
            // expected
        }
        try
        {
            cfg.readCatalogEntry( "2", null );
            Assert.fail();
        }
        catch ( InvalidConfigurationException e )
        {
            // expected
        }

        Assert.assertEquals( c1, cfg.readCatalog( "1" ) );
        cfg.deleteCatalog( "1" );


        try
        {
            cfg.readCatalogEntry( "3", "a" );
            Assert.fail();
        }
        catch ( InvalidConfigurationException e )
        {
            // expected
        }
        finally
        {
            cfg.deleteCatalog( "3" );
        }
    }
}
