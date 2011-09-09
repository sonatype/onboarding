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

public class P2LineupHelperTest
    extends TestCase
{
    public void testMasterInstallableUnitId()
    {
        String groupId = "com.mycompany.mylineupgroupid";
        String artifactId = "mylineupartifactid";
        
        IP2Lineup lineup = new P2Lineup();
        try
        {
            P2LineupHelper.getMasterInstallableUnitId( lineup );
            fail( "Expected IllegalArgumentException" );
        }
        catch ( IllegalArgumentException expected )
        {
            if ( !"Lineup group id cannot be null or empty".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
        
        lineup.setGroupId( groupId );
        try
        {
            P2LineupHelper.getMasterInstallableUnitId( lineup );
            fail( "Expected IllegalArgumentException" );
        }
        catch ( IllegalArgumentException expected )
        {
            if ( !"Lineup artifact id cannot be null or empty".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }

        lineup.setId( artifactId );
        String masterIUId = P2LineupHelper.getMasterInstallableUnitId( lineup );
        assertEquals( groupId + "." + artifactId + ".p2Lineup", masterIUId );
    }
}
