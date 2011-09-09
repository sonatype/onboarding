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
package com.sonatype.nexus.p2.lineup.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.index.artifact.Gav;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.IllegalRequestException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.DefaultStorageLinkItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.ArtifactStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.MetadataManager;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.AbstractRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.local.fs.FileContentLocator;

import com.sonatype.nexus.p2.P2Constants;
import com.sonatype.nexus.p2.lineup.persist.P2LineupManager;
import com.sonatype.nexus.p2.lineup.resolver.CannotResolveP2LineupException;
import com.sonatype.nexus.p2.lineup.resolver.P2LineupResolver;
import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Reader;

@Component( role = Repository.class, hint = P2LineupContentClass.ID, instantiationStrategy = "per-lookup", description = "P2 Lineup" )
public class P2LineupRepository
    extends AbstractRepository
    implements HostedRepository, Repository
{

    @Requirement( hint = P2LineupContentClass.ID )
    private ContentClass contentClass;

    @Requirement( role = P2LineupRepositoryConfigurator.class )
    private P2LineupRepositoryConfigurator p2LineupRepositoryConfigurator;

    @Requirement
    private RepositoryRegistry repositoryRegistry;

    @Requirement
    private MetadataManager metadataManager;

    @Requirement
    private P2LineupResolver lineupResolver;

    // @Requirement
    // private P2LineupManager lineupManager;

    @Requirement
    private GlobalRestApiSettings globalRestApiSettings;
    /*
     * Even though we do not use baseURLChangeListener directly, we need this here in order to create&initialize
     * BaseURLChangeListener singleton instance.
     */
    @SuppressWarnings( "unused" )
    @Requirement
    private BaseURLChangeListener baseURLChangeListener;

    private RepositoryKind repositoryKind;

    public void createArtifactStorageLinkItems( String artifactMappingsPath )
        throws IllegalOperationException, XmlPullParserException, NoSuchRepositoryException,
        UnsupportedStorageOperationException, ItemNotFoundException,
        StorageException
    {
        StorageFileItem artifactMappingsItem = getLocalStorageItem( artifactMappingsPath );
        Xpp3Dom dom = null;
        try
        {
            dom = Xpp3DomBuilder.build( new XmlStreamReader( artifactMappingsItem.getInputStream() ) );
        }
        catch ( IOException e )
        {
            throw new StorageException( e );
        }
        Xpp3Dom[] artifactRepositories = dom.getChildren( "repository" );
        for ( Xpp3Dom artifactRepositoryDom : artifactRepositories )
        {
            String nexusRepositoryId = artifactRepositoryDom.getAttribute( "nexusRepositoryId" );
            String nexusRepositoryRelativePath = artifactRepositoryDom.getAttribute( "nexusRepositoryRelativePath" );
            Repository targetRepository = repositoryRegistry.getRepository( nexusRepositoryId );
            for ( Xpp3Dom artifactDom : artifactRepositoryDom.getChildren( "artifact" ) )
            {
                String localPath = artifactDom.getAttribute( "localPath" );
                ResourceStoreRequest localStoreRequest = new ResourceStoreRequest( localPath );

                String remotePath = artifactDom.getAttribute( "remotePath" );
                if ( nexusRepositoryRelativePath != null )
                {
                    if ( !remotePath.startsWith( "/" ) )
                    {
                        remotePath = "/" + remotePath;
                    }
                    remotePath = nexusRepositoryRelativePath + remotePath;
                }
                if ( targetRepository.getRepositoryKind().isFacetAvailable( MavenRepository.class ) )
                {
                    // For artifacts from maven repositories we need to resolve them...
                    // ("SNAPSHOT" must be resolved to actual snapshot "version")
                    MavenRepository mavenRepository = targetRepository.adaptToFacet( MavenRepository.class );
                    ArtifactStoreRequest gavRequest =
                        new ArtifactStoreRequest( mavenRepository, remotePath, false /* localOnly */);
                    Gav gav = null;
                    try
                    {
                        gav = metadataManager.resolveArtifact( gavRequest );
                    }
                    catch ( IOException e )
                    {
                        throw new StorageException( e );
                    }
                    remotePath = mavenRepository.getGavCalculator().gavToPath( gav );
                }
                RepositoryItemUid itemUid = targetRepository.createUid( remotePath );

                DefaultStorageLinkItem storageLinkItem =
                    new DefaultStorageLinkItem( this, localStoreRequest, true /* canRead */, false /* canWrite */,
                                                itemUid );
                getLocalStorage().storeItem( this, storageLinkItem );
            }
        }
    }

    @Override
    protected Configurator getConfigurator()
    {
        return p2LineupRepositoryConfigurator;
    }

    @Override
    protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory()
    {
        return new CRepositoryExternalConfigurationHolderFactory<P2LineupRepositoryConfiguration>()
        {
            public P2LineupRepositoryConfiguration createExternalConfigurationHolder( CRepository config )
            {
                return new P2LineupRepositoryConfiguration( (Xpp3Dom) config.getExternalConfiguration() );
            }
        };
    }

    public ContentClass getRepositoryContentClass()
    {
        return contentClass;
    }

    public RepositoryKind getRepositoryKind()
    {
        if ( repositoryKind == null )
        {
            repositoryKind = new DefaultRepositoryKind( P2LineupRepository.class, null );
        }
        return repositoryKind;
    }

    // private String getPath( String itemPath )
    // {
    // if ( itemPath == null )
    // {
    // return null;
    // }
    //
    // int n = itemPath.lastIndexOf( '/' );
    // if ( n < 0 )
    // {
    // return itemPath;
    // }
    //
    // return itemPath.substring( 0, n );
    // }

    @Override
    public void storeItem( boolean fromTask, StorageItem item )
        throws UnsupportedStorageOperationException, IllegalOperationException, StorageException
    {
        if( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "storeItem:" + item.getPath() );
        }
        
        if ( !( item instanceof StorageFileItem ) )
        {
            getLogger().debug( "Attempting to store an item that is NOT a StorageFileItem, skipping:" + item.getPath() );
            return;
        }
        
        boolean fromLineupApi = item.getResourceStoreRequest().getRequestContext().containsKey( P2LineupManager.FROM_LINEUP_API );
        
        if( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Storage item called from Lineup persitance API: "+ fromLineupApi );
        }
        
        if ( item.getPath().toLowerCase().endsWith( P2LineupConstants.LINEUP_DESCRIPTOR_XML ) && !fromLineupApi )
        {   
            throw new IllegalRequestException( item.getResourceStoreRequest(), "Cannot write "+ P2LineupConstants.LINEUP_DESCRIPTOR_XML + " directly to the repository.  Use the lineup REST api." );
        }
        
        super.storeItem( fromTask, item );
    }

    public static boolean urisEquals( String uri1, String uri2 )
    {
        if ( uri1.endsWith( "/" ) )
        {
            uri1 = uri1.substring( 0, uri1.length() - 1 );
        }
        if ( uri2.endsWith( "/" ) )
        {
            uri2 = uri2.substring( 0, uri2.length() - 1 );
        }
        return uri1.equals( uri2 );
    }

    private IP2Lineup loadLineup( String p2LineupLocalPath )
        throws CannotLoadP2LineupException
    {
        InputStream p2LineupItemContent = null;

        try
        {
            StorageFileItem p2LineupItem =
                getLocalStorageItem( p2LineupLocalPath + P2LineupConstants.LINEUP_DESCRIPTOR_XML );

            p2LineupItemContent = p2LineupItem.getInputStream();

            XmlStreamReader reader = new XmlStreamReader( p2LineupItemContent );
            return new P2LineupXpp3Reader().read( reader, true /* strict */);
        }
        catch ( CannotLoadP2LineupException e )
        {
            throw e;
        }
        catch ( XmlPullParserException e )
        {
            throw new CannotLoadP2LineupException( e );
        }
        catch ( IOException e )
        {
            throw new CannotLoadP2LineupException( e );
        }
        catch ( IllegalOperationException e )
        {
            throw new CannotLoadP2LineupException( e );
        }
        catch ( ItemNotFoundException e )
        {
            throw new CannotLoadP2LineupException( e );
        }
        catch ( RuntimeException e )
        {
            throw new CannotLoadP2LineupException( e );
        }
        finally
        {
            IOUtil.close( p2LineupItemContent );
        }
    }

    public void resolveP2Lineup( String p2LineupLocalPath )
        throws CannotResolveP2LineupException
    {
        try
        {
            IP2Lineup p2Lineup = loadLineup( p2LineupLocalPath );
            lineupResolver.resolveLineup( this, p2Lineup, false /* validateOnly */);
        }
        catch ( CannotLoadP2LineupException e )
        {
            throw new CannotResolveP2LineupException( e );
        }
        catch ( RuntimeException e )
        {
            throw new CannotResolveP2LineupException( e );
        }
    }

    @Override
    protected StorageItem doRetrieveItem( ResourceStoreRequest request )
        throws IllegalOperationException,
            ItemNotFoundException,
            StorageException
    {
        String requestPath = request.getRequestPath();
        getLogger().debug( "doRetrieveItem:" + requestPath );
        
        //We are already authorized if we got here, so set flag
        //this works around the fact that we are actually retrieving data from a p2 repo, which isn't 
        //compatible with the p2lineup content class (so perms for p2lineup wont apply to this content)
        request.getRequestContext().put( AccessManager.REQUEST_AUTHORIZED, true );
        
        if ( RepositoryItemUid.PATH_ROOT.equals( requestPath )
            || requestPath.endsWith( P2LineupConstants.ARTIFACT_MAPPINGS_XML )
            || requestPath.endsWith( P2LineupConstants.LINEUP_DESCRIPTOR_XML )) // supporting legacy tests
        {
            return super.doRetrieveItem( request );
        }
        
        
        boolean doInterpolation = !request.getRequestContext().containsKey( P2Constants.PROP_SKIP_INTERPOLATION );
        if ( requestPath.endsWith( P2LineupConstants.CONTENT_XML ) && doInterpolation )
        {
            return this.interpolateContentXml( super.doRetrieveItem( request ), request );
        }

        if ( isNotAllowedP2MetadataItem( requestPath ) )
        {
            throw new ItemNotFoundException( request, this );
        }
        
        return super.doRetrieveItem( request );
    }

    // TODO: block deletion of lineups unless hitting lineup management REST resource.
