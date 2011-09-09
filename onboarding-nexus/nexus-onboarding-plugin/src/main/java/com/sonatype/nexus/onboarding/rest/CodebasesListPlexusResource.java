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

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.walker.AbstractFileWalkerProcessor;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.Walker;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.rest.NoSuchRepositoryAccessException;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import com.sonatype.nexus.onboarding.project.repository.OnboardingProjectRepository;
import com.sonatype.nexus.onboarding.rest.dto.CatalogDTO;
import com.sonatype.nexus.onboarding.rest.dto.CatalogEntryDTO;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.catalog.ProjectCatalogEntry;
import com.sonatype.s2.project.model.descriptor.Project;
import com.sonatype.s2.project.model.descriptor.io.xpp3.S2ProjectDescriptorXpp3Reader;

@Component( role = PlexusResource.class, hint = "CodebasesListPlexusResource" )
@Path( CodebasesListPlexusResource.RESOURCE_URI )
@Produces( { "application/xml" } )
@Consumes( { "application/xml" } )
public class CodebasesListPlexusResource
    extends AbstractOnboardingPlexusResource
{
    public static final String REPOSITORY_ID_KEY = "repositoryId";

    public static final String RESOURCE_URI = "/mse/codebases/{" + REPOSITORY_ID_KEY + "}";

    @Requirement
    private RepositoryRegistry repositoryRegistry;

    @Requirement
    private Walker walker;

    @Override
    public ProjectCatalogEntry getPayloadInstance()
    {
        return new ProjectCatalogEntry();
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( "/mse/codebases/*", "authcBasic,perms[nexus:codebasis]" );
    }

    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    @Override
    @GET
    @ResourceMethodSignature( output = CatalogDTO.class )
    public CatalogDTO get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        String repoId = (String) request.getAttributes().get( REPOSITORY_ID_KEY );

        Repository repository;
        try
        {
            repository = repositoryRegistry.getRepository( repoId );
        }
        catch ( NoSuchRepositoryAccessException e )
        {
            throw new ResourceException( 403, e );
        }
        catch ( NoSuchRepositoryException e )
        {
            throw new ResourceException( 400, e );
        }

        if ( !( repository instanceof OnboardingProjectRepository ) )
        {
            throw new ResourceException( 400 );
        }

        final CatalogDTO catalog = new CatalogDTO();
        catalog.setName( repoId );

        ResourceStoreRequest storeRequest = new ResourceStoreRequest( "/", true );
        DefaultWalkerContext ctx = new DefaultWalkerContext( repository, storeRequest );


        ctx.getProcessors().add( new AbstractFileWalkerProcessor()
        {
            @Override
            protected void processFileItem( WalkerContext context, StorageFileItem fItem )
                throws Exception
            {
                if ( IS2Project.PROJECT_DESCRIPTOR_FILENAME.equals( fItem.getName() ) )
                {
                    Project codebase;
                    InputStream is = fItem.getInputStream();
                    try
                    {
                        S2ProjectDescriptorXpp3Reader reader = new S2ProjectDescriptorXpp3Reader();
                        codebase = reader.read( is );
                    }
                    finally
                    {
                        IOUtil.close( is );
                    }

                    CatalogEntryDTO entry = new CatalogEntryDTO();
                    entry.setUrl( "." + fItem.getParentPath() );
                    entry.setGroupId( codebase.getGroupId() );
                    entry.setId( codebase.getArtifactId() );
                    entry.setVersion( codebase.getVersion() );
                    entry.setName( codebase.getName() );
                    catalog.addEntry( entry );
                }
            }
        } );

        walker.walk( ctx );

        return catalog;
    }

}
