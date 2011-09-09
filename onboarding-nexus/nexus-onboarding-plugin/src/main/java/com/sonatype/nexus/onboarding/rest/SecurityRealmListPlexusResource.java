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
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import com.sonatype.s2.securityrealm.model.S2SecurityRealm;

@Component( role = PlexusResource.class, hint = "SecurityRealmListPlexusResource" )
@Path( SecurityRealmListPlexusResource.RESOURCE_URI )
@Produces( { "application/xml" } )
@Consumes( { "application/xml" } )
public class SecurityRealmListPlexusResource
    extends AbstractOnboardingSecurityRealmPlexusResource
    implements PlexusResource
{
    private static final String RESOURCE_URI_PART = "/mse/realms";

    public static final String RESOURCE_URI = RESOURCE_URI_PART;

    @Override
    public Object getPayloadInstance()
    {
        return new S2SecurityRealm();
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( RESOURCE_URI, "authcBasic,perms[nexus:onboarding-security-realm]" );
    }

    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    /**
     * Returns a list of all onboarding security realms.
     * 
     * @return List of all onboarding security realms.
     */
    @Override
    @GET
    @ResourceMethodSignature( output = List.class )
    public List<S2SecurityRealm> get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        return toRest( configuration.listRealms() );
    }
}
