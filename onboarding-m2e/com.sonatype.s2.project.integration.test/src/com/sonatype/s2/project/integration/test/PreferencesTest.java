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

import org.eclipse.core.runtime.CoreException;

import com.sonatype.s2.project.core.test.HttpServer;

public class PreferencesTest
    extends AbstractMavenProjectMaterializationTest
{

    private static final String M2E_ID = "org.maven.ide.eclipse";

    /**
     * Tests project materialization where the import of preferences is disabled.
     */
    public void testPreferencesNotImportedIfRequested()
        throws Exception
    {
        HttpServer server = startHttpServer();

        assertEquals( null, getPreference( M2E_ID, "com.sonatype.s2.project.integration.test" ) );

        materializeProjects( server.getHttpUrl() + "/catalogs/preferences", false, "Project-File" );

        assertMavenProject( "test", "git-test", "0.0.1-SNAPSHOT" );

        assertEquals( null, getPreference( M2E_ID, "com.sonatype.s2.project.integration.test" ) );
    }

    /**
     * Tests project materialization in combination with the import of preferences via file: protocol.
     */
    public void testPreferencesOverFile()
        throws Exception
    {
        HttpServer server = startHttpServer();

        assertEquals( null, getPreference( M2E_ID, "com.sonatype.s2.project.integration.test" ) );

        materializeProjects( server.getHttpUrl() + "/catalogs/preferences", "Project-File" );

        assertMavenProject( "test", "git-test", "0.0.1-SNAPSHOT" );

        assertEquals( "PASSED", getPreference( M2E_ID, "com.sonatype.s2.project.integration.test" ) );
    }

    /**
     * Tests project materialization in combination with the import of missing preferences via HTTP.
     */
    public void testPreferencesMissingOverHttp()
        throws Exception
    {
        HttpServer server = startHttpServer();

        try
        {
            materializeProjects( server.getHttpUrl() + "/catalogs/preferences", "Project-HTTP-Missing" );
        }
        catch ( CoreException e )
        {
            assertTrue( true );
        }

        assertMavenProjects( 0 );
    }

    /**
     * Tests project materialization in combination with the import of preferences via http: protocol.
     */
    public void testPreferencesOverHttp()
        throws Exception
    {
        HttpServer server = startHttpServer();

        assertEquals( null, getPreference( M2E_ID, "com.sonatype.s2.project.integration.test" ) );

        materializeProjects( server.getHttpUrl() + "/catalogs/preferences", "Project-HTTP" );

        assertMavenProject( "test", "git-test", "0.0.1-SNAPSHOT" );

        assertEquals( "PASSED", getPreference( M2E_ID, "com.sonatype.s2.project.integration.test" ) );
    }

    /**
     * Tests project materialization in combination with the import of preferences via HTTP and BASIC authentication.
     */
    public void testPreferencesOverHttpWithBasicAuth()
        throws Exception
    {
        HttpServer server = newHttpServer();
        server.addUser( "testuser", "testpass", "authorized" );
        server.addSecuredRealm( "/*", "authorized" );
        server.start();

        addRealmAndURL( "test", server.getHttpUrl(), "testuser", "testpass" );

        assertEquals( null, getPreference( M2E_ID, "com.sonatype.s2.project.integration.test" ) );

        materializeProjects( server.getHttpUrl() + "/catalogs/preferences", "Project-HTTP" );

        assertMavenProject( "test", "git-test", "0.0.1-SNAPSHOT" );

        assertEquals( "PASSED", getPreference( M2E_ID, "com.sonatype.s2.project.integration.test" ) );
    }

    /**
     * Tests project materialization in combination with the import of preferences via https: protocol.
     */
    public void testPreferencesOverHttps()
        throws Exception
    {
        HttpServer server = startHttpServer();

        assertEquals( null, getPreference( M2E_ID, "com.sonatype.s2.project.integration.test" ) );

        materializeProjects( server.getHttpUrl() + "/catalogs/preferences", "Project-HTTPS" );

        assertMavenProject( "test", "git-test", "0.0.1-SNAPSHOT" );

        assertEquals( "PASSED", getPreference( M2E_ID, "com.sonatype.s2.project.integration.test" ) );
    }

    /**
     * Tests project materialization in combination with the import of preferences via HTTPS and BASIC authentication.
     */
    public void testPreferencesOverHttpsWithBasicAuth()
        throws Exception
    {
        HttpServer server = newHttpServer();
        server.addUser( "testuser", "testpass", "authorized" );
        server.addSecuredRealm( "/preferences/*", "authorized" );
        server.start();

        addRealmAndURL( "test", server.getHttpsUrl(), "testuser", "testpass" );

        assertEquals( null, getPreference( M2E_ID, "com.sonatype.s2.project.integration.test" ) );

        materializeProjects( server.getHttpUrl() + "/catalogs/preferences", "Project-HTTPS" );

        assertMavenProject( "test", "git-test", "0.0.1-SNAPSHOT" );

        assertEquals( "PASSED", getPreference( M2E_ID, "com.sonatype.s2.project.integration.test" ) );
    }

}
