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
package com.sonatype.nexus.onboarding.installer;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.Nexus;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.item.StorageFileItem;

import com.sonatype.nexus.onboarding.AbstractNexusOnboardingTest;

public class MSEInstallerManagerTest
    extends AbstractNexusOnboardingTest
{
    private MSEInstallerManager installerManager;

    protected Nexus nexus;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        // This is UT, and this plugin will be in Core during this test (when deployed, would be in own classloader
        // managed by PM).
        // Meaning, no auto-repoType discovery will happen, so we need to "trick" the core and register plugin
        // contributed repository
        // since nexus.xml provided by this test contains a repository that is of type provided by this plugin.
        // We have to do this before we lookup Nexus, since it triggers config load.
        registerOnboardingRepository();

        nexus = lookup( Nexus.class );

        installerManager = lookup( MSEInstallerManager.class );
    }

    @Test
    public void testGetInstallers_NoInstallers()
        throws Exception
    {
        publishArtifactsXML( "/artifacts_NoInstallers.xml" );
        Set<MSEInstallerInfo> installers = installerManager.getInstallers();
        assertNotNull( installers );
        assertEquals( "Expected no MSE installers", 0, installers.size() );
    }

    @Test
    public void testGetInstallers_MultipleInstallers()
        throws Exception
    {
        publishArtifactsXML( "/artifacts_MultipleInstallers.xml" );
        Set<MSEInstallerInfo> installers = installerManager.getInstallers();
        assertNotNull( installers );
        assertTrue( "Expected more than one MSE installers", installers.size() > 1 );
    }

    @Test
    public void testGetInstallers_OneInstaller()
        throws Exception
    {
        publishArtifactsXML();
        Set<MSEInstallerInfo> installers = installerManager.getInstallers();
        assertNotNull( installers );
        assertEquals( "Expected one MSE installer", 1, installers.size() );
    }

    @Test
    public void testResolveInstaller_MultipleInstallers()
        throws Exception
    {
        publishArtifactsXML( "/artifacts_MultipleInstallers.xml" );

        // Only one installer can install 1.0.0
        MSEInstallerInfo installer = installerManager.resolveInstaller( "1.0.0" );
        assertNotNull( "Expected installer for 1.0.0", installer );
        assertEquals( "1.0.4.201007010531", installer.getInstallerVersion().toString() );

        // Only one installer can install 1.0.3
        installer = installerManager.resolveInstaller( "1.0.3" );
        assertNotNull( "Expected installer for 1.0.3", installer );
        assertEquals( "1.0.4.201007010531", installer.getInstallerVersion().toString() );

        // Two installers can install 1.0.4
        installer = installerManager.resolveInstaller( "1.0.4" );
        assertNotNull( "Expected installer for 1.0.4", installer );
        assertEquals( "1.0.5.201007010531", installer.getInstallerVersion().toString() );

        // Only one installer can install 1.0.5
        installer = installerManager.resolveInstaller( "1.0.5" );
        assertNotNull( "Expected installer for 1.0.5", installer );
        assertEquals( "1.0.5.201007010531", installer.getInstallerVersion().toString() );
    }

    @Test
    public void testResolveInstaller_OneInstaller()
        throws Exception
    {
        publishArtifactsXML();
        assertNotNull( "Expected installer for 1.0.0", installerManager.resolveInstaller( "1.0.0" ) );
    }

    @Test
    public void testGetJnlpItem()
        throws Exception
    {
        publishArtifactsXML();
        Set<MSEInstallerInfo> installers = installerManager.getInstallers();
        assertNotNull( installers );
        assertEquals( "Expected one MSE installer", 1, installers.size() );

        MSEInstallerInfo installer = installers.iterator().next();
        try
        {
            installerManager.getJnlpItem( installer );
            Assert.fail( "Expected ItemNotFoundException" );
        }
        catch ( ItemNotFoundException expected )
        {
        }

        publishJNLP();
        StorageFileItem jnlpItem = installerManager.getJnlpItem( installer );
        assertNotNull( jnlpItem );
    }
}
