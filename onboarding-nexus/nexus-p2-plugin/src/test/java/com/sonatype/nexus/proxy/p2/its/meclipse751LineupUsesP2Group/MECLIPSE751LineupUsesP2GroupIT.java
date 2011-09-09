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
package com.sonatype.nexus.proxy.p2.its.meclipse751LineupUsesP2Group;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.nexus.proxy.p2.its.AbstractP2LineupIT;
import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupHelper;

public class MECLIPSE751LineupUsesP2GroupIT
    extends AbstractP2LineupIT
{
    private static final String P2_LINEUP_REPO_ID = "p2lineup751";

    public MECLIPSE751LineupUsesP2GroupIT()
    {
        super( P2_LINEUP_REPO_ID );
    }

    @Test
    public void testP2lineup()
        throws Exception
    {
        IP2Lineup p2Lineup = uploadP2Lineup( "p2lineup_MECLIPSE751.xml" );

        File installDir = new File( "target/eclipse/MECLIPSE751" );

        installUsingP2( getP2RepoURL( p2Lineup ), P2LineupHelper.getMasterInstallableUnitId( p2Lineup ),
                        installDir.getCanonicalPath() );

        File feature = new File( installDir, "features/com.sonatype.nexus.p2.its.feature_1.0.0" );
        Assert.assertTrue( feature.exists() && feature.isDirectory() );

        File feature2 = new File( installDir, "features/com.sonatype.nexus.p2.its.feature2_1.0.0" );
        Assert.assertTrue( feature2.exists() && feature2.isDirectory() );

        File bundle = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" );
        Assert.assertTrue( bundle.canRead() );
    }
}
