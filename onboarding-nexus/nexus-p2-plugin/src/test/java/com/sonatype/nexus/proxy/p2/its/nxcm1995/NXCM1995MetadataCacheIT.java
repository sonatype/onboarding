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
package com.sonatype.nexus.proxy.p2.its.nxcm1995;

import java.io.File;
import java.net.URL;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.sonatype.nexus.test.utils.FileTestingUtils;

import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2IntegrationIT;

public class NXCM1995MetadataCacheIT
    extends AbstractNexusProxyP2IntegrationIT
{

    private static final String REPO = "p2proxy";

    @Ignore
    @Test
    public void make()
        throws Exception
    {
        // check original content
        File f1 =
            downloadFile( new URL( getRepositoryUrl( REPO ) + "/content.xml" ),
                          "target/downloads/nxcm1995/1/content.xml" );
        Assert.assertTrue( f1.exists() );
        String c = FileUtils.fileRead( f1 );
        Assert.assertTrue( c.contains( "com.adobe.flexbuilder.utils.osnative.win" ) );
        Assert.assertFalse( c.contains( "com.sonatype.nexus.p2.its.feature2.feature.jar" ) );

        // check original artifact
        File a1 =
            downloadFile( new URL( getRepositoryUrl( REPO ) + "/artifacts.xml" ),
                          "target/downloads/nxcm1995/1/artifacts.xml" );
        Assert.assertTrue( a1.exists() );
        String a = FileUtils.fileRead( a1 );
        Assert.assertTrue( a.contains( "com.adobe.flexbuilder.multisdk" ) );
        Assert.assertFalse( a.contains( "com.sonatype.nexus.p2.its.feature2" ) );

        File reponxcm1995 = new File( localStorageDir, "nxcm1995" );

        // check new content
        File newContentXml = new File( localStorageDir, "p2repo2/content.xml" );
        Assert.assertTrue( newContentXml.exists() );
        c = FileUtils.fileRead( newContentXml );
        Assert.assertFalse( c.contains( "com.adobe.flexbuilder.utils.osnative.win" ) );
        Assert.assertTrue( c.contains( "com.sonatype.nexus.p2.its.feature2.feature.jar" ) );
        FileUtils.copyFileToDirectory( newContentXml, new File( reponxcm1995, "memberrepo1" ) );
        FileUtils.copyFileToDirectory( newContentXml, new File( reponxcm1995, "memberrepo2" ) );

        File newArtifactsXml = new File( localStorageDir, "p2repo2/artifacts.xml" );
        Assert.assertTrue( newArtifactsXml.exists() );
        FileUtils.copyFileToDirectory( newArtifactsXml, new File( reponxcm1995, "memberrepo1" ) );
        FileUtils.copyFileToDirectory( newArtifactsXml, new File( reponxcm1995, "memberrepo2" ) );

        // metadata cache expires in ONE minute, so let's give it some time to expire
        Thread.yield();
        Thread.sleep( 1 * 60 * 1000 );
        Thread.yield();
        Thread.sleep( 1 * 60 * 1000 );
        Thread.yield();

        // ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
        // prop.setId( "repositoryId" );
        // prop.setValue( REPO );
        // TaskScheduleUtil.runTask( ExpireCacheTaskDescriptor.ID, prop );
        // TaskScheduleUtil.waitForAllTasksToStop();

        // make sure nexus has the right content after metadata cache expires
        File f2 =
            downloadFile( new URL( getRepositoryUrl( REPO ) + "/content.xml" ),
                          "target/downloads/nxcm1995/2/content.xml" );
        Assert.assertTrue( f2.exists() );
        c = FileUtils.fileRead( f2 );
        Assert.assertFalse( c.contains( "com.adobe.flexbuilder.utils.osnative.win" ) );
        Assert.assertTrue( c.contains( "com.sonatype.nexus.p2.its.feature2.feature.jar" ) );

        Assert.assertFalse( FileTestingUtils.compareFileSHA1s( f1, f2 ) );

        // make sure nexus has the right content after metadata cache expires
        File a2 =
            downloadFile( new URL( getRepositoryUrl( REPO ) + "/artifacts.xml" ),
                          "target/downloads/nxcm1995/2/artifacts.xml" );
        Assert.assertTrue( a2.exists() );
        a = FileUtils.fileRead( a2 );
        Assert.assertFalse( a.contains( "com.adobe.flexbuilder.multisdk" ) );
        Assert.assertTrue( a.contains( "com.sonatype.nexus.p2.its.feature2" ) );

        Assert.assertFalse( FileTestingUtils.compareFileSHA1s( a1, a2 ) );
    }

}
