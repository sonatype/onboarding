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
package com.sonatype.nexus.p2.facade;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.sonatype.nexus.configuration.ConfigurationChangeEvent;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.application.GlobalHttpProxySettings;
import org.sonatype.nexus.plugins.repository.NoSuchPluginRepositoryArtifactException;
import org.sonatype.nexus.plugins.repository.PluginRepositoryArtifact;
import org.sonatype.nexus.plugins.repository.PluginRepositoryManager;
import org.sonatype.nexus.proxy.repository.RemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.UsernamePasswordRemoteAuthenticationSettings;
import org.sonatype.plexus.appevents.ApplicationEventMulticaster;
import org.sonatype.plexus.appevents.Event;
import org.sonatype.plexus.appevents.EventListener;
import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.tycho.osgi.EquinoxEmbedder;
import org.sonatype.tycho.osgi.EquinoxLocator;
import org.sonatype.tycho.p2.facade.internal.P2Logger;

import com.sonatype.nexus.p2.facade.internal.P2FacadeInternal;
import com.sonatype.nexus.p2.facade.internal.P2FacadeInternalException;
import com.sonatype.nexus.p2.facade.internal.P2InstallableUnitData;
import com.sonatype.nexus.p2.facade.internal.P2LineupResolutionRequest;
import com.sonatype.nexus.p2.facade.internal.P2LineupResolutionResult;
import com.sonatype.nexus.p2.facade.internal.P2RepositoryData;
import com.sonatype.nexus.p2.facade.internal.P2Resolver;
import com.sonatype.nexus.p2.rest.model.P2LineupUnresolvedInstallableUnit;

