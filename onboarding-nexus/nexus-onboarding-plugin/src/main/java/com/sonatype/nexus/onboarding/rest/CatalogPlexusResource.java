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
package com.sonatype.nexus.onboarding.rest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import com.sonatype.nexus.onboarding.persist.model.CCatalog;
import com.sonatype.nexus.onboarding.persist.model.CCatalogEntry;
import com.sonatype.nexus.onboarding.rest.dto.CatalogEntryRequest;
import com.sonatype.nexus.onboarding.rest.dto.CatalogRequest;

@Component( role = PlexusResource.class, hint = "CatalogPlexusResource" )
@Path( CatalogPlexusResource.RESOURCE_URI )
@Produces( { "application/xml" } )
@Consumes( { "application/xml" } )
public class CatalogPlexusResource
    extends AbstractOnboardingCatalogPlexusResource
    implements PlexusResource
{
    public static final String CATALOG_KEY = "catalog-id";

    public static final String RESOURCE_URI = RESOURCE_URI_PART + "/{" + CATALOG_KEY + "}";

    public CatalogPlexusResource()
    {
        this.setModifiable( true );
    }

    @Override
    public Object getPayloadInstance()
    {
        return null;
    }

    @Override
    public Object getPayloadInstance( Method method )
    {
        if ( Method.PUT.equals( method ) )
        {
            return new CatalogRequest();
        }
        if ( Method.POST.equals( method ) )
        {
            return new CatalogEntryRequest();
        }
        return null;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( RESOURCE_URI_PART + "/**", "authcBasic,perms[nexus:catalog]" );
    }

    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    protected static String getCatalogId( Request request )
    {
        return (String) request.getAttributes().get( CATALOG_KEY );
    }

    @Override
    @GET
    @ResourceMethodSignature( output = List.class )
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        try
        {
            String id = getCatalogId( request );
            CCatalog catalog = catalogCfg.readCatalog( id );
            return toRest( catalog );
        }
        catch ( InvalidConfigurationException e )
        {
            handleConfigurationException( e );
            return null;
        }
    }

    @Override
    @PUT
    @ResourceMethodSignature( input = CatalogRequest.class, output = CatalogRequest.class )
    public Object put( Context context, Request request, Response response, Object payload )
        throws ResourceException
    {
        try
        {
            CCatalog model = toModel( (CatalogRequest) payload );
            catalogCfg.createOrUpdateCatalog( model );
            return toRest( model );
        }
        catch ( InvalidConfigurationException e )
        {
            handleConfigurationException( e );
            return null;
        }
    }

    @Override
    public void delete( Context context, Request request, Response response )
        throws ResourceException
    {
        String id = getCatalogId( request );
        try
        {
            catalogCfg.deleteCatalog( id );
            throw new ResourceException( 204 );
        }
        catch ( InvalidConfigurationException e )
        {
            handleConfigurationException( e );
            return;
        }

    }

    @Override
    @POST
    @ResourceMethodSignature( input = CatalogEntryRequest.class, output = CatalogEntryRequest.class )
    public Object post( Context context, Request request, Response response, Object payload )
        throws ResourceException
    {
        String id = CatalogPlexusResource.getCatalogId( request );
        try
        {
            CCatalogEntry model = toModel( (CatalogEntryRequest) payload );
            catalogCfg.addCatalogEntry( id, model );
            return toRest( model );
        }
        catch ( InvalidConfigurationException e )
        {
            handleConfigurationException( e );
            return null;
        }
    }
}
