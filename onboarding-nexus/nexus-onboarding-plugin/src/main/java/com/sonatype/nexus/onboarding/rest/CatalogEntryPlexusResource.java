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

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import com.sonatype.nexus.onboarding.persist.model.CCatalogEntry;
import com.sonatype.nexus.onboarding.rest.dto.CatalogEntryRequest;

@Component( role = PlexusResource.class, hint = "CatalogEntryPlexusResource" )
@Path( CatalogEntryPlexusResource.RESOURCE_URI )
@Produces( { "application/xml" } )
@Consumes( { "application/xml" } )
public class CatalogEntryPlexusResource
    extends AbstractOnboardingCatalogPlexusResource
    implements PlexusResource
{

    private static final String CATALOG_ENTRY_KEY = "entry-id";

    public static final String RESOURCE_URI =
        RESOURCE_URI_PART + "/{" + CatalogPlexusResource.CATALOG_KEY + "}/entries/{" + CATALOG_ENTRY_KEY + "}";

    public CatalogEntryPlexusResource()
    {
        this.setModifiable( true );
    }

    @Override
    public Object getPayloadInstance()
    {
        return new CatalogEntryRequest();
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( RESOURCE_URI_PART + "/**/entries/**", "authcBasic,perms[nexus:catalog]" );
    }

    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    protected static String getCatalogEntryId( Request request )
    {
        return (String) request.getAttributes().get( CATALOG_ENTRY_KEY );
    }

    @Override
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        String catalogId = CatalogPlexusResource.getCatalogId( request );
        String entryId = getCatalogEntryId( request );

        try
        {
            CCatalogEntry entry = catalogCfg.readCatalogEntry( catalogId, entryId );
            return toRest( entry );
        }
        catch ( InvalidConfigurationException e )
        {
            handleConfigurationException( e );
            return null;
        }
    }

    @Override
    @PUT
    @ResourceMethodSignature( input = CatalogEntryRequest.class, output = CatalogEntryRequest.class )
    public Object put( Context context, Request request, Response response, Object payload )
        throws ResourceException
    {
        try
        {
            String id = CatalogPlexusResource.getCatalogId( request );
            String entryId = getCatalogEntryId( request );
            CCatalogEntry model = toModel( (CatalogEntryRequest) payload );
            catalogCfg.updateCatalogEntry( id, entryId, model );
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
        String catalogId = CatalogPlexusResource.getCatalogId( request );
        String entryId = getCatalogEntryId( request );

        try
        {
            catalogCfg.removeCatalogEntry( catalogId, entryId );
            throw new ResourceException( 204 );
        }
        catch ( InvalidConfigurationException e )
        {
            handleConfigurationException( e );
            return;
        }
    }

}
