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
package com.sonatype.nexus.p2.rest;

import org.junit.Assert;
import org.junit.Test;

import org.junit.Test;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.sonatype.nexus.proxy.access.NexusItemAuthorizer;
import org.sonatype.plexus.rest.resource.PlexusResource;

import com.sonatype.nexus.p2.lineup.persist.AbstractP2LineupManagerTest;
import com.sonatype.nexus.p2.lineup.persist.MockNexusItemAuthorizer;
import com.sonatype.nexus.p2.lineup.persist.NoSuchP2LineupException;
import com.sonatype.nexus.p2.lineup.persist.P2Gav;
import com.sonatype.nexus.p2.lineup.persist.P2LineupManager;
import com.sonatype.s2.p2lineup.model.P2Lineup;

public class PreValidationTest
    extends AbstractP2LineupManagerTest
{
    @Test
    public void testPreValidation()
        throws Exception
    {

        P2Lineup lineup = new P2Lineup();
        lineup.setGroupId( "groupId-testPreValidation" );
        lineup.setId( "id-testGettestPreValidationLineup" );
        lineup.setVersion( "version-testPreValidation" );

        PlexusResource lineupResource = this.lookup( PlexusResource.class, "P2LineupPlexusResource" );

        Request request = new Request();
        Response response = new Response( request );

        request.setResourceRef( new Reference( "/id-testGettestPreValidationLineup/version-testPreValidation" ) );
        request.getResourceRef().addQueryParameter( "pre-validate", "true" );
        request.getAttributes().put( "lineupId", "groupId-testPreValidation" );

        lineupResource.put( null, request, response, lineup );
        try
        {
            this.lookup( P2LineupManager.class ).getLineup( new P2Gav( lineup ) );
            Assert.fail( "expected NoSuchP2LineupException" );
        }
        catch( NoSuchP2LineupException e)
        {
            // expected
        }

        MockNexusItemAuthorizer itemAuthorizer = (MockNexusItemAuthorizer) this.lookup( NexusItemAuthorizer.class );
        itemAuthorizer.setAuthorized( false );

        try
        {
            lineupResource.put( null, request, response, lineup );
            Assert.fail( "expected ResourceException" );
        }
        catch ( ResourceException e )
        {
            Assert.assertEquals( Status.CLIENT_ERROR_FORBIDDEN, e.getStatus() );
        }
    }
}