//    @Override
//    protected void doDeleteItem( ResourceStoreRequest request )
//    throws UnsupportedStorageOperationException, ItemNotFoundException, StorageException
//    {
//        if ( !request.getRequestPath().endsWith( P2LineupConstants.LINEUP_DESCRIPTOR_XML ))
//        {
//            getLocalStorage().deleteItem( this, request ); 
//        }
//    }

    private boolean isNotAllowedP2MetadataItem( String requestPath )
    {
        return requestPath.endsWith( P2LineupConstants.CONTENT_JAR )
            || requestPath.endsWith( P2LineupConstants.ARTIFACTS_JAR );
    }

    private StorageFileItem getLocalStorageItem( String path )
        throws StorageException, IllegalOperationException, ItemNotFoundException
    {
        ResourceStoreRequest req = new ResourceStoreRequest( path );
        req.setRequestLocalOnly( true );

        return (StorageFileItem) retrieveItem( true, req );
    }

    public void storeItemFromFile( String path, File file )
        throws StorageException, UnsupportedStorageOperationException
    {
        ContentLocator content = new FileContentLocator( file, "text/xml" );
        DefaultStorageFileItem storageItem =
            new DefaultStorageFileItem( this, new ResourceStoreRequest( path ), true /* isReadable */,
                                        false /* isWritable */, content );
        getLocalStorage().storeItem( this, storageItem );
    }

    private StorageItem interpolateContentXml( StorageItem storageItem, ResourceStoreRequest request )
        throws StorageException
    {
        if ( !StorageFileItem.class.isInstance( storageItem ) )
        {
            throw new StorageException( "Item located at: '" + storageItem.getPath() + "' is not a file item." );
        }

        Map<String, String> properties = new HashMap<String, String>();
        try
        {
            properties.put( P2Constants.PROP_BASE_URL, this.getBaseUrl( request ) );
        }
        catch ( IllegalRequestException e )
        {
            throw new StorageException( "Cannot interpolate: '" + storageItem.getPath()
                + "' the Base URL is not set, and could not resolved from the original request.", e );
        }

        StorageFileItem fileItem = (StorageFileItem) storageItem;
        InterpolatedContentLocator newContentLocator =
            new InterpolatedContentLocator( fileItem.getContentLocator(), properties );
        fileItem.setContentLocator( newContentLocator );
        // the length is unknown now because we are interpolating it on the fly.
        fileItem.setLength( -1 );
        
        return storageItem;
    }

    private String getBaseUrl( ResourceStoreRequest request )
        throws IllegalRequestException
    {
        String baseUrl = request.getRequestAppRootUrl();

        if ( StringUtils.isEmpty( baseUrl ) )
        {
            baseUrl = this.globalRestApiSettings.getBaseUrl();
            if ( baseUrl == null )
            {
                throw new IllegalRequestException( request, "Base URL is not set." );
            }
        }

        if ( baseUrl.endsWith( "/" ) )
        {
            baseUrl = baseUrl.substring( 0, baseUrl.length() - 1 );
        }

        this.getLogger().debug( "baseURL=" + baseUrl );

        return baseUrl;
    }
}
