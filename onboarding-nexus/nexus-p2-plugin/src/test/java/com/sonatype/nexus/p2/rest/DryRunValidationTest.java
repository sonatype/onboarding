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
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;

import com.sonatype.nexus.p2.lineup.persist.AbstractP2LineupManagerTest;
import com.sonatype.nexus.p2.lineup.persist.MockP2LineupResolver;
import com.sonatype.nexus.p2.lineup.resolver.CannotResolveP2LineupException;
import com.sonatype.nexus.p2.lineup.resolver.P2LineupResolver;
import com.sonatype.nexus.p2.rest.model.P2LineupErrorResponse;
import com.sonatype.s2.p2lineup.model.P2Lineup;

public class DryRunValidationTest
    extends AbstractP2LineupManagerTest
{
    @Test
    public void testDryRunWithErrors()
        throws Exception
    {
        MockP2LineupResolver mockResolver = (MockP2LineupResolver) this.lookup( P2LineupResolver.class );

        CannotResolveP2LineupException exception = new CannotResolveP2LineupException();
        mockResolver.setFailValidation( exception );
        exception.addUnresolvedInstallableUnit( "installableUnitId1", "installableUnitVersion1", "Error Number One" );
        exception.addUnresolvedInstallableUnit( "installableUnitId2", "installableUnitVersion2", "Error Number Two" );
        exception.addRepositoryError( "http://invalid.com", "Cannot resolve repository on Nexus server." );
        exception.addRepositoryError( "http://foobar.com", "Cannot resolve repository on Nexus server." );

        P2Lineup lineup = new P2Lineup();
        lineup.setDescription( "description" );
        lineup.setGroupId( "groupId-testDryRunWithErrors" );
        lineup.setId( "id-testGettestDryRunWithErrorsLineup" );
        lineup.setVersion( "version-testDryRunWithErrors" );

        PlexusResource lineupResource = this.lookup( PlexusResource.class, "P2LineupPlexusResource" );

        Request request = new Request();
        Response response = new Response( request );

        request.setResourceRef( new Reference( "/id-testGettestDryRunWithErrorsLineup/version-testDryRunWithErrors" ) );
        request.getResourceRef().addQueryParameter( "dry-run", "true" );
        request.getAttributes().put( "lineupId", "groupId-testDryRunWithErrors" );

        try
        {
            lineupResource.put( null, request, response, lineup );
            Assert.fail( "expected PlexusResourceException" );
        }
        catch ( PlexusResourceException e )
        {
            P2LineupErrorResponse errorResponse = (P2LineupErrorResponse) e.getResultObject();
            Assert.assertEquals( 4, errorResponse.getErrors().size() );
        }
    }
}
