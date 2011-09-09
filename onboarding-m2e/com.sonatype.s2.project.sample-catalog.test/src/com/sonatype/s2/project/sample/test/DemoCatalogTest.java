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
package com.sonatype.s2.project.sample.test;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;

import com.sonatype.s2.project.integration.test.AbstractMavenProjectMaterializationTest;
import com.sonatype.s2.project.integration.test.SimpleSshHandler;
import com.sonatype.s2.ssh.SshHandler;
import com.sonatype.s2.ssh.SshHandlerManager;

public class DemoCatalogTest
    extends AbstractMavenProjectMaterializationTest
{

    private String catalogUrl;

    private SshHandler sshHandler;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        catalogUrl = "http://s2demo.sonatype.com/content/repositories/democatalog/";

        String userSettings = System.getProperty( "com.sonatype.s2.project.sample.test.userSettings" );
        if ( userSettings != null && !userSettings.startsWith( "${" ) )
        {
            IPreferenceStore prefStore = M2EUIPluginActivator.getDefault().getPreferenceStore();
            prefStore.setValue( "eclipse.m2.userSettingsFile", userSettings );
        }

        sshHandler = new SimpleSshHandler();
        SshHandlerManager.getInstance().addSshHandler( sshHandler );

        String username = System.getProperty( "test.username", "" );
        String password = System.getProperty( "test.password", "" );
        // addRealm( "sonatype.ldap", username, password );
        // addRealm( "sonatype.org", username, password );
        // addRealm( "s2demo", username, password );
        // if ( !catalogUrl.startsWith( "file:" ) )
        // {
        // addRealm( catalogUrl, username, password );
        // }
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        SshHandlerManager.getInstance().removeSshHandler( sshHandler );
        sshHandler = null;

        super.tearDown();
    }

    protected void assertMaterialization( String projectName, int minProjects, int maxProjects )
        throws Exception
    {
        materializeProject( catalogUrl, projectName );
        int projects = getWorkspaceProjects().length;
        assertTrue( projects + " < " + minProjects, projects >= minProjects );
        assertTrue( projects + " > " + maxProjects, projects <= maxProjects );
    }

    // public void testPlexus()
    // throws Exception
    // {
    // assertMaterialization( "Plexus", 5, 5 );
    // }

    public void testMaven()
        throws Exception
    {
        assertMaterialization( "Maven", 8, 12 );
    }

    public void testGit()
        throws Exception
    {
        assertMaterialization( "git (jgit+egit)", 18, 22 );
    }

    public void testNexus()
        throws Exception
    {
        assertMaterialization( "Nexus", 37, 39 );
    }

    // public void testPolyglotMaven()
    // throws Exception
    // {
    // assertMaterialization( "Polyglot Maven", 8, 12 );
    // }
    //
    // public void testTycho()
    // throws Exception
    // {
    // assertMaterialization( "Tycho", 1, 1 );
    // }
    //
    // public void testTychoM2eConfigurator()
    // throws Exception
    // {
    // assertMaterialization( "Tycho M2E Configurator", 5, 5 );
    // }
    //
    // public void testM2Eclipse()
    // throws Exception
    // {
    // assertMaterialization( "m2e", 37, 41 );
    // }

}
