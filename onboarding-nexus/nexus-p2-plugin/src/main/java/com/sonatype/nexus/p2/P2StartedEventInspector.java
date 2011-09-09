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
package com.sonatype.nexus.p2;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.model.CRemoteAuthentication;
import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.plugins.NexusPluginManager;
import org.sonatype.nexus.plugins.PluginDescriptor;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.events.AbstractEventInspector;
import org.sonatype.nexus.proxy.events.EventInspector;
import org.sonatype.nexus.proxy.events.NexusStartedEvent;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.PreparedContentLocator;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.ConfigurableRepository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.remote.commonshttpclient.CommonsHttpClientRemoteStorage;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.nexus.templates.NoSuchTemplateIdException;
import org.sonatype.nexus.templates.TemplateProvider;
import org.sonatype.nexus.templates.repository.RepositoryTemplate;
import org.sonatype.plexus.appevents.Event;
import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.scheduling.schedules.ManualRunSchedule;

import com.sonatype.nexus.p2.lineup.repository.P2LineupConstants;
import com.sonatype.nexus.p2.lineup.repository.P2LineupContentClass;
import com.sonatype.nexus.p2.lineup.repository.P2LineupRepository;
import com.sonatype.nexus.p2.lineup.task.PublishP2LineupTask;
import com.sonatype.s2.p2lineup.model.IP2Lineup;

