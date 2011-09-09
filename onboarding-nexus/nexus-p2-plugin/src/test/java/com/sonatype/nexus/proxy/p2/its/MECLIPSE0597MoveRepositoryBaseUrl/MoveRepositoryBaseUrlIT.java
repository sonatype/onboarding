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
package com.sonatype.nexus.proxy.p2.its.MECLIPSE0597MoveRepositoryBaseUrl;

import org.junit.Assert;
import org.junit.Test;
import com.sonatype.nexus.proxy.p2.its.AbstractP2LineupIT;

public class MoveRepositoryBaseUrlIT
    extends AbstractP2LineupIT
{
    private static final String P2_LINEUP_REPO_ID = "p2lineups";

    public MoveRepositoryBaseUrlIT()
    {
        super( P2_LINEUP_REPO_ID );
    }

    // @Override
    // protected void copyTestResources()
    // throws IOException
    // {
    // super.copyTestResources();
    //
    // File src = getTestFile( "p2lineup_MECLIPSE597.xml" );
    // Assert.assertTrue( src.exists() );
    //
    // File target =
    // new File( nexusWorkDir + "/storage/" + P2_LINEUP_REPO_ID + P2_LINEUP_DESTINATION_PATH
    // + P2LineupConstants.LINEUP_DESCRIPTOR_XML );
    //
    // FileTestingUtils.interpolationFileCopy( src, target, TestProperties.getAll() );
    // }

    @Test
    public void p2lineupWithP2Advice()
        throws Exception
    {
        Assert.assertTrue( true );

        //This test no longer valid, as we removed the republishing of lineups, once proper change is put in place
        //this test can be uncommented
        /**
        IP2Lineup p2Lineup = uploadP2Lineup( "p2lineup_MECLIPSE597.xml" );

        String p2Path = new P2Gav( p2Lineup ).toPathString();

        String nexusTestRepoUrl = getNexusTestRepoUrl();

        URL url = new URL( nexusTestRepoUrl + p2Path + P2Constants.CONTENT_XML );
        File contentXml = downloadFile( url, "target/eclipse/meclipse0597/original.xml" );

        Assert.assertTrue( FileUtils.fileRead( contentXml ).contains(
                                                                      "addJvmArg(jvmArg:-Ds2.catalogs="
                                                                          + getBaseNexusUrl() ) );

        String newBaseURL = "http://foo/bar";

        GlobalConfigurationResource settings = SettingsMessageUtil.getCurrentSettings();
        settings.getGlobalRestApiSettings().setBaseUrl( newBaseURL );
        SettingsMessageUtil.save( settings );

        TaskScheduleUtil.waitForAllTasksToStop( 2000 );

        // Not sure why we have to have this extra delay here :(
        // This test fails on some machines without it...
        Thread.sleep( 1000 );

        File newBaseUrlContentXml = downloadFile( url, "target/eclipse/meclipse0597/newURL.xml" );

        Assert.assertTrue( FileUtils.fileRead( newBaseUrlContentXml ).contains(
                                                                                "addJvmArg(jvmArg:-Ds2.catalogs="
                                                                                    + newBaseURL ) );

        **/
    }

}