@Component( role = P2Facade.class )
public class DefaultP2Facade
    implements P2Facade, EventListener, Initializable
{
    @Requirement
    private EquinoxEmbedder equinox;

    @Requirement
    private ApplicationEventMulticaster applicationEventMulticaster;

    @Requirement
    private GlobalHttpProxySettings globalHttpProxySettings;

    @Requirement
    private EquinoxLocator equinoxLocator;

    @Requirement
    private PluginRepositoryManager pluginRepositoryManager;

    @Requirement
    ApplicationConfiguration applicationConfiguration;

    @Requirement
    private Logger logger;

    private P2FacadeInternal p2;

    private GAVCoordinate pluginGav;

    public void getRepositoryArtifacts( String url, String username, String password, File destination,
                                        File artifactMappingsXmlFile )
    {
        getP2().getRepositoryArtifacts( url, username, password, destination, artifactMappingsXmlFile );
    }

    private synchronized P2FacadeInternal getP2()
    {
        if ( pluginGav == null )
        {
            throw new IllegalStateException(
                "P2Facade was not properly initialized, call P2Facade.initializeP2() first!" );
        }

        if ( p2 == null )
        {
            PluginRepositoryArtifact pluginArtifact;
            try
            {
                pluginArtifact = pluginRepositoryManager.resolveArtifact( pluginGav );
            }
            catch ( NoSuchPluginRepositoryArtifactException e )
            {
                throw new IllegalStateException( "Could not locate nexus-p2-plugin", e );
            }
            equinoxLocator.setRuntimeLocation( new File( pluginArtifact.getFile().getParentFile(), "p2/eclipse" ) );

            File secureStorage =
                new File( applicationConfiguration.getConfigurationDirectory(), "eclipse.secure_storage" );
            logger.debug( "Setting -eclipse.keyring=" + secureStorage.getAbsolutePath() );
            equinox.setNonFrameworkArgs( new String[] { "-eclipse.keyring", secureStorage.getAbsolutePath(),
            // TODO Do we need to set: "-eclipse.password", "" ?
            } );

            // restore thread context classloader (workaround for an equinox bug)
            ClassLoader tcl = Thread.currentThread().getContextClassLoader();
            try
            {
                p2 = equinox.getService( P2FacadeInternal.class );
                setProxySettings();
                p2.setLogger( new _P2Logger( logger.getChildLogger( "P2FacadeInternal" ) ) );
            }
            finally
            {
                Thread.currentThread().setContextClassLoader( tcl );
            }
        }

        return p2;
    }

    public void initializeP2( GAVCoordinate pluginGav )
    {
        this.pluginGav = pluginGav;
    }

    public void getRepositoryContent( String url, String username, String password, File destination )
    {
        getP2().getRepositoryContent( url, username, password, destination );
    }

    public void generateSiteMetadata( File location, File metadataDir, String name )
    {
        getP2().generateSiteMetadata( location, metadataDir, name );
    }

    public void initialize()
    {
        applicationEventMulticaster.addEventListener( this );
    }

    public void onEvent( Event<?> evt )
    {
        if ( ConfigurationChangeEvent.class.isAssignableFrom( evt.getClass() ) )
        {
            // if there is no singleton yet, just let it be, when create it will apply the proxy settings
            if ( p2 != null )
            {
                setProxySettings();
            }
        }
    }

    private synchronized void setProxySettings()
    {
        logger.debug( "setProxySettings() was called" );
        if ( p2 != null )
        {
            if ( globalHttpProxySettings != null )
            {
                String username = null;
                String password = null;
                RemoteAuthenticationSettings authentication = globalHttpProxySettings.getProxyAuthentication();

                if ( authentication != null
                    && UsernamePasswordRemoteAuthenticationSettings.class.isAssignableFrom( authentication.getClass() ) )
                {
                    username = ( (UsernamePasswordRemoteAuthenticationSettings) authentication ).getUsername();
                    password = ( (UsernamePasswordRemoteAuthenticationSettings) authentication ).getPassword();
                    logger.debug( "Found global http proxy uses authentication, username=" + username );
                }
                String hostname = globalHttpProxySettings.getHostname();
                int port = globalHttpProxySettings.getPort();
                Set<String> nonProxyHosts = globalHttpProxySettings.getNonProxyHosts();
                logger.debug( "Found global http proxy settings: hostname=" + hostname + ", port=" + port
                    + ", username=" + username + ", nonProxyHosts=" + nonProxyHosts.toString() );
                p2.setProxySettings( hostname, port, username, password, nonProxyHosts );
            }
            else
            {
                logger.debug( "No global http proxy settings" );
                p2.setProxySettings( null, -1, null, null, null );
            }
        }
    }

    private URI file2Url( File file )
        throws URISyntaxException, IOException
    {
        String path = file.getCanonicalPath().replaceAll( "\\\\", "/" ).replaceAll( " ", "%20" );
        if ( path.charAt( 0 ) != '/' )
        {
            path = "/" + path;
        }
        return new URI( "file://" + path );
    }

    private void loadP2Lineup( P2LineupResolutionRequest request, P2Resolver p2Resolver, P2LineupResolutionResult result )
    {
        try
        {
            for ( P2RepositoryData sourceRepositoryData : request.getSourceRepositories() )
            {
                if ( "p2".equals( sourceRepositoryData.getLayout() ) )
                {
                    p2Resolver.addP2Repository( sourceRepositoryData.getId(),
                        file2Url( sourceRepositoryData.getLocalPath() ) );
                }
                else
                {
                    throw new IllegalStateException( "Unknown repository layout: " + sourceRepositoryData.getLayout() );
                }
            }
        }
        catch ( IOException e )
        {
            throw new P2FacadeInternalException( "Could not load p2 lineup descriptor data", e );
        }
        catch ( URISyntaxException e )
        {
            throw new P2FacadeInternalException( "Could not load p2 lineup descriptor data", e );
        }

        for ( P2InstallableUnitData rootIUData : request.getRootInstallableUnits() )
        {
            if ( rootIUData.getId().equals( request.getId() ) )
            {
                result.addUnresolvedInstallableUnit( new P2LineupUnresolvedInstallableUnit( rootIUData.getId(),
                                                                                            rootIUData.getVersion(),
                                                                                            "A lineup cannot depend on itself or another version of itself." ) );
            }
            else
            {
                p2Resolver.addRootInstallableUnit( rootIUData.getId(), rootIUData.getName(), rootIUData.getVersion(),
                                                   rootIUData.getTargetEnvironments(), result );
            }
        }
    }

    private static class _P2Logger
        implements P2Logger
    {
        private Logger logger;

        public _P2Logger( Logger logger )
        {
            this.logger = logger;
        }

        public void debug( String message )
        {
            if ( message.length() > 0 )
            {
                logger.debug( message );
            }
        }

        public void info( String message )
        {
            if ( message.length() > 0 )
            {
                logger.info( message );
            }
        }
    }

    public void resolveP2Lineup( P2LineupResolutionRequest request, P2LineupResolutionResult result )
    {
        P2Resolver p2Resolver = getP2().createResolver();
        p2Resolver.setLogger( new _P2Logger( logger.getChildLogger( "P2Resolver" ) ) );

        try
        {
            loadP2Lineup( request, p2Resolver, result );

            if ( result.isSuccess() )
            {
                getP2().resolveP2Lineup( request, p2Resolver, result );
            }
        }
        finally
        {
            p2Resolver.cleanupRepositories();
        }
    }

    public boolean containsExecutable( File metadataRepository )
    {
        return getP2().containsExecutable( metadataRepository );
    }
}
