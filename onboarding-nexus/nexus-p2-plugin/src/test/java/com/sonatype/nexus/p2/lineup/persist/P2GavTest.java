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
package com.sonatype.nexus.p2.lineup.persist;

import org.junit.Assert;
import org.junit.Test;

public class P2GavTest
{

    @Test
    public void testEndsWithSlash()
        throws InvalidP2GavException
    {
        P2Gav gav = new P2Gav( "group/valid/artifact/version//" );
        Assert.assertEquals( "group.valid", gav.getGroupId() );
        Assert.assertEquals( "artifact", gav.getId() );
        Assert.assertEquals( "version", gav.getVersion() );
    }

    @Test
    public void testInvalidGav()
        throws InvalidP2GavException
    {
        try
        {
            new P2Gav( "/artifact/version" );
            Assert.fail( "Expected InvalidP2GavException" );
        }
        catch ( InvalidP2GavException e )
        {
            // expected
        }
    }
}
