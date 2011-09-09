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

import com.sonatype.nexus.onboarding.persist.model.CSecurityRealm;
import com.sonatype.s2.securityrealm.model.S2SecurityRealm;

@Component( role = PlexusResource.class, hint = "SecurityRealmPlexusResource" )
@Path( SecurityRealmPlexusResource.RESOURCE_URI )
@Produces( { "application/xml" } )
@Consumes( { "application/xml" } )
public class SecurityRealmPlexusResource
    extends AbstractOnboardingSecurityRealmPlexusResource
    implements PlexusResource
{
    private static final String RESOURCE_URI_PART = "/mse/realms";

    public static final String REALM_KEY = "realm-id";

    public static final String RESOURCE_URI = RESOURCE_URI_PART + "/{" + REALM_KEY + "}";

    public SecurityRealmPlexusResource()
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
            return new S2SecurityRealm();
        }
        return null;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( RESOURCE_URI_PART + "/*",
                                             "authcBasic,perms[nexus:onboarding-security-realm]" );
    }

    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    protected static String getRealmId( Request request )
    {
        return (String) request.getAttributes().get( REALM_KEY );
    }

    /**
     * Returns the onboarding security realm identified by the specified <code>realm-id</code>.
     * 
     * @param realm-id The id of the onboarding security realm to return.
     * @return The onboarding security realm identified by the specified <code>realm-id</code>.
     */
    @Override
    @GET
    @ResourceMethodSignature( output = S2SecurityRealm.class, pathParams = { @PathParam( value = "realm-id" ) } )
    public S2SecurityRealm get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        try
        {
            String realmId = getRealmId( request );
            CSecurityRealm realm = configuration.readRealm( realmId );
            return toRest( realm );
        }
        catch ( InvalidConfigurationException e )
        {
            handleConfigurationException( e );
            return null;
        }
    }

    /**
     * Creates or updates an onboarding security realm based on the <code>realm-id</code> and <code>payload</code>.
     * 
     * @param realm-id the id of the object to be created or updated.
     * @param payload the object to be created or updated.
     * @return Null
     */
    @Override
    @PUT
    @ResourceMethodSignature( input = S2SecurityRealm.class, pathParams = { @PathParam( value = "realm-id" ) } )
    public Object put( Context context, Request request, Response response, Object payload )
        throws ResourceException
    {
        try
        {
            String realmId = getRealmId( request );
            S2SecurityRealm realm = (S2SecurityRealm) payload;
            if ( !realmId.equals( realm.getId() ) )
            {
                throw new InvalidConfigurationException( "Request id (" + realmId
                    + ") does not match the id in the payload (" + realm.getId() + ")" );
            }
            CSecurityRealm cRealm = toModel( realm );
            configuration.createOrUpdateRealm( cRealm );
        }
        catch ( InvalidConfigurationException e )
        {
            handleConfigurationException( e );
        }
        return null;
    }

    /**
     * Deletes an onboarding security realm based on the specified <code>realm-id</code>.
     * 
     * @param realm-id the id of the object to be deleted.
     */
    @Override
    @DELETE
    @ResourceMethodSignature( pathParams = { @PathParam( value = "realm-id" ) } )
    public void delete( Context context, Request request, Response response )
        throws ResourceException
    {
        String realmId = getRealmId( request );
        try
        {
            configuration.deleteRealm( realmId );
            throw new ResourceException( 204 );
        }
        catch ( InvalidConfigurationException e )
        {
            handleConfigurationException( e );
            return;
        }
    }
}
