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
package com.sonatype.nexus.proxy.p2.its.meclipse0850P2LineupIUWithTargetEnvironments;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.nexus.proxy.p2.its.AbstractP2LineupIT;
import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupHelper;

public class MECLIPSE850P2LineupIUWithTargetEnvironmentsIT
    extends AbstractP2LineupIT
{
    private static final String P2_LINEUP_REPO_ID = "p2lineups";

    public MECLIPSE850P2LineupIUWithTargetEnvironmentsIT()
    {
        super( P2_LINEUP_REPO_ID );
    }

    @Test
    public void IUWithFakeTargetEnvironment()
        throws Exception
    {
        IP2Lineup p2Lineup = uploadP2Lineup( "IUWithFakeTargetEnvironment.xml" );

        File installDir = new File( "target/eclipse/meclipse0850_IUWithFakeTargetEnvironment" );

        installUsingP2( getP2RepoURL( p2Lineup ), P2LineupHelper.getMasterInstallableUnitId( p2Lineup ),
                        installDir.getCanonicalPath(), "win32", "win32", "x86" );

        File bundle = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" );
        Assert.assertTrue( bundle.canRead() );

        File bundle3 = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle3_1.0.0.jar" );
        Assert.assertFalse( bundle3.canRead() );
    }

    @Test
    public void IUWithCorrectTargetEnvironment()
        throws Exception
    {
        IP2Lineup p2Lineup = uploadP2Lineup( "IUWithCorrectTargetEnvironment.xml" );

        File installDir = new File( "target/eclipse/meclipse0850_IUWithCorrectTargetEnvironment" );

        installUsingP2( getP2RepoURL( p2Lineup ), P2LineupHelper.getMasterInstallableUnitId( p2Lineup ),
                        installDir.getCanonicalPath(), "win32", "win32", "x86" );

        File bundle = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" );
        Assert.assertTrue( bundle.canRead() );

        File bundle3 = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle3_1.0.0.jar" );
        Assert.assertTrue( bundle3.canRead() );
    }
}