@Component( role = EventInspector.class, hint = "P2StartedEventInspector" )
public class P2StartedEventInspector
    extends AbstractEventInspector
{

    @Requirement( role = RepositoryRegistry.class )
    private RepositoryRegistry repositoryRegistry;

    @Requirement( role = TemplateProvider.class, hint = P2LineupContentClass.ID )
    private TemplateProvider lineupTemplateProvider;

    @Requirement( role = TemplateProvider.class, hint = "p2-repository" )
    private TemplateProvider p2TemplateProvider;

    @Requirement
    private NexusScheduler scheduler;

    @Requirement
    private NexusPluginManager nexusPluginManager;

    public boolean accepts( Event<?> evt )
    {
        return evt instanceof NexusStartedEvent;
    }

    public void inspect( Event<?> evt )
    {
            if ( "true".equals( System.getProperty( "p2.lineups.create" ) ) )
            {
                createRepos();
            }
            else
            {
                Map<GAVCoordinate, PluginDescriptor> pluginMap = nexusPluginManager.getActivatedPlugins();

                for ( GAVCoordinate gav : pluginMap.keySet() )
                {
                    // TOTAL HACK, only install the lineups if the onboarding plugin is available
                    if ( gav.getArtifactId().equals( "nexus-onboarding-plugin" ) )
                    {
                        createRepos();
                        break;
                    }
                }
            }

            getLogger().debug( "Not creating lineup repos, onboarding not present" );
    }

    private void createRepos()
    {
        try
        {
            repositoryRegistry.getRepositoryWithFacet( IP2Lineup.LINEUP_REPOSITORY_ID, P2LineupRepository.class );
        }
        catch ( NoSuchRepositoryException e1 )
        {
            try
            {
                P2LineupRepository p2LineupRepo = createP2LineupRepo();
                createP2Repo( "nx-galileo", "Galileo Nexus Proxy", "http://download.eclipse.org/releases/galileo/" );
                createP2Repo( "nx-mse", "Sonatype Eclipse Plugin", "https://dist.sonatype.com/mse", "sonatype-dist-user",
                    "qah3JeX8" );
                createP2Repo( "nx-galileo-epp", "Galileo EPP Packages Nexus Proxy",
                    "http://download.eclipse.org/technology/epp/packages/galileo/" );

                copyLineup( p2LineupRepo, "org.sonatype.s2.catalogs.demo.maven.0.0.1.20100421-1239-lineup.xml",
                    "/org/sonatype/s2/catalogs/demo/maven/0.0.1.20100421-1239"
                        + P2LineupConstants.LINEUP_DESCRIPTOR_XML );
                copyLineup( p2LineupRepo, "org.sonatype.s2.catalogs.demo.maven-git.0.0.1.20100421-1239-lineup.xml",
                    "/org/sonatype/s2/catalogs/demo/maven-git/0.0.1.20100421-1239"
                        + P2LineupConstants.LINEUP_DESCRIPTOR_XML );

                // eclipse 3.6
                createP2Repo( "nx-helios", "Helios Nexus Proxy", "http://download.eclipse.org/releases/helios/" );
                createP2Repo( "nx-helios-epp", "Helios EPP Packages Nexus Proxy",
                    "http://download.eclipse.org/technology/epp/packages/helios/" );
                copyLineup( p2LineupRepo, "org.sonatype.s2.catalogs.demo.maven-git-e36.0.0.1.20100614-1435-lineup.xml",
                    "/org/sonatype/s2/catalogs/demo/maven-git-e36/0.0.1.20100614-1435"
                        + P2LineupConstants.LINEUP_DESCRIPTOR_XML );

                getLogger().debug( "This is first start of nexus, setting up default p2 lineup." );
                scheduleP2Publish();
            }
            catch ( Exception e )
            {
                getLogger().error( "Unable to setup default p2lineup properly", e );
            }
        }
    }

    private void scheduleP2Publish()
    {
        PublishP2LineupTask task = scheduler.createTaskInstance( PublishP2LineupTask.class );
        scheduler.schedule( "P2 Lineup publishing.", task, new ManualRunSchedule() );
    }

    private P2LineupRepository createP2LineupRepo()
        throws NoSuchTemplateIdException, ConfigurationException, IOException
    {
        List<P2LineupRepository> existingRepos = repositoryRegistry.getRepositoriesWithFacet( P2LineupRepository.class );

        if ( existingRepos != null && existingRepos.size() > 0 )
        {
            getLogger().info( "P2Lineup repository already exists, default P2Lineup repository will not be created!" );
            return existingRepos.get( 0 );
        }
        else
        {
            getLogger().info( "Default P2Lineup repository is missing, creating with default content." );
            RepositoryTemplate template =
                (RepositoryTemplate) lineupTemplateProvider.getTemplateById( P2LineupContentClass.ID );

            template.getConfigurableRepository().setId( IP2Lineup.LINEUP_REPOSITORY_ID );
            template.getConfigurableRepository().setName( IP2Lineup.LINEUP_REPOSITORY_NAME );
            template.getConfigurableRepository().setSearchable( false );
            template.getConfigurableRepository().setNotFoundCacheActive( false );
            template.getConfigurableRepository().setUserManaged( false );

            return (P2LineupRepository) template.create();
        }
    }

    private void createP2Repo( String id, String name, String remoteURL )
        throws NoSuchTemplateIdException, ConfigurationException, IOException
    {
        createP2Repo( id, name, remoteURL, null, null );
    }

    private void createP2Repo( String id, String name, String remoteURL, String username, String password )
        throws NoSuchTemplateIdException, ConfigurationException, IOException
    {
        RepositoryTemplate template = (RepositoryTemplate) p2TemplateProvider.getTemplateById( "p2_proxy" );

        ConfigurableRepository cfg = template.getConfigurableRepository();
        cfg.setId( id );
        cfg.setName( name );

        CRepositoryCoreConfiguration coreCfg = (CRepositoryCoreConfiguration) cfg.getCurrentCoreConfiguration();
        CRemoteStorage remote = new CRemoteStorage();
        remote.setProvider( CommonsHttpClientRemoteStorage.PROVIDER_STRING );
        remote.setUrl( remoteURL );

        if ( username != null )
        {
            CRemoteAuthentication auth = new CRemoteAuthentication();
            auth.setUsername( username );
            auth.setPassword( password );

            remote.setAuthentication( auth );
        }

        coreCfg.getConfiguration( true ).setRemoteStorage( remote );

        template.create();
    }

    private void copyLineup( P2LineupRepository p2LineupRepo, String lineupFilename, String deployPath )
        throws IOException, UnsupportedStorageOperationException, IllegalOperationException
    {
        ResourceStoreRequest storeRequest = new ResourceStoreRequest( deployPath );

        InputStream is = null;
        try
        {
            is = getClass().getResourceAsStream( "/META-INF/nexus/p2lineups/" + lineupFilename );
            // write to repository
            StorageFileItem storageFileItem =
                new DefaultStorageFileItem( p2LineupRepo, storeRequest, true, true, new PreparedContentLocator( is,
                    "application/xml" ) );
            p2LineupRepo.getLocalStorage().storeItem( p2LineupRepo, storageFileItem );
        }
        finally
        {
            IOUtil.close( is );
        }
    }
}
