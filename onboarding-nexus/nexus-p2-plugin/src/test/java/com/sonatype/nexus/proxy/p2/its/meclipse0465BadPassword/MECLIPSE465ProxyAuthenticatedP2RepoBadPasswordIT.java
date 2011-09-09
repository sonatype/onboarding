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
package com.sonatype.nexus.proxy.p2.its.meclipse0465BadPassword;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2SecureIntegrationIT;
import com.sonatype.nexus.proxy.p2.its.P2ITException;

public class MECLIPSE465ProxyAuthenticatedP2RepoBadPasswordIT
    extends AbstractNexusProxyP2SecureIntegrationIT
{
    public MECLIPSE465ProxyAuthenticatedP2RepoBadPasswordIT()
    {
        super( "proxyAuthenticatedP2RepoBadPassword" );
    }

    @Test
    public void MECLIPSE465ProxyAuthenticatedP2RepoBadPassword()
        throws Exception
    {
        String nexusTestRepoUrl = getNexusTestRepoUrl();

        File installDir = new File( "target/eclipse/meclipse0465BadPassword" );

        try
        {
            installUsingP2( nexusTestRepoUrl, "com.sonatype.nexus.p2.its.feature.feature.group", installDir.getCanonicalPath() );
            Assert.fail( "Expected P2ITException" );
        }
        catch ( P2ITException e )
        {
            if ( !e.getMessage().contains(
                                           "No repository found at " + getBaseNexusUrl()
                                               + "content/repositories/proxyAuthenticatedP2RepoBadPassword/" ) )
            {
                throw e;
            }
        }

        File feature = new File( installDir, "features/com.sonatype.nexus.p2.its.feature_1.0.0" );
        Assert.assertFalse( feature.exists() );

        File bundle = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" );
        Assert.assertFalse( bundle.canRead() );
    }
}
