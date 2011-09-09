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
package com.sonatype.nexus.proxy.p2.its.nxcm0128;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2IntegrationIT;

public class NXCM128P2GroupIT
    extends AbstractNexusProxyP2IntegrationIT
{

    @Test
    public void make() throws Exception {
        File installDir = new File("target/eclipse/nxcm0128");

        installUsingP2(
            getGroupUrl( "p2group" ),
            "com.sonatype.nexus.p2.its.feature2.feature.group",
            installDir.getCanonicalPath() );

        File feature = new File(installDir, "features/com.sonatype.nexus.p2.its.feature_1.0.0");
        Assert.assertTrue(feature.exists() && feature.isDirectory());

        File feature2 = new File(installDir, "features/com.sonatype.nexus.p2.its.feature2_1.0.0");
        Assert.assertTrue(feature2.exists() && feature2.isDirectory());

        File bundle = new File(installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar");
        Assert.assertTrue(bundle.canRead());
    }

}
