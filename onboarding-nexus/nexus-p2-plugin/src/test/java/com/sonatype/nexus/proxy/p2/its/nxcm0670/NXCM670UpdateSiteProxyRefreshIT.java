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
package com.sonatype.nexus.proxy.p2.its.nxcm0670;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2IntegrationIT;

public class NXCM670UpdateSiteProxyRefreshIT
    extends AbstractNexusProxyP2IntegrationIT
{
    public NXCM670UpdateSiteProxyRefreshIT()
    {
        super( "updatesiteproxy" );
    }

    @Test
    public void updatesiteproxy() throws Exception {
        File nexusDir = new File("target/nexus/nexus-work-dir/storage/updatesiteproxy");
        File remoteDir = new File( "target/nexus/proxy-repo/updatesite0670" );

        TaskScheduleUtil.run( "1" );
        TaskScheduleUtil.waitForAllTasksToStop();

        Assert.assertTrue( new File( nexusDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar").exists() );

        FileUtils.copyFile( new File( remoteDir, "site-empty.xml"), new File( remoteDir, "site.xml") );

        TaskScheduleUtil.run( "1" );
        TaskScheduleUtil.waitForAllTasksToStop();

        Assert.assertFalse( new File( nexusDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar").exists() );

    }
}