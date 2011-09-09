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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

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

import com.sonatype.nexus.p2.lineup.persist.InvalidP2GavException;
import com.sonatype.nexus.p2.lineup.persist.NoSuchP2LineupException;
import com.sonatype.nexus.p2.lineup.persist.P2AccessDeniedException;
import com.sonatype.nexus.p2.lineup.persist.P2ConfigurationException;
import com.sonatype.nexus.p2.lineup.persist.P2Gav;
import com.sonatype.nexus.p2.lineup.persist.P2LineupStorageException;
import com.sonatype.nexus.p2.lineup.resolver.CannotResolveP2LineupException;
import com.sonatype.nexus.p2.rest.model.P2LineupSummaryDto;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.p2lineup.model.io.xstream.XStreamUtil;
import com.thoughtworks.xstream.XStream;

/**
 * REST resource that controls CRUD operations for P2 Lineups. NOTE: Creation is done in
 * {@link P2LineupListPlexusResource}.
 * 
 * @author bdemers
 */
@Component( role = PlexusResource.class, hint = "P2LineupPlexusResource" )
@Path( P2LineupPlexusResource.RESOURCE_URI )
@Produces( { "application/xml" } )
@Consumes( { "application/xml" } )
public class P2LineupPlexusResource
    extends AbstractP2LineupPlexusResource
    implements PlexusResource
{

    public static final String LINEUP_KEY = "lineupId";

    private static final String DRY_RUN_PARAM_KEY = "dry-run";

    private static final String PRE_VALIDATE_PARAM_KEY = "pre-validate";

    private static final String RESOURCE_URI_PART = "/p2/lineups/";

    public static final String RESOURCE_URI = RESOURCE_URI_PART + "{" + LINEUP_KEY + "}";

    @Requirement
    private Logger logger;

    public P2LineupPlexusResource()
    {
        this.setModifiable( true );
    }

    @Override
    public Object getPayloadInstance()
    {
        return new P2Lineup();
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( RESOURCE_URI_PART + "/**", "authcBasic,perms[nexus:p2lineup]" );
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

    protected String getLineupId( Request request )
    {
        String groupFirstPart = (String) request.getAttributes().get( LINEUP_KEY );
        return groupFirstPart + request.getResourceRef().getRemainingPart( true, false );
    }

    /**
     * Returns a lineup based on the <code>lineupId</code>.
     * 
     * @param lineupId the Id of the lineup to return.
     */
    @Override
    @GET
    @ResourceMethodSignature( output = P2Lineup.class, pathParams = { @PathParam( value = "lineupId" ) } )
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        try
        {
            P2Gav gav = new P2Gav( this.getLineupId( request ) );
            P2Lineup lineup = this.getLineupManager().getLineup( gav );
            return lineup;
        }
        catch ( NoSuchP2LineupException e )
        {
            this.logger.debug( "P2 Lineup does not exist", e );
            throw new PlexusResourceException( Status.CLIENT_ERROR_NOT_FOUND, "P2 Lineup does not exist", e,
                                               this.createErrorResponse( e ) );
        }
        catch ( InvalidP2GavException e )
        {
            this.logger.debug( "Invalid P2 Gav.", e );
            throw new PlexusResourceException( Status.CLIENT_ERROR_BAD_REQUEST, "Invalid P2 Gav.", e,
                                               this.createErrorResponse( e ) );
        }
        catch ( P2ConfigurationException e )
        {
            this.logger.debug( "Could not read P2 Lineup.", e );
            throw new PlexusResourceException( Status.SERVER_ERROR_INTERNAL, "Could not read P2 Lineup.", e,
                                               this.createErrorResponse( e ) );
        }
        catch ( P2AccessDeniedException e )
        {
            throw new ResourceException( Status.CLIENT_ERROR_FORBIDDEN, e.getMessage() );
        }
    }

    /**
     * Updates a lineup based on the <code>lineupId</code> and the <code>payload</code>.
     * 
     * @param lineupId the Id of the lineup to return.
     * @param payload the new object to be updated.
     * @param dryRun the new object to be updated.
     */
    @Override
    @PUT
    @ResourceMethodSignature( input = P2Lineup.class, output = P2LineupSummaryDto.class, pathParams = { @PathParam( value = "lineupId" ) }, queryParams = { @QueryParam( value = "dry-run" ) } )
    public Object put( Context context, Request request, Response response, Object payload )
        throws ResourceException
    {
        P2Lineup lineup = null;
        // this wouldn't happen at runtime, but it could happen during test, so check the type
        if ( payload instanceof P2Lineup )
        {
            lineup = (P2Lineup) payload;
        }
        else
        {
            String errorMessage = "Object in payload is not a P2Lineup";
            this.logger.debug( "Object in payload is not a P2Lineup" );
            throw new PlexusResourceException( Status.CLIENT_ERROR_BAD_REQUEST, errorMessage,
                                               this.createErrorResponse( errorMessage ) );
        }

        try
        {

            P2Gav requestGav = new P2Gav( getLineupId( request ) );
            P2Gav lineupGav = new P2Gav( lineup );

            if ( !requestGav.equals( lineupGav ) )
            {
                String errorMessage =
                    "Request GAV (" + requestGav + ") doesn't match GAV inside lineup (" + lineupGav + ")";
                this.logger.debug( errorMessage );
                throw new PlexusResourceException( Status.CLIENT_ERROR_BAD_REQUEST, errorMessage,
                                                   this.createErrorResponse( errorMessage ) );
            }
            
            // check if it exists, if so update it, if not create it
            boolean update = true;
            try
            {
                this.getLineupManager().getLineup( requestGav );
                String errorMessage =
                    "Lineup coordinates " + requestGav + " cannot be modified. Use different coordinates to publish a new lineup.";
                throw new PlexusResourceException( Status.CLIENT_ERROR_BAD_REQUEST, errorMessage,
                                                   this.createErrorResponse( errorMessage ) );
            }
            catch ( NoSuchP2LineupException e )
            {
                //this is good case, if no lineup for gav, go ahead
                update = false;
            }

            // check to see if we are only doing a dry run
            // TODO: the validation logic needs improvement
            if ( this.isDryRun( request ) )
            {
                // the validation throws exceptions
                this.getLineupManager().validateLineup( lineup );

                // Success no content
                response.setStatus( Status.SUCCESS_NO_CONTENT );
                return null;
            }
            
            if( this.isPreValidate( request ) )
            {
               this.getLineupManager().validateAccess( lineup, update );
               
               // Success no content
               response.setStatus( Status.SUCCESS_NO_CONTENT );
               return null;
            }

            if( update )
            {
                try
                {
                    this.getLineupManager().updateLineup( lineup );
                }
                catch ( NoSuchP2LineupException e )
                {
                    // SHOULD NEVER HAPPEN, we just checked
                    this.getLogger().debug( "P2 Lineup: " + requestGav + " does not exist, creating it." );
                    this.getLineupManager().addLineup( lineup );
                }
            }
            else
            {
                this.getLineupManager().addLineup( lineup );
            }

            return newLineupSummary( request, lineup );
        }
        catch ( CannotResolveP2LineupException e )
        {
            this.logger.debug( "Could not Resolve P2 Lineup.", e );
            throw new PlexusResourceException( Status.CLIENT_ERROR_BAD_REQUEST, "Could not Resolve P2 Lineup.", e,
                                               this.createErrorResponse( e ) );
        }
        catch ( P2LineupStorageException e )
        {
            this.logger.debug( "Could not write P2 Lineup.", e );
            throw new PlexusResourceException( Status.SERVER_ERROR_INTERNAL, "Could not write P2 Lineup.", e,
                                               this.createErrorResponse( e ) );
        }
        catch ( InvalidP2GavException e )
        {
            this.logger.debug( "Invalid P2 Gav.", e );
            throw new PlexusResourceException( Status.CLIENT_ERROR_BAD_REQUEST, "Invalid P2 Gav.", e,
                                               this.createErrorResponse( e ) );
        }
        catch ( P2ConfigurationException e )
        {
            this.logger.debug( "Could not read P2 Lineup.", e );
            throw new PlexusResourceException( Status.SERVER_ERROR_INTERNAL, "Could not read P2 Lineup.", e,
                                               this.createErrorResponse( e ) );
        }
        catch ( P2AccessDeniedException e )
        {
            throw new ResourceException( Status.CLIENT_ERROR_FORBIDDEN, e.getMessage() );
        }
    }

    private boolean isPreValidate( Request request )
    {
        return Boolean.valueOf( request.getResourceRef().getQueryAsForm().getFirstValue( PRE_VALIDATE_PARAM_KEY, true ) );
    }

    private boolean isDryRun( Request request )
    {
        return Boolean.valueOf( request.getResourceRef().getQueryAsForm().getFirstValue( DRY_RUN_PARAM_KEY, true ) );
    }

    /**
     * Removes a lineup based on the <code>lineupId</code>.
     * 
     * @param lineupId the Id of the lineup to be removed.
     */
    @Override
    public void delete( Context context, Request request, Response response )
        throws ResourceException
    {
        try
        {
            P2Gav gav = new P2Gav( this.getLineupId( request ) );
            this.getLineupManager().deleteLineup( gav );
        }
        catch ( NoSuchP2LineupException e )
        {
            this.logger.debug( "P2 Lineup does not exist", e );
            throw new PlexusResourceException( Status.CLIENT_ERROR_NOT_FOUND, "P2 Lineup does not exist", e,
                                               this.createErrorResponse( e ) );
        }
        catch ( InvalidP2GavException e )
        {
            this.logger.debug( "Invalid P2 Gav.", e );
            throw new PlexusResourceException( Status.CLIENT_ERROR_BAD_REQUEST, "Invalid P2 Gav.", e,
                                               this.createErrorResponse( e ) );
        }
        catch ( P2LineupStorageException e )
        {
            this.logger.debug( "Could not write P2 Lineup.", e );
            throw new PlexusResourceException( Status.SERVER_ERROR_INTERNAL, "Could not write P2 Lineup.", e,
                                               this.createErrorResponse( e ) );
        }
        catch ( P2ConfigurationException e )
        {
            this.logger.debug( "Could not read P2 Lineup.", e );
            throw new PlexusResourceException( Status.SERVER_ERROR_INTERNAL, "Could not read P2 Lineup.", e,
                                               this.createErrorResponse( e ) );
        }
        catch ( P2AccessDeniedException e )
        {
            throw new ResourceException( Status.CLIENT_ERROR_FORBIDDEN, e.getMessage() );
        }
    }
}
