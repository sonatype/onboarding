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
package com.sonatype.nexus.proxy.p2.its;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupHelper;

public abstract class AbstractNXCM1923LineupUsesUpdateSiteIT
    extends AbstractP2LineupIT
{
    private static final String P2_LINEUP_REPO_ID = "p2lineup1923";

    protected String getP2LineupDestinationPath()
    {
        return "/test" + getTestType();
    }

    protected abstract String getTestType();

    public AbstractNXCM1923LineupUsesUpdateSiteIT()
    {
        super( P2_LINEUP_REPO_ID );
    }

    @Test
    public void testP2lineup()
        throws Exception
    {
        // Trigger the mirroring of the updatesite into updatesiteproxy
        String updateSiteProxyRepoUrl = getNexusTestRepoUrl( "updatesiteproxy" );
        final File tempFile = File.createTempFile( "NXCM1923LineupUsesUpdateSiteIT", ".site.xml" );
        try
        {
            downloadFile( new URL( updateSiteProxyRepoUrl + "/content.xml" ), tempFile.getCanonicalPath() );
        }
        catch ( FileNotFoundException e )
        {
            // That's fine
        }
        finally
        {
            tempFile.delete();
        }

        // Wait for the updatesiteproxy to be updated
        TaskScheduleUtil.waitForAllTasksToStop();

        IP2Lineup p2Lineup = uploadP2Lineup( "p2lineup_NXCM1923" + getTestType() + ".xml" );

        File installDir = new File( "target/eclipse/nxcm1923" + getTestType() );

        installUsingP2( getP2RepoURL( p2Lineup ), P2LineupHelper.getMasterInstallableUnitId( p2Lineup ),
                        installDir.getCanonicalPath() );

        File bundle = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" );
        Assert.assertTrue( bundle.canRead() );
    }
}
