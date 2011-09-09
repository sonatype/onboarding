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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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

import com.sonatype.nexus.onboarding.persist.model.CSecurityRealmURLAssoc;
import com.sonatype.s2.securityrealm.model.S2SecurityRealmURLAssoc;

@Component( role = PlexusResource.class, hint = "SecurityRealmURLAssocPlexusResource" )
@Path( SecurityRealmURLAssocPlexusResource.RESOURCE_URI )
@Produces( { "application/xml" } )
@Consumes( { "application/xml" } )
public class SecurityRealmURLAssocPlexusResource
    extends AbstractOnboardingSecurityRealmPlexusResource
    implements PlexusResource
{
    protected static final String RESOURCE_URI_PART = "/mse/urls";

    public static final String URL_KEY = "url-id";

    public static final String RESOURCE_URI = RESOURCE_URI_PART + "/{" + URL_KEY + "}";

    public SecurityRealmURLAssocPlexusResource()
    {
        setModifiable( true );
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
            return new S2SecurityRealmURLAssoc();
        }
        return null;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( RESOURCE_URI_PART + "/*",
                                             "authcBasic,perms[nexus:onboarding-security-realm-url]" );
    }

    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    protected static String getUrlId( Request request )
    {
        return (String) request.getAttributes().get( URL_KEY );
    }

    /**
     * Returns the onboarding security realm to URL association identified by the specified <code>url-id</code>.
     * 
     * @param url-id The id of the onboarding security realm to URL association to return.
     * @return The onboarding security realm to URL association identified by the specified <code>url-id</code>.
     */
    @Override
    @GET
    @ResourceMethodSignature( output = S2SecurityRealmURLAssoc.class, pathParams = { @PathParam( value = "url-id" ) } )
    public S2SecurityRealmURLAssoc get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        try
        {
            String urlId = getUrlId( request );
            CSecurityRealmURLAssoc url = configuration.readURL( urlId );
            return toRest( url );
        }
        catch ( InvalidConfigurationException e )
        {
            handleConfigurationException( e );
            return null;
        }
    }

    /**
     * Updates an onboarding security realm to URL association based on the <code>url-id</code> and <code>payload</code>
     * 
     * @param url-id the id of the object to be updated. Must match the id inside <code>payload</code>.
     * @param payload the object to be updated.
     * @return Null
     */
    @Override
    @PUT
    @ResourceMethodSignature( input = S2SecurityRealmURLAssoc.class, pathParams = { @PathParam( value = "url-id" ) } )
    public Object put( Context context, Request request, Response response, Object payload )
        throws ResourceException
    {
        try
        {
            String urlId = getUrlId( request );
            S2SecurityRealmURLAssoc urlAssoc = (S2SecurityRealmURLAssoc) payload;
            if ( !urlId.equals( urlAssoc.getId() ) )
            {
                throw new InvalidConfigurationException( "Request id (" + urlId
                    + ") does not match the id in the payload (" + urlAssoc.getId() + ")" );
            }
            CSecurityRealmURLAssoc url = toModel( urlAssoc );
            configuration.updateURL( url );
        }
        catch ( InvalidConfigurationException e )
        {
            handleConfigurationException( e );
        }
        return null;
    }

    /**
     * Deletes an an onboarding security realm to URL association based on the specified <code>url-id</code>.
     * 
     * @param url-id the id of the object to be deleted.
     */
    @Override
    @DELETE
    @ResourceMethodSignature( pathParams = { @PathParam( value = "url-id" ) } )
    public void delete( Context context, Request request, Response response )
        throws ResourceException
    {
        String urlId = getUrlId( request );
        try
        {
            configuration.deleteURL( urlId );
            throw new ResourceException( 204 );
        }
        catch ( InvalidConfigurationException e )
        {
            handleConfigurationException( e );
            return;
        }
    }
}
