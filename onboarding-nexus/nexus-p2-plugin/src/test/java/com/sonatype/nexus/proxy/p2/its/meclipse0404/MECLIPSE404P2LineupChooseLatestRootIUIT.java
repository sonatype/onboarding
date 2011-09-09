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
package com.sonatype.nexus.proxy.p2.its.meclipse0404;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.nexus.proxy.p2.its.AbstractP2LineupIT;
import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupHelper;

/**
 * Tests that p2 lineup root IUs specified with a version range are "resolved" to the latest version. The test uses two
 * repositories that contain the same IU with different versions. The p2 lineup descriptors specify both repositories in
 * different orders.
 */
public class MECLIPSE404P2LineupChooseLatestRootIUIT
    extends AbstractP2LineupIT
{
    private static final String P2_LINEUP_REPO_ID = "p2lineup";

    public MECLIPSE404P2LineupChooseLatestRootIUIT()
    {
        super( P2_LINEUP_REPO_ID );
    }

    @Test
    public void oldFirst()
        throws Exception
    {
        IP2Lineup p2Lineup = uploadP2Lineup( "p2lineup_MECLIPSE404_OldFirst.xml" );

        File installDir = new File( "target/eclipse/meclipse0404/oldfirst" );

        installUsingP2( getP2RepoURL( p2Lineup ), P2LineupHelper.getMasterInstallableUnitId( p2Lineup ),
                        installDir.getCanonicalPath() );

        File bundle = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.1.jar" );
        Assert.assertTrue( bundle.canRead() );
    }

    @Test
    public void newFirst()
        throws Exception
    {
        IP2Lineup p2Lineup = uploadP2Lineup( "p2lineup_MECLIPSE404_NewFirst.xml" );

        File installDir = new File( "target/eclipse/meclipse0404/newfirst" );

        installUsingP2( getP2RepoURL( p2Lineup ), P2LineupHelper.getMasterInstallableUnitId( p2Lineup ),
                        installDir.getCanonicalPath() );

        File bundle = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.1.jar" );
        Assert.assertTrue( bundle.canRead() );
    }
}
