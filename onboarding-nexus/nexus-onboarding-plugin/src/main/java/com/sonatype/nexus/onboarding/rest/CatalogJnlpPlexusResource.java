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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import com.sonatype.nexus.onboarding.JnlpTemplateUtil;
import com.sonatype.nexus.onboarding.project.repository.OnboardingProjectRepository;

@Component( role = PlexusResource.class, hint = "CatalogJnlpPlexusResource" )
@Path( CatalogJnlpPlexusResource.RESOURCE_URI )
@Produces( { "application/xml" } )
@Consumes( { "application/xml" } )
public class CatalogJnlpPlexusResource
    extends AbstractOnboardingCatalogPlexusResource
    implements PlexusResource
{
    public static final String RESOURCE_URI =
        RESOURCE_URI_PART + "/{" + CatalogPlexusResource.CATALOG_KEY + "}/catalog.jnlp";

    @Requirement
    private GlobalRestApiSettings globalRestApiSettings;

    public CatalogJnlpPlexusResource()
    {
        this.setModifiable( true );
    }

    @Override
    public Object getPayloadInstance()
    {
        return null;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( RESOURCE_URI_PART + "/catalog.jnlp", "authcBasic,perms[nexus:catalog]" );
    }

    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    @Override
    @GET
    @ResourceMethodSignature( output = List.class )
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        String id = CatalogPlexusResource.getCatalogId( request );
        try
        {
            // just making sure the catalog exits!
            catalogCfg.readCatalog( id );

            String baseUrl = request.getRootRef().toString();

            if ( StringUtils.isEmpty( baseUrl ) )
            {
                baseUrl = this.globalRestApiSettings.getBaseUrl();
                if ( StringUtils.isEmpty( baseUrl ) )
                {
                    throw new ResourceException( Status.CLIENT_ERROR_BAD_REQUEST,
                                                 "Cannot generate installer jnlp: Base URL is not set." );
                }
            }
            this.getLogger().debug( "baseURL=" + baseUrl );

            // use catalog URL with ID
            String codebaseURL = request.getResourceRef().toString().replace( "/catalog.jnlp", "" );

            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put( "codebaseURL", codebaseURL );
            properties.put( "generationDate", new Date().toString() );
            properties.put( "s2installerURL", baseUrl );
            properties.put( "osgiInstallTempArea", ".mse/installer/" + System.currentTimeMillis() );
            properties.put( "descriptorToInstall", "" );
            properties.put( "user.home", System.getProperty( "user.home" ) );

            final String processedTemplate =
                JnlpTemplateUtil.processJnlpTemplate( OnboardingProjectRepository.INSTALL_TEMPLATE_JNLP, properties );

            return new OutputRepresentation( MediaType.APPLICATION_JNLP )
            {
                @Override
                public void write( OutputStream out )
                    throws IOException
                {
                    out.write( processedTemplate.getBytes() );
                    out.flush();
                }
            };
        }
        catch ( InvalidConfigurationException e )
        {
            handleConfigurationException( e );
            return null;
        }
        catch ( IOException e )
        {
            throw new ResourceException( Status.SERVER_ERROR_INTERNAL, e );
        }
    }
}
