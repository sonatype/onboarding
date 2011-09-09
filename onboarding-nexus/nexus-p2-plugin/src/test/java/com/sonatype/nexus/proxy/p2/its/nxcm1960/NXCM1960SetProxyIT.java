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
package com.sonatype.nexus.proxy.p2.its.nxcm1960;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Status;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.RemoteHttpProxySettings;
import org.sonatype.nexus.test.utils.SettingsMessageUtil;

import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2IntegrationIT;

public class NXCM1960SetProxyIT
extends AbstractNexusProxyP2IntegrationIT
{

    public NXCM1960SetProxyIT()
    {
        super( "p2proxy" );
    }

    @Test
    public void setTheProxyServer()
        throws Exception
    {
        setupProxyConfig();

        String nexusTestRepoUrl = getNexusTestRepoUrl();

        File installDir = new File("target/eclipse/nxcm1960");

        installUsingP2(
            nexusTestRepoUrl,
            "com.sonatype.nexus.p2.its.feature.feature.group",
            installDir.getCanonicalPath() );

        File feature = new File(installDir, "features/com.sonatype.nexus.p2.its.feature_1.0.0");
        Assert.assertTrue(feature.exists() && feature.isDirectory());

        File bundle = new File(installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar");
        Assert.assertTrue(bundle.canRead());
    }

    private void setupProxyConfig()
        throws IOException
    {
        GlobalConfigurationResource resource = SettingsMessageUtil.getCurrentSettings();

        RemoteHttpProxySettings proxy = resource.getGlobalHttpProxySettings();

        if ( proxy == null )
        {
            proxy = new RemoteHttpProxySettings();
            resource.setGlobalHttpProxySettings( proxy );
        }

        proxy.setProxyHostname( "http://somejunkproxyurl" );
        proxy.setProxyPort( 555 );
        proxy.getNonProxyHosts().clear();
        proxy.addNonProxyHost( "localhost" );

        Status status = SettingsMessageUtil.save( resource );

        Assert.assertTrue( status.isSuccess() );
    }
}
