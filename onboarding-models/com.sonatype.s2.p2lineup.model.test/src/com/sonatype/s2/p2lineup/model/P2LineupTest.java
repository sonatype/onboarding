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
package com.sonatype.s2.p2lineup.model;

import junit.framework.TestCase;

public class P2LineupTest
    extends TestCase
{
    public void testDuplicateRepository()
    {
        P2Lineup lineup = new P2Lineup();
        lineup.addRepository( new P2LineupSourceRepository( "http://fakeurl" ) );
        assertEquals( 1, lineup.getRepositories().size() );
        lineup.addRepository( new P2LineupSourceRepository( "http://fakeurl" ) );
        assertEquals( 1, lineup.getRepositories().size() );
    }

    public void testDuplicateRootIU()
    {
        P2Lineup lineup = new P2Lineup();
        lineup.addRootInstallableUnit( new P2LineupInstallableUnit( "iuId", "1.2.3" ) );
        assertEquals( 1, lineup.getRootInstallableUnits().size() );
        lineup.addRootInstallableUnit( new P2LineupInstallableUnit( "iuId", "1.2.3" ) );
        assertEquals( 1, lineup.getRootInstallableUnits().size() );
    }

    public void testDuplicateTargetEnvironment()
    {
        P2Lineup lineup = new P2Lineup();
        lineup.addTargetEnvironment( new P2LineupTargetEnvironment( "win32", "win32", "x86" ) );
        assertEquals( 1, lineup.getTargetEnvironments().size() );
        lineup.addTargetEnvironment( new P2LineupTargetEnvironment( "win32", "win32", "x86" ) );
        assertEquals( 1, lineup.getTargetEnvironments().size() );
    }
}
