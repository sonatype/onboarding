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
package com.sonatype.nexus.proxy.p2.its.meclipse0942LineupUsesP2Lineup;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.nexus.proxy.p2.its.AbstractP2LineupIT;
import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupHelper;

public class MECLIPSE942LineupUsesP2LineupIT
    extends AbstractP2LineupIT
{
    private static final String P2_LINEUP_REPO_ID = "p2lineups";

    public MECLIPSE942LineupUsesP2LineupIT()
    {
        super( P2_LINEUP_REPO_ID );
    }

    @Test
    public void testP2lineup()
        throws Exception
    {
        uploadP2Lineup( "p2lineup_child.xml" );
        IP2Lineup p2LineupParent = uploadP2Lineup( "p2lineup_parent.xml" );

        File installDir = new File( "target/eclipse/MECLIPSE942" );

        installUsingP2( getP2RepoURL( p2LineupParent ), P2LineupHelper.getMasterInstallableUnitId( p2LineupParent ),
                        installDir.getCanonicalPath() );

        File feature = new File( installDir, "features/com.sonatype.nexus.p2.its.feature_1.0.0" );
        Assert.assertTrue( feature.exists() && feature.isDirectory() );

        File bundle = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" );
        Assert.assertTrue( bundle.canRead() );

        feature = new File( installDir, "features/com.sonatype.nexus.p2.its.feature2_1.0.0" );
        Assert.assertTrue( feature.exists() && feature.isDirectory() );
    }
}
