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
package com.sonatype.s2.project.integration.test;

import org.eclipse.core.runtime.IStatus;

import com.sonatype.s2.project.core.IS2ProjectCatalogRegistryEntry;
import com.sonatype.s2.project.core.test.HttpServer;

public class ProjectCatalogTest
    extends AbstractMavenProjectMaterializationTest
{

    /**
     * Tests missing catalog retrieval via file protocol.
     */
    public void testMissingCatalogOverFile()
        throws Exception
    {
        String catalogUrl = getBaseUri() + "/resources/catalogs/missing";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertFalse( entry.getStatus().toString(), entry.isLoaded() );
        assertNotNull( entry.getStatus() );
        assertEquals( IStatus.ERROR, entry.getStatus().getSeverity() );
    }

    /**
     * Tests catalog retrieval via file protocol.
     */
    public void testCatalogOverFile()
        throws Exception
    {
        String catalogUrl = getBaseUri() + "/resources/catalogs/basic";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertTrue( entry.getStatus().toString(), entry.isLoaded() );
        assertEquals( 2, entry.getCatalog().getEntries().size() );
    }

    /**
     * Tests missing catalog retrieval via HTTP protocol.
     */
    public void testMissingCatalogOverHttp()
        throws Exception
    {
        HttpServer server = startHttpServer();

        String catalogUrl = server.getHttpUrl() + "/catalogs/missing";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertFalse( entry.getStatus().toString(), entry.isLoaded() );
        assertNotNull( entry.getStatus() );
        assertEquals( IStatus.ERROR, entry.getStatus().getSeverity() );
    }

    /**
     * Tests catalog retrieval via HTTP protocol when catalog URL points at directory.
     */
    public void testCatalogOverHttp()
        throws Exception
    {
        HttpServer server = startHttpServer();

        String catalogUrl = server.getHttpUrl() + "/catalogs/basic";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertTrue( entry.getStatus().toString(), entry.isLoaded() );
        assertEquals( 2, entry.getCatalog().getEntries().size() );
    }

    /**
     * Tests catalog retrieval via HTTP protocol when catalog URL already ends with "catalog.xml".
     */
    public void testCatalogOverHttpWithUrlEndingWithCatalogXml()
        throws Exception
    {
        HttpServer server = startHttpServer();

        String catalogUrl = server.getHttpUrl() + "/catalogs/basic/catalog.xml";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertTrue( entry.getStatus().toString(), entry.isLoaded() );
        assertEquals( 2, entry.getCatalog().getEntries().size() );
    }

    /**
     * Tests catalog retrieval via HTTP protocol and BASIC authentication.
     */
    public void testCatalogOverHttpWithBasicAuth()
        throws Exception
    {
        HttpServer server = newHttpServer();
        server.addUser( "testuser", "testpass", "authorized" );
        server.addSecuredRealm( "/*", "authorized" );
        server.start();

        addRealmAndURL( "test", server.getHttpUrl(), "testuser", "testpass" );

        String catalogUrl = server.getHttpUrl() + "/catalogs/basic";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertTrue( entry.getStatus().toString(), entry.isLoaded() );
        assertEquals( 2, entry.getCatalog().getEntries().size() );
    }

    /**
     * Tests catalog retrieval via HTTP protocol and BASIC authentication.
     */
    public void testCatalogOverHttpWithBasicAuthAndWithUrlEndingWithCatalogXml()
        throws Exception
    {
        HttpServer server = newHttpServer();
        server.addUser( "testuser", "testpass", "authorized" );
        server.addSecuredRealm( "/*", "authorized" );
        server.start();

        String catalogUrl = server.getHttpUrl() + "/catalogs/basic/catalog.xml";
        addRealmAndURL( "test", catalogUrl, "testuser", "testpass" );
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertTrue( entry.getStatus().toString(), entry.isLoaded() );
        assertEquals( 2, entry.getCatalog().getEntries().size() );
    }

    /**
     * Tests catalog retrieval via HTTP protocol and BASIC authentication when the password is empty.
     */
    public void testCatalogOverHttpWithBasicAuthUsingEmptyPassword()
        throws Exception
    {
        HttpServer server = newHttpServer();
        server.addUser( "testuser", "", "authorized" );
        server.addSecuredRealm( "/*", "authorized" );
        server.start();

        addRealmAndURL( "test", server.getHttpUrl(), "testuser", "" );

        String catalogUrl = server.getHttpUrl() + "/catalogs/basic";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertTrue( entry.getStatus().toString(), entry.isLoaded() );
        assertEquals( 2, entry.getCatalog().getEntries().size() );
    }

    /**
     * Tests catalog retrieval via HTTPS protocol.
     */
    public void testCatalogOverHttps()
        throws Exception
    {
        HttpServer server = startHttpServer();

        String catalogUrl = server.getHttpsUrl() + "/catalogs/basic";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertTrue( entry.getStatus().toString(), entry.isLoaded() );
        assertEquals( 2, entry.getCatalog().getEntries().size() );
    }

    /**
     * Tests catalog retrieval via HTTPS protocol and BASIC authentication.
     */
    public void testCatalogOverHttpsWithBasicAuth()
        throws Exception
    {
        HttpServer server = newHttpServer();
        server.addUser( "testuser", "testpass", "authorized" );
        server.addSecuredRealm( "/*", "authorized" );
        server.start();

        addRealmAndURL( "test", server.getHttpsUrl(), "testuser", "testpass" );

        String catalogUrl = server.getHttpsUrl() + "/catalogs/basic";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertTrue( entry.getStatus().toString(), entry.isLoaded() );
        assertEquals( 2, entry.getCatalog().getEntries().size() );
    }

    /**
     * Tests catalog retrieval via HTTPS protocol and BASIC authentication when the password is empty.
     */
    public void testCatalogOverHttpsWithBasicAuthUsingEmptyPassword()
        throws Exception
    {
        HttpServer server = newHttpServer();
        server.addUser( "testuser", "", "authorized" );
        server.addSecuredRealm( "/*", "authorized" );
        server.start();

        addRealmAndURL( "test", server.getHttpsUrl(), "testuser", "" );

        String catalogUrl = server.getHttpsUrl() + "/catalogs/basic";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertTrue( entry.getStatus().toString(), entry.isLoaded() );
        assertEquals( 2, entry.getCatalog().getEntries().size() );
    }

    /**
     * Tests catalog retrieval via HTTPS after redirection from HTTP protocol.
     */
    public void testCatalogOverHttpsAfterRedirectionFromHttp()
        throws Exception
    {
        HttpServer server = newHttpServer();
        server.setRedirectToHttps( true );
        server.start();

        String catalogUrl = server.getHttpsUrl() + "/catalogs/basic";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertTrue( entry.getStatus().toString(), entry.isLoaded() );
        assertEquals( 2, entry.getCatalog().getEntries().size() );
    }

    /**
     * Tests catalog retrieval via HTTP when the server hangs.
     */
    public void testCatalogOverHttpWhenServerTimeouts()
        throws Exception
    {
        HttpServer server = newHttpServer();
        server.setLatency( 60 * 60 * 1000 );
        server.start();

        String catalogUrl = server.getHttpsUrl() + "/catalogs/basic";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertFalse( entry.getStatus().toString(), entry.isLoaded() );
        assertNotNull( entry.getStatus() );
        assertEquals( IStatus.ERROR, entry.getStatus().getSeverity() );
    }

    /**
     * Tests catalog retrieval via HTTP when the host is bad.
     */
    public void testCatalogOverHttpWhenHostIsBad()
        throws Exception
    {
        HttpServer server = newHttpServer();
        server.start();

        String catalogUrl = "http://bad.host/catalogs/basic";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertFalse( entry.getStatus().toString(), entry.isLoaded() );
        assertNotNull( entry.getStatus() );
        assertEquals( IStatus.ERROR, entry.getStatus().getSeverity() );
    }

    /**
     * Tests catalog retrieval via HTTP proxy.
     */
    public void testCatalogOverHttpProxy()
        throws Exception
    {
        HttpServer server = startHttpServer();

        setProxy( "localhost", server.getHttpPort(), false, null, null );

        String catalogUrl = "http://host-to-be-proxied.org/catalogs/basic";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertTrue( entry.getStatus().toString(), entry.isLoaded() );
        assertEquals( 2, entry.getCatalog().getEntries().size() );
    }

    /**
     * Tests catalog retrieval via HTTP proxy that requires authentication.
     */
    public void testCatalogOverHttpProxyWithProxyAuth()
        throws Exception
    {
        HttpServer server = newHttpServer();
        server.setProxyAuth( "proxyuser", "proxypass" );
        server.start();

        setProxy( "localhost", server.getHttpPort(), false, "proxyuser", "proxypass" );

        String catalogUrl = "http://host-to-be-proxied.org/catalogs/basic";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertTrue( entry.getStatus().toString(), entry.isLoaded() );
        assertEquals( 2, entry.getCatalog().getEntries().size() );
    }

    /**
     * Tests catalog retrieval via HTTP when a non-applicable proxy is present.
     */
    public void testCatalogOverHttpWithNonProxiedHost()
        throws Exception
    {
        HttpServer server = startHttpServer();

        setProxy( "bad.host", 12347, false, null, null );
        setNonProxiedHosts( "localhost" );

        String catalogUrl = server.getHttpUrl() + "/catalogs/basic";
        IS2ProjectCatalogRegistryEntry entry = addCatalog( catalogUrl );
        assertTrue( entry.getStatus().toString(), entry.isLoaded() );
        assertEquals( 2, entry.getCatalog().getEntries().size() );
    }

}
