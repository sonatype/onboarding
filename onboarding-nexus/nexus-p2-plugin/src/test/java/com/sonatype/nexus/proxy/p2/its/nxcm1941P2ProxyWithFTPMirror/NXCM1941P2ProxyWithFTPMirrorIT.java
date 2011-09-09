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
package com.sonatype.nexus.proxy.p2.its.nxcm1941P2ProxyWithFTPMirror;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.test.utils.TestProperties;

import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2IntegrationIT;

public class NXCM1941P2ProxyWithFTPMirrorIT
    extends AbstractNexusProxyP2IntegrationIT
{
    public NXCM1941P2ProxyWithFTPMirrorIT()
    {
        super( "p2proxywithmirror" );
    }

    @Override
    protected void copyTestResources()
        throws IOException
    {
        super.copyTestResources();

        String proxyRepoBaseUrl = TestProperties.getString( "proxy.repo.base.url" );
        Assert.assertTrue( proxyRepoBaseUrl.startsWith( "http://" ) );

        replaceInFile( "target/nexus/proxy-repo/p2repowithftpmirror/artifacts.xml", "${proxy-repo-base-url}",
                       proxyRepoBaseUrl );
        replaceInFile( "target/nexus/proxy-repo/p2repowithftpmirror/mirrors.xml", "${proxy-repo-base-url}",
                       proxyRepoBaseUrl );
        replaceInFile( "target/nexus/proxy-repo/p2repowithftpmirror/mirrors.xml", "${ftp-proxy-repo-base-url}", "ftp"
            + proxyRepoBaseUrl.substring( 4 ) );
    }

    @Test
    public void testProxyWithFTPMirror()
        throws Exception
    {
        String nexusTestRepoUrl = getNexusTestRepoUrl();

        File installDir = new File( "target/eclipse/nxcm1491" );

        installUsingP2( nexusTestRepoUrl, "com.sonatype.nexus.p2.its.feature.feature.group", installDir
            .getCanonicalPath() );

        File feature = new File( installDir, "features/com.sonatype.nexus.p2.its.feature_1.0.0" );
        Assert.assertTrue( feature.exists() && feature.isDirectory() );

        File bundle = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" );
        Assert.assertTrue( bundle.canRead() );
    }
}
