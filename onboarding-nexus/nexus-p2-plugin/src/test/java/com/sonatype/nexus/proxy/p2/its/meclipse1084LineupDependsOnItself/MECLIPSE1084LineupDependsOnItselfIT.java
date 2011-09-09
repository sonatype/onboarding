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
package com.sonatype.nexus.proxy.p2.its.meclipse1084LineupDependsOnItself;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.nexus.p2.rest.model.P2LineupError;
import com.sonatype.nexus.p2.rest.model.P2LineupErrorResponse;
import com.sonatype.nexus.p2.rest.model.P2LineupRepositoryError;
import com.sonatype.nexus.p2.rest.model.P2LineupUnresolvedInstallableUnit;
import com.sonatype.nexus.proxy.p2.its.AbstractP2LineupIT;
import com.sonatype.s2.p2lineup.model.IP2Lineup;

public class MECLIPSE1084LineupDependsOnItselfIT
    extends AbstractP2LineupIT
{
    public MECLIPSE1084LineupDependsOnItselfIT()
    {
        super( IP2Lineup.LINEUP_REPOSITORY_ID );
    }

    @Test
    public void lineupDependsOnItselfAsInstallableUnit()
        throws Exception
    {
        //uploadP2Lineup( "p2lineup_MECLIPSE1084.xml" );

        P2LineupErrorResponse p2LineupSecondVersion =
            uploadInvalidP2Lineup( "p2lineup_0.0.2_MECLIPSE1084_IUDependency.xml" );
        List<P2LineupError> errors = p2LineupSecondVersion.getErrors();
        Assert.assertNotNull( errors );
        Assert.assertEquals( errors.toString(), 1, errors.size() );
        Assert.assertEquals( "A lineup cannot depend on itself or another version of itself.",
                             errors.get( 0 ).getErrorMessage() );
        Assert.assertTrue( errors.get( 0 ).getClass().getCanonicalName(),
                           errors.get( 0 ) instanceof P2LineupUnresolvedInstallableUnit );
    }

    @Test
    public void lineupDependsOnItselfAsRepository()
        throws Exception
    {
        P2LineupErrorResponse p2LineupSecondVersion =
            uploadInvalidP2Lineup( "p2lineup_0.0.2_MECLIPSE1084_RepoDependency.xml" );
        List<P2LineupError> errors = p2LineupSecondVersion.getErrors();
        Assert.assertNotNull( errors );
        Assert.assertEquals( errors.toString(), 1, errors.size() );
        Assert.assertEquals( "A lineup cannot depend on itself or another version of itself.",
                             errors.get( 0 ).getErrorMessage() );
        Assert.assertTrue( errors.get( 0 ).getClass().getCanonicalName(),
                           errors.get( 0 ) instanceof P2LineupRepositoryError );
    }
}
