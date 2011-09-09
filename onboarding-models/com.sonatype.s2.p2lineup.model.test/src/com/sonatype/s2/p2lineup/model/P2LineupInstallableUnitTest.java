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

public class P2LineupInstallableUnitTest
    extends TestCase
{
    public void testDuplicateTargetEnvironment()
    {
        P2LineupInstallableUnit iu = new P2LineupInstallableUnit();
        iu.addTargetEnvironment( new P2LineupTargetEnvironment( "win32", "win32", "x86" ) );
        assertEquals( 1, iu.getTargetEnvironments().size() );
        iu.addTargetEnvironment( new P2LineupTargetEnvironment( "win32", "win32", "x86" ) );
        assertEquals( 1, iu.getTargetEnvironments().size() );
    }
}
