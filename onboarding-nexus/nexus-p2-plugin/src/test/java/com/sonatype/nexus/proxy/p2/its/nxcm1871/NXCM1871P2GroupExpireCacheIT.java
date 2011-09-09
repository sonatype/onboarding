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
package com.sonatype.nexus.proxy.p2.its.nxcm1871;

import java.io.File;
import java.net.URL;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.ExpireCacheTaskDescriptor;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2IntegrationIT;

public class NXCM1871P2GroupExpireCacheIT
    extends AbstractNexusProxyP2IntegrationIT
{

    private static final String GROUP = "p2group";

    @Test
    public void make()
        throws Exception
    {
        // check original content
        File f1 =
            downloadFile( new URL( getGroupUrl( GROUP ) + "/content.xml" ), "target/downloads/nxcm1871/1/content.xml" );
        Assert.assertTrue( f1.exists() );
        String c = FileUtils.fileRead( f1 );
        Assert.assertTrue( c.contains( "com.sonatype.nexus.p2.its.feature2.feature.jar" ) );
        Assert.assertFalse( c.contains( "com.sonatype.nexus.p2.its.feature3.feature.jar" ) );

        // check new content
        File newContentXml = new File( localStorageDir, "p2repo3/content.xml" );
        Assert.assertTrue( newContentXml.exists() );
        c = FileUtils.fileRead( newContentXml );
        Assert.assertFalse( c.contains( "com.sonatype.nexus.p2.its.feature2.feature.jar" ) );
        Assert.assertTrue( c.contains( "com.sonatype.nexus.p2.its.feature3.feature.jar" ) );

        File repoNxcm1871 = new File( localStorageDir, "nxcm1871" );
        FileUtils.copyFileToDirectory( newContentXml, repoNxcm1871 );
        File newArtifactsXml = new File( localStorageDir, "p2repo3/artifacts.xml" );
        FileUtils.copyFileToDirectory( newArtifactsXml, repoNxcm1871 );

        ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
        prop.setKey( "repositoryId" );
        prop.setValue( GROUP );

        TaskScheduleUtil.runTask( ExpireCacheTaskDescriptor.ID, prop );

        // make sure nexus has the right content after reindex
        File f2 =
            downloadFile( new URL( getGroupUrl( GROUP ) + "/content.xml" ), "target/downloads/nxcm1871/2/content.xml" );
        Assert.assertTrue( f2.exists() );
        c = FileUtils.fileRead( f2 );
        Assert.assertFalse( c.contains( "com.sonatype.nexus.p2.its.feature2.feature.jar" ) );
        Assert.assertTrue( c.contains( "com.sonatype.nexus.p2.its.feature3.feature.jar" ) );

        Assert.assertFalse( FileTestingUtils.compareFileSHA1s( f1, f2 ) );
    }

}
