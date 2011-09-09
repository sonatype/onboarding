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
package com.sonatype.nexus.proxy.p2.its.nxcm1898;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.test.utils.SettingsMessageUtil;

import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2IntegrationIT;

public abstract class AbstractProxyTimeout
    extends AbstractNexusProxyP2IntegrationIT
{

    public AbstractProxyTimeout()
    {
        super( "p2proxy" );
        //System.setProperty( "org.eclipse.ecf.provider.filetransfer.retrieve.readTimeout", "30000" );
    }



    @Override
    public void startProxy()
        throws Exception
    {
        if ( server == null )
        {
            server = (ServletServer) lookup( ServletServer.ROLE, "timeout" );
            server.start();
        }
    }

    @After
    public void stopProxy()
        throws Exception
    {
        if ( server != null )
        {
            server.stop();
            server = null;
        }
    }


    protected void doTest( int timeout )
        throws IOException, Exception
    {
        String nexusTestRepoUrl = getNexusTestRepoUrl();

        File installDir = new File( "target/eclipse/nxcm1898" );

        // give it a good amount of time
        GlobalConfigurationResource settings = SettingsMessageUtil.getCurrentSettings();
        settings.getGlobalConnectionSettings().setConnectionTimeout( timeout );
        SettingsMessageUtil.save( settings );

        installUsingP2( nexusTestRepoUrl, "com.sonatype.nexus.p2.its.feature.feature.group",
                        installDir.getCanonicalPath() );

        File feature = new File( installDir, "features/com.sonatype.nexus.p2.its.feature_1.0.0" );
        Assert.assertTrue( feature.exists() && feature.isDirectory() );

        File bundle = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" );
        Assert.assertTrue( bundle.canRead() );

    }

}