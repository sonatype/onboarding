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
package com.sonatype.nexus.proxy.p2.its.meclipse0465;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2SecureIntegrationIT;

public class MECLIPSE465ProxyAuthenticatedP2RepoIT
    extends AbstractNexusProxyP2SecureIntegrationIT
{
    public MECLIPSE465ProxyAuthenticatedP2RepoIT()
    {
        super( "proxyAuthenticatedP2Repo" );
    }

    @Test
    public void MECLIPSE465ProxyAuthenticatedP2Repo()
        throws Exception
    {
        String nexusTestRepoUrl = getNexusTestRepoUrl();

        File installDir = new File( "target/eclipse/meclipse0465" );

        installUsingP2( nexusTestRepoUrl, "com.sonatype.nexus.p2.its.feature.feature.group", installDir.getCanonicalPath() );

        File feature = new File( installDir, "features/com.sonatype.nexus.p2.its.feature_1.0.0" );
        Assert.assertTrue( feature.exists() && feature.isDirectory() );

        File bundle = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" );
        Assert.assertTrue( bundle.canRead() );

        File eclipseSecureStorage = new File( "target/nexus/nexus-work-dir/conf/eclipse.secure_storage" );
        Assert.assertTrue( eclipseSecureStorage.exists() );
    }
}
