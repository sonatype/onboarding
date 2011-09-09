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
package com.sonatype.nexus.proxy.p2.its.nxcm1720;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2IntegrationIT;

public class NXCM1720UpdateSiteFeatureAndPluginFileNameChangesIT
    extends AbstractNexusProxyP2IntegrationIT
{
    public NXCM1720UpdateSiteFeatureAndPluginFileNameChangesIT()
    {
        super( "updatesiteproxy" );
    }

    @Test
    public void testSiteWithAbsoluteUrls()
        throws Exception
    {
        File nexusDir = new File( nexusWorkDir, "storage/updatesiteproxy" );

        TaskScheduleUtil.run( "1" );
        TaskScheduleUtil.waitForAllTasksToStop();

        Assert.assertFalse( new File( nexusDir, "features/com.sonatype.nexus.p2.its.feature_1.0.0-feature.jar" ).exists() );
        Assert.assertFalse( new File( nexusDir, "features/com.sonatype.nexus.p2.its.feature.local_1.0.0-feature.jar" ).exists() );
        Assert.assertTrue( new File( nexusDir, "features/com.sonatype.nexus.p2.its.feature_1.0.0.jar" ).exists() );
        Assert.assertTrue( new File( nexusDir, "features/com.sonatype.nexus.p2.its.feature2_1.0.0.jar" ).exists() );
        Assert.assertTrue( new File( nexusDir, "features/com.sonatype.nexus.p2.its.feature.local_1.0.0.jar" ).exists() );
        Assert.assertTrue( new File( nexusDir, "features/com.sonatype.nexus.p2.its.feature2.local_1.0.0.jar" ).exists() );
        Assert.assertTrue( new File( nexusDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" ).exists() );
        Assert.assertTrue( new File( nexusDir, "plugins/com.sonatype.nexus.p2.its.bundle.local_1.0.0.jar" ).exists() );
    }
}