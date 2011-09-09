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
package com.sonatype.nexus.proxy.p2.its.nxcm1998;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.sonatype.nexus.p2.lineup.persist.P2Gav;
import com.sonatype.nexus.proxy.p2.its.AbstractP2LineupIT;
import com.sonatype.s2.p2lineup.model.IP2Lineup;

public class NXCM1998P2GavMatchIT
    extends AbstractP2LineupIT
{
    private static final String P2_LINEUP_REPO_ID = "p2lineup1998";

    public NXCM1998P2GavMatchIT()
    {
        super( P2_LINEUP_REPO_ID );
    }

    @Test
    public void testP2lineup()
        throws Exception
    {
        IP2Lineup p2Lineup = loadP2Lineup( "p2lineup_MECLIPSE1998.xml" );

        XStreamRepresentation representation = new XStreamRepresentation(
            this.getXMLXStream(),
            "",
            MediaType.APPLICATION_XML );
        representation.setPayload( p2Lineup );
        P2Gav gav = new P2Gav( p2Lineup );
        Response r = null;
        try
        {
            r = RequestFacade.sendMessage( URI_PART + "/" + gav.toPathString(), Method.PUT, representation );
            Assert.assertTrue( r.getStatus().isSuccess() );
        }
        finally
        {
            RequestFacade.releaseResponse( r );
        }

        // different version, but same gav
        p2Lineup = loadP2Lineup( "p2lineup_MECLIPSE1998_2.xml" );
        representation = new XStreamRepresentation( this.getXMLXStream(), "", MediaType.APPLICATION_XML );
        representation.setPayload( p2Lineup );

        try
        {
            r = RequestFacade.sendMessage( URI_PART + "/" + gav.toPathString(), Method.PUT, representation );
            Assert.assertFalse( r.getStatus().isSuccess() );
        }
        finally
        {
            RequestFacade.releaseResponse( r );
        }
    }
}
