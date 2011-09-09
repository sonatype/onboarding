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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

import com.sonatype.s2.project.core.IS2ProjectCatalogRegistryEntry;
import com.sonatype.s2.project.core.IS2ProjectCatalogRegistryListener;
import com.sonatype.s2.project.core.internal.S2ProjectCatalogRegistry;
import com.sonatype.s2.project.model.IS2ProjectCatalog;
import com.sonatype.s2.project.model.IS2ProjectCatalogEntry;
import com.sonatype.s2.project.model.catalog.ProjectCatalogEntry;

public class S2ProjectCatalogRegistryTest
    extends AbstractMavenProjectTestCase
{
    public void testBasic()
        throws Exception
    {
        String urlStr = new File( "resources/" ).getCanonicalFile().toURL().toExternalForm();

        File basedir = new File( "target" );
        File registryFile = new File( basedir, S2ProjectCatalogRegistry.CATALOG_REGISTRY_FILENAME );

        final ArrayList<IS2ProjectCatalog> addedCatalogs = new ArrayList<IS2ProjectCatalog>();
        final ArrayList<IS2ProjectCatalog> removedCatalogs = new ArrayList<IS2ProjectCatalog>();

        IS2ProjectCatalogRegistryListener listener = new IS2ProjectCatalogRegistryListener()
        {
            public void catalogRemoved( IS2ProjectCatalogRegistryEntry catalog )
            {
                removedCatalogs.add( catalog.getCatalog() );
            }

            public void catalogAdded( IS2ProjectCatalogRegistryEntry catalog )
            {
                addedCatalogs.add( catalog.getCatalog() );
            }
        };

        registryFile.delete();

        // new empty registry
        S2ProjectCatalogRegistry registry = new S2ProjectCatalogRegistry( basedir );
        registry.addListener( listener );
        assertTrue( registry.getCatalogs( monitor ).isEmpty() );

        // add catalog
        registry.addCatalog( urlStr );
        waitForJobsToComplete();
        assertEquals( 1, addedCatalogs.size() );
        assertEquals( 1, registry.getCatalogs( monitor ).size() );

        // read existing non-empty registry
        registry = new S2ProjectCatalogRegistry( basedir );
        registry.addListener( listener );
        assertEquals( 1, registry.getCatalogs( monitor ).size() );

        // remove
        registry.removeCatalog( urlStr );
        waitForJobsToComplete();
        assertEquals( 1, removedCatalogs.size() );
        assertTrue( registry.getCatalogs( monitor ).isEmpty() );

        // read existing empty registry
        assertTrue( registryFile.canRead() );
        registry = new S2ProjectCatalogRegistry( basedir );
        assertTrue( registry.getCatalogs( monitor ).isEmpty() );
    }

    public void testCatalogUrlAlreadyEndsWithCatalogXml()
        throws Exception
    {
        String urlStr = new File( "resources/" ).getCanonicalFile().toURL().toExternalForm();

        File basedir = new File( "target" );
        File registryFile = new File( basedir, S2ProjectCatalogRegistry.CATALOG_REGISTRY_FILENAME );
        registryFile.delete();

        final ArrayList<IS2ProjectCatalogRegistryEntry> added = new ArrayList<IS2ProjectCatalogRegistryEntry>();
        final ArrayList<IS2ProjectCatalogRegistryEntry> removed = new ArrayList<IS2ProjectCatalogRegistryEntry>();

        IS2ProjectCatalogRegistryListener listener = new IS2ProjectCatalogRegistryListener()
        {
            public void catalogAdded( IS2ProjectCatalogRegistryEntry catalog )
            {
                added.add( catalog );
            }

            public void catalogRemoved( IS2ProjectCatalogRegistryEntry catalog )
            {
                removed.add( catalog );
            }
        };

        S2ProjectCatalogRegistry registry = new S2ProjectCatalogRegistry( basedir );
        registry.addListener( listener );
        assertTrue( registry.getCatalogs( monitor ).isEmpty() );

        // add catalog when its URL already ends with "catalog.xml"
        registry.addCatalog( urlStr + "catalog.xml" );
        waitForJobsToComplete();
        assertEquals( 1, added.size() );
        assertEquals( added.get( 0 ).getUrl(), added.get( 0 ).getCatalog().getUrl() );
        assertEquals( 1, registry.getCatalogs( monitor ).size() );

        // verify relative PMD URLs are properly resolved
        for ( IS2ProjectCatalogEntry entry : added.get( 0 ).getCatalog().getEntries() )
        {
            String url = registry.getEffectiveDescriptorUrl( entry );
            assertEquals( urlStr + "descriptor.xml", url );
            break;
        }

        // and remove (i.e. check the registry recognizes the URL)
        registry.removeCatalog( added.get( 0 ).getUrl() );
        waitForJobsToComplete();
        assertEquals( 1, removed.size() );
        assertTrue( registry.getCatalogs( monitor ).isEmpty() );
    }

    public void testEffectiveDescriptorUrl()
        throws CoreException
    {
        S2ProjectCatalogRegistry registry = new S2ProjectCatalogRegistry( new File( "target" ) );

        ProjectCatalogEntry entry = new ProjectCatalogEntry();
        entry.setCatalogUrl( "http://foo/bar" );

        entry.setDescriptorUrl( "./munchy.xml" );
        assertEquals( "http://foo/bar/munchy.xml", registry.getEffectiveDescriptorUrl( entry ) );

        entry.setDescriptorUrl( "http://foo/munchy.xml" );
        assertEquals( "http://foo/munchy.xml", registry.getEffectiveDescriptorUrl( entry ) );
    }

    public void testNoDefaultCatalogs()
        throws Exception
    {
        System.clearProperty( S2ProjectCatalogRegistry.DEFAULT_CATALOGS_PROP );
        S2ProjectCatalogRegistry registry = new S2ProjectCatalogRegistry( new File( "target/testNoDefaultCatalogs" ) );
        assertFalse(registry.hasDefaultCatalogs());
        List<IS2ProjectCatalog> catalogs = registry.getCatalogs( monitor );
        assertNotNull( catalogs );
        assertEquals( 0, catalogs.size() );
    }

    public void testOneDefaultCatalog()
        throws Exception
    {
        String catalogUrl = new File( "resources/default-catalogs/1" ).toURI().toURL().toString();
        System.setProperty( S2ProjectCatalogRegistry.DEFAULT_CATALOGS_PROP, catalogUrl );

        File baseDir = new File( "target/testOneDefaultCatalog" );
        baseDir.mkdirs();
        S2ProjectCatalogRegistry registry = new S2ProjectCatalogRegistry( baseDir );
        waitForJobsToComplete();
        assertTrue(registry.hasDefaultCatalogs());
        List<IS2ProjectCatalog> catalogs = registry.getCatalogs( monitor );
        assertNotNull(catalogs);
        assertEquals( 1, catalogs.size() );
        assertEquals( "Foo", catalogs.iterator().next().getName() );

        // Remove the default catalog
        registry.removeCatalog( catalogUrl, new NullProgressMonitor() );
        catalogs = registry.getCatalogs( monitor );
        assertNotNull( catalogs );
        assertEquals( 0, catalogs.size() );

        // Restore the default catalog
        registry.addDefaultCatalogs();
        waitForJobsToComplete();
        catalogs = registry.getCatalogs( monitor );
        assertNotNull( catalogs );
        assertEquals( 1, catalogs.size() );
        assertEquals( "Foo", catalogs.iterator().next().getName() );
    }

    public void testTwoDefaultCatalogs()
        throws Exception
    {
        String catalogUrl1 = new File( "resources/default-catalogs/1" ).toURI().toURL().toString();
        String catalogUrl2 = new File( "resources/default-catalogs/2" ).toURI().toURL().toString();
        System.setProperty( S2ProjectCatalogRegistry.DEFAULT_CATALOGS_PROP, catalogUrl1 + "|" + catalogUrl2 );

        File baseDir = new File( "target/testTwoDefaultCatalogs" );
        baseDir.mkdirs();
        S2ProjectCatalogRegistry registry = new S2ProjectCatalogRegistry( baseDir );
        waitForJobsToComplete();
        assertTrue(registry.hasDefaultCatalogs());
        List<IS2ProjectCatalog> catalogs = registry.getCatalogs( monitor );
        assertNotNull( catalogs );
        assertEquals( 2, catalogs.size() );
        Iterator<IS2ProjectCatalog> catalogsIter = catalogs.iterator();
        assertEquals( "Foo", catalogsIter.next().getName() );
        assertEquals( "Foo1", catalogsIter.next().getName() );

        // Remove the default catalogs
        registry.removeCatalog( catalogUrl1, new NullProgressMonitor() );
        registry.removeCatalog( catalogUrl2, new NullProgressMonitor() );
        catalogs = registry.getCatalogs( monitor );
        assertNotNull( catalogs );
        assertEquals( 0, catalogs.size() );

        // Restore the default catalogs
        registry.addDefaultCatalogs();
        waitForJobsToComplete();
        catalogs = registry.getCatalogs( monitor );
        assertNotNull( catalogs );
        assertEquals( 2, catalogs.size() );
        catalogsIter = catalogs.iterator();
        assertEquals( "Foo", catalogsIter.next().getName() );
        assertEquals( "Foo1", catalogsIter.next().getName() );
    }

    public void testDefaultCatalogEndingWithCatalogXml()
        throws Exception
    {
        String catalogUrl = new File( "resources/default-catalogs/1/catalog.xml" ).toURI().toURL().toString();
        System.setProperty( S2ProjectCatalogRegistry.DEFAULT_CATALOGS_PROP, catalogUrl );

        File baseDir = new File( "target/default-catalog-xml" );
        baseDir.mkdirs();
        S2ProjectCatalogRegistry registry = new S2ProjectCatalogRegistry( baseDir );
        assertTrue(registry.hasDefaultCatalogs());
        List<IS2ProjectCatalog> catalogs = registry.getCatalogs( monitor );
        assertNotNull( catalogs );
        assertEquals( 1, catalogs.size() );
        assertEquals( "Foo", catalogs.iterator().next().getName() );

        // Remove the default catalog
        registry.removeCatalog( catalogs.get( 0 ).getUrl(), new NullProgressMonitor() );
        catalogs = registry.getCatalogs( monitor );
        assertNotNull( catalogs );
        assertEquals( 0, catalogs.size() );

        // Restore the default catalog
        registry.addDefaultCatalogs();
        waitForJobsToComplete();
        catalogs = registry.getCatalogs( monitor );
        assertNotNull( catalogs );
        assertEquals( 1, catalogs.size() );
        assertEquals( "Foo", catalogs.iterator().next().getName() );
    }

}
