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
package com.sonatype.s2.project.validator.p2;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class P2TestSuite extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite( P2TestSuite.class.getName() );
        // suite.addTestSuite( SuccessfulUpdateTest.class );
        suite.addTestSuite( BrokenUpdateTest.class );
        // suite.addTestSuite(UpdateToDifferentLineup.class);
        suite.addTestSuite( EclipseInstallationValidatorTest.class );
        return suite;
    }
}
