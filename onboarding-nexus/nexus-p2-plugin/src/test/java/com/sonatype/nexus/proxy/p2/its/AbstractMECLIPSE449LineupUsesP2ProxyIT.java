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

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupHelper;

/**
 * Some artifact repositories contain packed artifacts but cannot handle them correctly (i.e. they don't have rules for
 * packed format). This IT tests that if the same artifact is in both packed and unpacked formats in the repositories,
 * the p2 lineup resolution and the installation from the p2 lineup does not fail.
 */
public abstract class AbstractMECLIPSE449LineupUsesP2ProxyIT
    extends AbstractP2LineupIT
{
    private static final String P2_LINEUP_REPO_ID = "p2lineup0449";

    protected String getP2LineupDestinationPath()
    {
        return "/test" + getTestType();
    }

    protected abstract String getTestType();

    public AbstractMECLIPSE449LineupUsesP2ProxyIT()
    {
        super( P2_LINEUP_REPO_ID );
    }

    @Test
    public void testP2lineup()
        throws Exception
    {
        IP2Lineup p2Lineup = uploadP2Lineup( "p2lineup_MECLIPSE449" + getTestType() + ".xml" );

        File installDir = new File( "target/eclipse/meclipse0449" + getTestType() );

        installUsingP2( getP2RepoURL( p2Lineup ), P2LineupHelper.getMasterInstallableUnitId( p2Lineup ),
                        installDir.getCanonicalPath() );

        File bundle = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" );
        Assert.assertTrue( bundle.canRead() );
    }
}
