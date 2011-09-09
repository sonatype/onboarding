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

import com.sonatype.nexus.onboarding.persist.model.CSecurityRealmURLAssoc;
import com.sonatype.s2.securityrealm.model.S2SecurityRealmURLAssoc;

@Component( role = PlexusResource.class, hint = "SecurityRealmURLAssocListPlexusResource" )
@Path( SecurityRealmURLAssocListPlexusResource.RESOURCE_URI )
@Produces( { "application/xml" } )
@Consumes( { "application/xml" } )
public class SecurityRealmURLAssocListPlexusResource
    extends AbstractOnboardingSecurityRealmPlexusResource
    implements PlexusResource
{
    private static final String RESOURCE_URI_PART = "/mse/urls";

    public static final String RESOURCE_URI = RESOURCE_URI_PART;

    public SecurityRealmURLAssocListPlexusResource()
    {
        setModifiable( true );
    }

    @Override
    public Object getPayloadInstance()
    {
        return new S2SecurityRealmURLAssoc();
    }

    @Override
    public Object getPayloadInstance( Method method )
    {
        if ( Method.POST.equals( method ) )
        {
            return new S2SecurityRealmURLAssoc();
        }
        return null;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( RESOURCE_URI, "authcBasic,perms[nexus:onboarding-security-realm-url]" );
    }

    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    /**
     * Returns a list of all URLs associated with onboarding security realms.
     * 
     * @return List of all URLs associated with onboarding security realms.
     */
    @Override
    @GET
    @ResourceMethodSignature( output = List.class )
    public List<S2SecurityRealmURLAssoc> get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        return urlAssocListToRest( configuration.listURLs() );
    }

    /**
     * Creates an onboarding security realm to URL association based on the <code>payload</code>.
     * 
     * @param payload the object to be created.
     * @return The newly created onboarding security realm to URL association.
     */
    @Override
    @POST
    @ResourceMethodSignature( input = S2SecurityRealmURLAssoc.class, output = S2SecurityRealmURLAssoc.class )
    public S2SecurityRealmURLAssoc post( Context context, Request request, Response response, Object payload )
        throws ResourceException
    {
        try
        {
            CSecurityRealmURLAssoc url = toModel( (S2SecurityRealmURLAssoc) payload );
            url = configuration.createURL( url );
            return toRest( url );
        }
        catch ( InvalidConfigurationException e )
        {
            handleConfigurationException( e );
        }
        return null;
    }
}
