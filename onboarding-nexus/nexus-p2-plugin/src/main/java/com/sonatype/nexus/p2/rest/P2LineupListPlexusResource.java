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

import java.util.Collection;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;

import com.sonatype.nexus.p2.lineup.persist.P2ConfigurationException;
import com.sonatype.nexus.p2.rest.model.P2LineupListResponse;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.p2lineup.model.io.xstream.XStreamUtil;
import com.thoughtworks.xstream.XStream;

/**
 * REST resource that lists and creates P2 Lineups.
 * 
 * @author bdemers
 * @see P2LineupPlexusResource
 */
@Component( role = PlexusResource.class, hint = "P2LineupListPlexusResource" )
@Path( P2LineupListPlexusResource.RESOURCE_URI )
@Produces( { "application/xml" } )
@Consumes( { "application/xml" } )
public class P2LineupListPlexusResource
    extends AbstractP2LineupPlexusResource
    implements PlexusResource
{

    public static final String RESOURCE_URI = "/p2/lineups";

    @Requirement
    private Logger logger;

    @Override
    public Object getPayloadInstance()
    {
        return new P2Lineup();
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( RESOURCE_URI, "authcBasic,perms[nexus:p2lineup]" );
    }

    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    @Override
    public void configureXStream( XStream xstream )
    {
        super.configureXStream( xstream );
        XStreamUtil.initializeXStream( xstream );
    }

    /**
     * Returns a list of all linups
     * 
     * @param lineupId the Id of the lineup to return.
     */
    @Override
    @GET
    @ResourceMethodSignature( output = List.class )
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        try
        {   
            return this.convertP2Lineups( request, this.getLineupManager().getLineups() );
        }
        catch ( P2ConfigurationException e )
        {
            this.logger.debug( "Could not read P2 Lineup.", e );
            throw new PlexusResourceException( Status.SERVER_ERROR_INTERNAL, "Could not read P2 Lineup.", e, this.createErrorResponse( e ) );
        }
    }

    private P2LineupListResponse convertP2Lineups( Request request, Collection<P2Lineup> lineups )
        throws P2ConfigurationException
    {
        P2LineupListResponse response = new P2LineupListResponse();
        for ( P2Lineup lineup : lineups )
        {
            response.addData( newLineupSummary( request, lineup ) );

        }
        return response;
    }
}
