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
package com.sonatype.nexus.onboarding.project.repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.IllegalRequestException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.ByteArrayContentLocator;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

import com.sonatype.nexus.onboarding.JnlpTemplateUtil;
import com.sonatype.nexus.onboarding.installer.MSEInstallerInfo;
import com.sonatype.nexus.onboarding.installer.MSEInstallerManager;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.descriptor.io.xpp3.S2ProjectDescriptorXpp3Reader;

@Component( role = Repository.class, hint = OnboardingContentClass.ID, instantiationStrategy = "per-lookup", description = "Onboarding Codebase Repository" )
public class OnboardingProjectRepository
    extends AbstractRepository
    implements HostedRepository, Repository
{

    public static final String INSTALL_JNLP_FILENAME = "mse-codebase.jnlp";

    public static final String INSTALL_TEMPLATE_JNLP = "templates/codebase-install.jnlp";

    private static final String BASE_URL_KEY = "nexus.baseURL";

    @Requirement( role = OnboardingRepositoryConfigurator.class )
    private OnboardingRepositoryConfigurator s2ProjectCatalogRepositoryConfigurator;

    @Requirement( hint = OnboardingContentClass.ID )
    private ContentClass contentClass;

    /**
     * GlobalRestApiSettings is need to get the base URL
     */
    @Requirement
    private GlobalRestApiSettings globalRestApiSettings;

    @Requirement
    private MSEInstallerManager mseInstallerManager;

    private RepositoryKind repositoryKind;

    @Override
    protected Configurator getConfigurator()
    {
        return this.s2ProjectCatalogRepositoryConfigurator;
    }

    @Override
    protected CRepositoryExternalConfigurationHolderFactory<OnboardingRepositoryConfiguration> getExternalConfigurationHolderFactory()
    {
        return new CRepositoryExternalConfigurationHolderFactory<OnboardingRepositoryConfiguration>()
        {
            public OnboardingRepositoryConfiguration createExternalConfigurationHolder( CRepository config )
            {
                return new OnboardingRepositoryConfiguration( (Xpp3Dom) config.getExternalConfiguration() );
            }
        };
    }

    public ContentClass getRepositoryContentClass()
    {
        return this.contentClass;
    }

    public RepositoryKind getRepositoryKind()
    {
        if ( repositoryKind == null )
        {
            repositoryKind = new DefaultRepositoryKind( this.getClass(), null );
        }
        return repositoryKind;
    }

    @Override
    public void storeItem( boolean fromTask, StorageItem item )
        throws UnsupportedStorageOperationException, IllegalOperationException, StorageException
    {
        if ( item.getName().equals( INSTALL_JNLP_FILENAME ) )
        {
            throw new UnsupportedStorageOperationException(
                                                            OnboardingProjectRepository.INSTALL_JNLP_FILENAME + " is a reserved filename for onboarding repositories, path: "
                                                                + item.getPath() );
        }

        super.storeItem( fromTask, item );
    }

    @Override
    protected Collection<StorageItem> doListItems( ResourceStoreRequest request )
        throws ItemNotFoundException, StorageException
    {

        Collection<StorageItem> storageItems = getLocalStorage().listItems( this, request );

        // add the JNLP to the listing (if it should be there)
        if ( this.checkRenderJnlp( request ) )
        {
            ContentLocator contentLocator =
                new ByteArrayContentLocator( "".getBytes(), JnlpTemplateUtil.JNLP_MIME_TYPE );

            // need a different request to update the file name
            ResourceStoreRequest jnlpRequest =
                new ResourceStoreRequest( request.getRequestPath() + "/" + INSTALL_JNLP_FILENAME );

            DefaultStorageFileItem jnlpStorageItem =
                new DefaultStorageFileItem( this, jnlpRequest, true /* canRead */, false /* canWrite */, contentLocator );
            jnlpStorageItem.setModified( System.currentTimeMillis() );

            storageItems.add( jnlpStorageItem );
        }
        return storageItems;

    }

    private IS2Project loadCodebase( String requestPath )
        throws StorageException, ItemNotFoundException
    {
        ResourceStoreRequest codebaseRequest = new ResourceStoreRequest( requestPath );
        StorageFileItem codebaseItem = (StorageFileItem) getLocalStorage().retrieveItem( this, codebaseRequest );
        IS2Project s2Project;
        try
        {
            InputStream codebaseInputStream = codebaseItem.getInputStream();
            try
            {
                s2Project = new S2ProjectDescriptorXpp3Reader().read( codebaseInputStream, false /* strict */);
                return s2Project;
            }
            finally
            {
                IOUtil.close( codebaseInputStream );
            }
        }
        catch ( XmlPullParserException e )
        {
            throw new LocalStorageException( e );
        }
        catch ( IOException e )
        {
            throw new LocalStorageException( e );
        }
    }

    private boolean checkRenderJnlp( ResourceStoreRequest request )
        throws StorageException
    {
        String path = request.getRequestPath();
        if ( path.endsWith( INSTALL_JNLP_FILENAME ) )
        {
            path = path.substring( 0, path.length() - INSTALL_JNLP_FILENAME.length() );
            path = path + IS2Project.PROJECT_DESCRIPTOR_FILENAME;
        }
        else
        {
            path = path + "/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME;
        }

        try
        {
            IS2Project s2Project = loadCodebase( path );
            // Render jnlp only if the codebase has a lineup associated with it.
            return s2Project.getP2LineupLocation() != null && s2Project.getP2LineupLocation().getUrl() != null
                && s2Project.getP2LineupLocation().getUrl().trim().length() > 0;
        }
        catch ( ItemNotFoundException e )
        {
            return false;
        }
    }

    @Override
    protected StorageItem doRetrieveItem( ResourceStoreRequest request )
        throws IllegalOperationException, ItemNotFoundException, StorageException
    {
        String requestPath = request.getRequestPath();

        // directory listing
        if ( requestPath.equals( RepositoryItemUid.PATH_ROOT ) )
        {
            return super.doRetrieveItem( request );
        }

        // render JNLP
        if ( requestPath.endsWith( INSTALL_JNLP_FILENAME ) )
        {
            if ( !this.checkRenderJnlp( request ) )
            {
                throw new ItemNotFoundException( request );
            }
            try
            {
                return createInstallJnlpItem( request );
            }
            catch ( IOException e )
            {
                throw new StorageException( "Error processing JNLP template.", e );
            }
        }

        // render project xml, but only in case of real request, an auth check we dont have baseURL in request
        // so cant filter, and really is no need, just checking if file exists
        if ( requestPath.endsWith( IS2Project.PROJECT_DESCRIPTOR_FILENAME ) 
             && !request.getRequestContext().containsKey( RequestContext.CTX_AUTH_CHECK_ONLY ) )
        {
            try
            {
                return filterProjectXml( request );
            }
            catch ( IOException e )
            {
                throw new StorageException( "Error processing filtering codebase descriptor.", e );
            }
        }

        // anything else
        return super.doRetrieveItem( request );
    }

    private StorageItem filterProjectXml( ResourceStoreRequest request ) throws StorageException, IllegalOperationException, ItemNotFoundException
    {
        StorageItem item = super.doRetrieveItem( request );

        getLogger().debug( "Filtering: " + item.getPath() );

        StorageFileItem fileItem = (StorageFileItem) item;

        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        Map<String, String> interpolationProperties = new HashMap<String, String>();
        interpolationProperties.put( BASE_URL_KEY, this.getBaseUrl( request ) );

        InputStreamReader reader = null;
        try
        {
            reader = new InputStreamReader( fileItem.getInputStream() );

            InterpolationFilterReader interpolationFilterReader =
                new InterpolationFilterReader( reader, interpolationProperties );

            IOUtil.copy( interpolationFilterReader, new OutputStreamWriter( buf, reader.getEncoding() ) );
        }
        catch ( IOException e )
        {
            throw new StorageException( e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        ContentLocator content = new ByteArrayContentLocator( buf.toByteArray(), "text/xml" );

        fileItem.setContentLocator( content );
        fileItem.setLength( buf.size() );

        return item;
    }

    private StorageFileItem getJnlpItem( ResourceStoreRequest jnlpRequest, String codebasePath )
        throws ItemNotFoundException, StorageException, IllegalOperationException
    {
        try
        {
            IS2Project codebase = loadCodebase( codebasePath );
            MSEInstallerInfo mseInstaller = mseInstallerManager.resolveInstaller( codebase.getInstallerVersion() );
            if ( mseInstaller == null )
            {
                String errorMessage =
                    "Cannot find an MSE installer that can install codebase: " + codebasePath
                        + ". Required MSE installer version: " + codebase.getInstallerVersion();
                getLogger().debug( errorMessage );
                throw new ItemNotFoundException( errorMessage, jnlpRequest, this );
            }
            return mseInstallerManager.getJnlpItem( mseInstaller );
        }
        catch ( NoSuchRepositoryException e )
        {
            throw new LocalStorageException( e );
        }
        catch ( AccessDeniedException e )
        {
            throw new LocalStorageException( e );
        }
    }

    public String getMSEInstallersRepositoryId()
    {
        return getExternalConfiguration( false ).getMSEInstallersRepositoryId();
    }

    private StorageItem createInstallJnlpItem( ResourceStoreRequest request )
        throws IOException, ItemNotFoundException, IllegalOperationException
    {
        String baseUrl = this.getBaseUrl( request );

        String requestPath = request.getRequestPath();

        String descriptorToInstall =
            requestPath.substring( 0, requestPath.length() - INSTALL_JNLP_FILENAME.length() )
                + IS2Project.PROJECT_DESCRIPTOR_FILENAME;

        Map<String, String> properties = new LinkedHashMap<String, String>();

        String codebaseURL = baseUrl + "/content/repositories/" + getPathPrefix();
        properties.put( "nexus.baseUrl", baseUrl );
        properties.put( "codebaseURL", codebaseURL );
        properties.put( "generationDate", new Date().toString() );
        // properties.put( "s2installerURL", baseUrl );
        properties.put( "osgiInstallTempArea", ".mse/installer/" + System.currentTimeMillis() );
        properties.put( "descriptorToInstall", descriptorToInstall );
        properties.put( "mseInstallerRepoId", getMSEInstallersRepositoryId() );

        String processedTemplate = null;
        StorageFileItem jnlpItem = getJnlpItem( request, descriptorToInstall );
        InputStream jnlpInputStream = jnlpItem.getInputStream();
        try
        {
            processedTemplate = JnlpTemplateUtil.processJnlpTemplate( jnlpInputStream, properties );
        }
        finally
        {
            IOUtil.close( jnlpInputStream );
        }

        ContentLocator contentLocator =
            new ByteArrayContentLocator( processedTemplate.getBytes(), JnlpTemplateUtil.JNLP_MIME_TYPE );

        DefaultStorageFileItem item =
            new DefaultStorageFileItem( this, request, true /* canRead */, false /* canWrite */, contentLocator );
        item.setLength( processedTemplate.length() );
        item.setModified( System.currentTimeMillis() );

        // remove leading '/'
        String gavString = ( requestPath.startsWith( "/" ) ) ? requestPath.substring( 1 ) : requestPath;
        // remove the jnlp file name
        gavString = gavString.substring( 0, gavString.length() - INSTALL_JNLP_FILENAME.length() );
        // convert / to .
        gavString = gavString.replaceAll( "/", "\\." ) + "jnlp";

        item.getItemContext().put( "override-filename", "codebase-" + gavString );

        return item;
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
                throw new IllegalRequestException( request, "Cannot generate installer jnlp: Base URL is not set." );
            }
        }
        
        if( baseUrl.endsWith( "/" ))
        {
            baseUrl = baseUrl.substring( 0, baseUrl.length() - 1 );
        }
        
        this.getLogger().debug( "baseURL=" + baseUrl );

        return baseUrl;
    }

    @Override
    public OnboardingRepositoryConfiguration getExternalConfiguration( boolean forModification )
    {
        return (OnboardingRepositoryConfiguration) super.getExternalConfiguration( forModification );
    }

    @Override
    protected void enforceWritePolicy( ResourceStoreRequest request, Action action )
        throws IllegalRequestException
    {
        // allow redeploy of -HEAD versions
        if ( !request.getRequestPath().contains( "-" + IS2Project.HEAD_VERSION_SUFFIX ) )
        {
            super.enforceWritePolicy( request, action );
        }
    }
}
