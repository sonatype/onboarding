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
package com.sonatype.nexus.p2.lineup.resolver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

import com.sonatype.nexus.p2.P2Constants;
import com.sonatype.nexus.p2.facade.P2Facade;
import com.sonatype.nexus.p2.facade.internal.P2AdviceData;
import com.sonatype.nexus.p2.facade.internal.P2InstallableUnitData;
import com.sonatype.nexus.p2.facade.internal.P2LineupResolutionRequest;
import com.sonatype.nexus.p2.facade.internal.P2LineupResolutionResult;
import com.sonatype.nexus.p2.facade.internal.P2RepositoryData;
import com.sonatype.nexus.p2.group.P2GroupRepository;
import com.sonatype.nexus.p2.lineup.persist.InvalidP2GavException;
import com.sonatype.nexus.p2.lineup.persist.P2Gav;
import com.sonatype.nexus.p2.lineup.repository.P2LineupConstants;
import com.sonatype.nexus.p2.lineup.repository.P2LineupRepository;
import com.sonatype.nexus.p2.proxy.P2ProxyRepository;
import com.sonatype.nexus.p2.rest.model.P2LineupError;
import com.sonatype.nexus.p2.rest.model.P2LineupRepositoryError;
import com.sonatype.nexus.p2.rest.model.P2LineupUnresolvedInstallableUnit;
import com.sonatype.nexus.p2.updatesite.UpdateSiteRepository;
import com.sonatype.nexus.p2.util.P2Util;
import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.IP2LineupInstallableUnit;
import com.sonatype.s2.p2lineup.model.IP2LineupP2Advice;
import com.sonatype.s2.p2lineup.model.IP2LineupSourceRepository;
import com.sonatype.s2.p2lineup.model.IP2LineupTargetEnvironment;
import com.sonatype.s2.p2lineup.model.P2LineupHelper;
import com.sonatype.s2.p2lineup.model.P2LineupP2Advice;

@Component( role = P2LineupResolver.class )
public class DefaultP2LineupResolver
    implements P2LineupResolver, Initializable
{
    @Requirement
    private Logger log;

    @Requirement
    private RepositoryRegistry repositoryRegistry;

    @Requirement
    private P2Facade p2;

    public void initialize()
        throws InitializationException
    {
        p2.initializeP2( P2Util.getPluginCoordinates() );
    }

    public void resolveLineup( P2LineupRepository p2LineupRepository, IP2Lineup p2Lineup, boolean validateOnly )
        throws CannotResolveP2LineupException
    {
        long start = System.currentTimeMillis();

        P2LineupResolutionRequest p2LineupResolutionRequest = new P2LineupResolutionRequest();
        try
        {
            try
            {
                CannotResolveP2LineupException error = new CannotResolveP2LineupException();
                loadP2ResolutionRequest( p2Lineup, p2LineupResolutionRequest, error );

                P2LineupResolutionResult resolutionResult = new P2LineupResolutionResult( log );
                try
                {
                    p2.resolveP2Lineup( p2LineupResolutionRequest, resolutionResult );
                    if ( !resolutionResult.isSuccess() )
                    {
                        for ( P2LineupUnresolvedInstallableUnit iu : resolutionResult.getUnresolvedInstallableUnits() )
                        {
                            error.addUnresolvedInstallableUnit( iu );
                        }
                    }
                    else if ( !error.isFatal( false /* considerRepositoryErrors */) )
                    {
                        if ( !p2.containsExecutable( resolutionResult.getContentFile().getParentFile() ) )
                        {
                            P2LineupError missingExecutable = new P2LineupError("The lineup does not contain an executable.");
                            missingExecutable.setWarning( true );
                            error.setError( missingExecutable );
                        }
                        if ( !validateOnly )
                        {
                            P2Gav p2Gav = new P2Gav( p2Lineup );
                            String p2LineupLocalPath = p2Gav.toPathString();
                            p2LineupRepository.storeItemFromFile( p2LineupLocalPath + P2LineupConstants.CONTENT_XML,
                                                                  resolutionResult.getContentFile() );
                            p2LineupRepository.storeItemFromFile( p2LineupLocalPath + P2LineupConstants.ARTIFACTS_XML,
                                                                  resolutionResult.getArtifactsFile() );
                            p2LineupRepository.storeItemFromFile( p2LineupLocalPath
                                + P2LineupConstants.ARTIFACT_MAPPINGS_XML, resolutionResult.getArtifactMappingsFile() );

                            p2LineupRepository.createArtifactStorageLinkItems( p2LineupLocalPath
                                + P2LineupConstants.ARTIFACT_MAPPINGS_XML );
                        }

                        for ( P2LineupRepositoryError repositoryError : error.getRepositoryErrors() )
                        {
                            repositoryError.setWarning( true );
                        }
                    }
                }
                finally
                {
                    resolutionResult.cleanup();
                }
                if ( !error.isEmpty() )
                {
                    throw error;
                }
            }
            finally
            {
                p2LineupResolutionRequest.cleanup();
            }
        }
        catch ( CannotResolveP2LineupException e )
        {
            throw e;
        }
        catch ( IOException e )
        {
            throw new CannotResolveP2LineupException( e );
        }
        catch ( NoSuchRepositoryException e )
        {
            throw new CannotResolveP2LineupException( e );
        }
        catch ( UnsupportedStorageOperationException e )
        {
            throw new CannotResolveP2LineupException( e );
        }
        catch ( XmlPullParserException e )
        {
            throw new CannotResolveP2LineupException( e );
        }
        catch ( IllegalOperationException e )
        {
            throw new CannotResolveP2LineupException( e );
        }
        catch ( ItemNotFoundException e )
        {
            throw new CannotResolveP2LineupException( e );
        }
        catch ( RuntimeException e )
        {
            throw new CannotResolveP2LineupException( e );
        }
        finally
        {
            log.debug( "P2 Lineup resolved in " + ( System.currentTimeMillis() - start ) + " ms." );
        }
    }

    private static class ResolvedRepository
    {
        public Repository nexusRepository;

        public String nexusRepositoryRelativePath = "";

        public ResolvedRepository( Repository nexusRepository )
        {
            this.nexusRepository = nexusRepository;
        }

        public boolean isP2LineupRepository()
        {
            return nexusRepository instanceof P2LineupRepository;
        }
    }

    private Map<String, ResolvedRepository> resolveLineupRepositories( IP2Lineup p2Lineup )
    {
        Map<String, ResolvedRepository> result = new LinkedHashMap<String, ResolvedRepository>();

        for ( IP2LineupSourceRepository sourceRepository : p2Lineup.getRepositories() )
        {
            ResolvedRepository resolvedRepository = null;

            String sourceRepositoryURL = sourceRepository.getUrl();
            String layout = sourceRepository.getLayout();
            if ( "p2".equals( layout ) )
            {
                resolvedRepository = getP2RepositoryByUri( sourceRepositoryURL );
            }
            else if ( "maven".equals( layout ) )
            {
                resolvedRepository = getMavenRepositoryByUri( sourceRepositoryURL );
            }
            if ( resolvedRepository != null )
            {
                result.put( sourceRepositoryURL, resolvedRepository );
            }
        }
        return result;
    }

    private ResolvedRepository getMavenRepositoryByUri( String repositoryUri )
    {
        if ( repositoryUri.startsWith( IP2LineupSourceRepository.NEXUS_BASE_URL ) )
        {
            MavenRepository result = getLocalRepositoryByUriAndType( repositoryUri, MavenRepository.class );
            if ( result != null )
            {
                return new ResolvedRepository( result );
            }
            return null;
        }

        // Try to find it as a MavenProxyRepository
        MavenRepository result = getProxyRepositoryByUriAndType( repositoryUri, MavenProxyRepository.class );
        repositoryRegistry.getRepositoriesWithFacet( MavenProxyRepository.class );
        if ( result != null )
        {
            return new ResolvedRepository( result );
        }

        return null;
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

    private <T extends ProxyRepository> T getProxyRepositoryByUriAndType( String repositoryUri, Class<T> repositoryType )
    {
        List<T> existingProxyRepositories = repositoryRegistry.getRepositoriesWithFacet( repositoryType );
        for ( T existingProxyRepository : existingProxyRepositories )
        {
            if ( urisEquals( repositoryUri, existingProxyRepository.getRemoteUrl() ) )
            {
                log.debug( "Using " + existingProxyRepository.getClass().getSimpleName() + " repository "
                    + existingProxyRepository.getId() + " for " + repositoryUri );
                return existingProxyRepository;
            }
        }

        return null;
    }

    private <T extends Repository> T getLocalRepositoryByUriAndType( String repositoryUri, Class<T> repositoryType )
    {
        if ( !repositoryUri.startsWith( IP2LineupSourceRepository.NEXUS_BASE_URL ) )
        {
            throw new IllegalStateException( repositoryUri + " does not start with "
                + IP2LineupSourceRepository.NEXUS_BASE_URL );
        }

        if ( repositoryUri.endsWith( "/" ) )
        {
            repositoryUri = repositoryUri.substring( 0, repositoryUri.length() - 1 );
        }
        String repositoryId = repositoryUri.substring( repositoryUri.lastIndexOf( '/' ) + 1 );
        Repository repository;
        try
        {
            repository = repositoryRegistry.getRepository( repositoryId );
        }
        catch ( NoSuchRepositoryException e )
        {
            return null;
        }
        if ( repository.getRepositoryKind().isFacetAvailable( repositoryType ) )
        {
            log.debug( "Using " + repository.getClass().getSimpleName() + " repository " + repository.getId() + " for "
                + repositoryUri );
            return repository.adaptToFacet( repositoryType );
        }

        return null;
    }

    private ResolvedRepository getP2RepositoryByUri( String repositoryUri )
    {
        if ( repositoryUri.startsWith( IP2LineupSourceRepository.NEXUS_BASE_URL ) )
        {
            Repository result = getLocalRepositoryByUriAndType( repositoryUri, P2ProxyRepository.class );
            if ( result != null )
            {
                return new ResolvedRepository( result );
            }

            result = getLocalRepositoryByUriAndType( repositoryUri, UpdateSiteRepository.class );
            if ( result != null )
            {
                return new ResolvedRepository( result );
            }

            result = getLocalRepositoryByUriAndType( repositoryUri, P2GroupRepository.class );
            if ( result != null )
            {
                return new ResolvedRepository( result );
            }

            return getP2LineupRepositoryByUri( repositoryUri );
        }

        // Try to find it as a proxy repository
        Repository result = getProxyRepositoryByUriAndType( repositoryUri, P2ProxyRepository.class );
        if ( result != null )
        {
            return new ResolvedRepository( result );
        }

        result = getProxyRepositoryByUriAndType( repositoryUri, UpdateSiteRepository.class );
        if ( result != null )
        {
            return new ResolvedRepository( result );
        }

        return null;
    }

    private ResolvedRepository getP2LineupRepositoryByUri( String repositoryUri )
    {
        if ( !repositoryUri.startsWith( IP2LineupSourceRepository.NEXUS_BASE_URL ) )
        {
            throw new IllegalStateException( repositoryUri + " does not start with "
                + IP2LineupSourceRepository.NEXUS_BASE_URL );
        }

        // Remove the baseUrl part from the repositoryUri
        repositoryUri = repositoryUri.substring( IP2LineupSourceRepository.NEXUS_BASE_URL.length() );
        // Remove leading / from repositoryUri
        if ( repositoryUri.startsWith( "/" ) )
        {
            repositoryUri = repositoryUri.substring( 1 );
        }

        List<P2LineupRepository> existingP2LineupRepositories =
            repositoryRegistry.getRepositoriesWithFacet( P2LineupRepository.class );
        for ( P2LineupRepository p2LineupRepository : existingP2LineupRepositories )
        {
            String p2LineupRepositoryRelativePath = "content/repositories/" + p2LineupRepository.getId() + "/";
            if ( !repositoryUri.startsWith( p2LineupRepositoryRelativePath ) )
            {
                continue;
            }

            // Remove the p2LineupRepositoryRelativePath part from the repositoryUri
            repositoryUri = repositoryUri.substring( p2LineupRepositoryRelativePath.length() );
            // Remove trailing / from repositoryUri
            if ( repositoryUri.endsWith( "/" ) )
            {
                repositoryUri = repositoryUri.substring( 0, repositoryUri.length() - 1 );
            }

            try
            {
                new P2Gav( repositoryUri );
            }
            catch ( InvalidP2GavException e )
            {
                // The repositoryUri matches the current p2lineup repo URL, but it doesn't match a lineup
                return null;
            }

            ResolvedRepository result = new ResolvedRepository( p2LineupRepository );
            result.nexusRepositoryRelativePath = repositoryUri;
            return result;
        }

        return null;
    }

    private void saveStorageItemToTempFile( Repository repository, File path, String fileName )
        throws AccessDeniedException, ItemNotFoundException, IllegalOperationException, IOException
    {
        File tempFile = new File( path, new File( fileName ).getName() );

        ResourceStoreRequest storeRequest = new ResourceStoreRequest( fileName );
        storeRequest.getRequestContext().put( P2Constants.PROP_SKIP_INTERPOLATION, "true" );
        
        StorageFileItem storageItem = (StorageFileItem) repository.retrieveItem( storeRequest );

        InputStream is = null;
        OutputStream os = null;
        try
        {
            is = new BufferedInputStream( storageItem.getInputStream() );
            os = new BufferedOutputStream( new FileOutputStream( tempFile ) );
            IOUtil.copy( is, os );
        }
        finally
        {
            IOUtil.close( is );
            IOUtil.close( os );
        }
    }

    private void loadP2ResolutionRequest( IP2Lineup p2Lineup, P2LineupResolutionRequest request,
                                          CannotResolveP2LineupException error )
        throws CannotResolveP2LineupException
    {
        try
        {
            if ( p2Lineup.getRepositories().size() == 0 )
            {
                throw new CannotResolveP2LineupException( "At least one repository must be specified" );
            }
            if ( p2Lineup.getRootInstallableUnits().size() == 0 )
            {
                throw new CannotResolveP2LineupException( "At least one installable unit must be specified" );
            }

            request.setLocalPathPrefix( new P2Gav( p2Lineup ).toPathString() );

            Map<String, ResolvedRepository> nexusRepositoriesMap = resolveLineupRepositories( p2Lineup );

            request.setId( P2LineupHelper.getMasterInstallableUnitId( p2Lineup ) );
            request.setVersion( p2Lineup.getVersion() );
            request.setName( p2Lineup.getName() );
            request.setDescription( p2Lineup.getDescription() );

            // Load repository data
            for ( IP2LineupSourceRepository sourceRepository : p2Lineup.getRepositories() )
            {
                String sourceRepositoryURL = sourceRepository.getUrl();
                ResolvedRepository resolvedRepository = nexusRepositoriesMap.get( sourceRepositoryURL );
                if ( resolvedRepository == null )
                {
                    error.addRepositoryError( sourceRepositoryURL, "Cannot resolve repository on Nexus server." );
                    continue;
                }
                else if ( resolvedRepository.isP2LineupRepository() )
                {
                    try
                    {
                        P2Gav gav = new P2Gav( resolvedRepository.nexusRepositoryRelativePath );
                        if ( gav.getGroupId().equals( p2Lineup.getGroupId() ) && gav.getId().equals( p2Lineup.getId() ) )
                        {
                            error.addRepositoryError( sourceRepositoryURL,
                                                      "A lineup cannot depend on itself or another version of itself." );
                            // This is a fatal error
                            throw error;
                        }
                    }
                    catch ( InvalidP2GavException e )
                    {
                        // Ignore it - we already checked it's a valid GAV in resolveLineupRepositories
                    }
                }

                P2RepositoryData repositoryData = new P2RepositoryData();

                repositoryData.setLocation( sourceRepositoryURL );
                String layout = sourceRepository.getLayout();
                repositoryData.setLayout( layout );
                repositoryData.setNexusRepositoryId( resolvedRepository.nexusRepository.getId() );
                repositoryData.setNexusRepositoryName( resolvedRepository.nexusRepository.getName() );
                repositoryData.setNexusRepositoryRelativePath( resolvedRepository.nexusRepositoryRelativePath );

                if ( "p2".equals( layout ) )
                {
                    try
                    {
                        File tempDir = createTempDirectory();
                        repositoryData.setLocalPath( tempDir );
                        saveStorageItemToTempFile( resolvedRepository.nexusRepository, tempDir,
                                                   resolvedRepository.nexusRepositoryRelativePath
                                                       + P2LineupConstants.CONTENT_XML );
                        saveStorageItemToTempFile( resolvedRepository.nexusRepository, tempDir,
                                                   resolvedRepository.nexusRepositoryRelativePath
                                                       + P2LineupConstants.ARTIFACTS_XML );
                    }
                    catch ( Exception e )
                    {
                        log.error( e.getMessage(), e );
                        error.addRepositoryError( sourceRepositoryURL, e.getMessage() );
                        continue;
                    }
                }
                else if ( !"maven".equals( layout ) )
                {
                    error.addRepositoryError( sourceRepositoryURL, "Unknown repository layout: " + layout );
                    continue;
                }
                request.getSourceRepositories().add( repositoryData );
            }

            // Load target environments data
            loadTargetEnvironments( p2Lineup.getTargetEnvironments(), request.getTargetEnvironments() );

            // Load root installable units data
            for ( IP2LineupInstallableUnit iu : p2Lineup.getRootInstallableUnits() )
            {
                P2InstallableUnitData iuData = new P2InstallableUnitData();
                iuData.setId( iu.getId() );
                iuData.setVersion( iu.getVersion() );
                if ( iu.getName() != null && iu.getName().trim().length() > 0 )
                {
                    iuData.setName( iu.getName() );
                }
                else
                {
                    iuData.setName( iu.getId() );
                }

                loadTargetEnvironments( iu.getTargetEnvironments(), iuData.getTargetEnvironments() );

                request.getRootInstallableUnits().add( iuData );

                for ( IP2LineupTargetEnvironment iuTE : iu.getTargetEnvironments() )
                {
                    if ( !iuTargetEnvironmentMatchesLineupTargetEnvironments( iuTE, p2Lineup.getTargetEnvironments() ) )
                    {
                        error.addUnresolvedInstallableUnit( new P2LineupUnresolvedInstallableUnit( iu.getId(),
                                                                                                   iu.getVersion(),
                                                                                                   "Target environment: '"
                                                                                                       + targetEnvironmentToString( iuTE )
                                                                                                       + "' does not match any of the target environments declared in the lineup." ) );
                    }
                }
            }

            if ( p2Lineup.getP2Advice() == null) {
            	P2LineupP2Advice p2Advice =  new P2LineupP2Advice();
            	p2Advice.setTouchpointId("org.eclipse.equinox.p2.osgi");
            	p2Advice.setTouchpointVersion("1.0.0");
                p2Lineup.setP2Advice( p2Advice );
            }
            
            IP2LineupP2Advice p2Advice = p2Lineup.getP2Advice();
            P2AdviceData p2AdviceData = new P2AdviceData();
            p2AdviceData.setTouchpointId( p2Advice.getTouchpointId() );
            p2AdviceData.setTouchpointVersion( p2Advice.getTouchpointVersion() );
            
            List<String> p2Advices = p2Advice.getAdvices();
            
            // Build the advice to add the reference to the lineup repo
            String repositoryAdvice =
                getAddRepositoryP2Advice( IP2Lineup.LINEUP_REPOSITORY_ID,
                                          P2LineupHelper.getMasterInstallableUnitId( p2Lineup ), /* repository name */
                                          new P2Gav( p2Lineup ).toPathString(), true /* enabled */);
            p2Advices.add( repositoryAdvice );
            // Build the advice to add references to all lineup source repos (as disabled repos)
            for ( P2RepositoryData repositoryData : request.getSourceRepositories() )
            {
                repositoryAdvice =
                    getAddRepositoryP2Advice( repositoryData.getNexusRepositoryId(),
                                              repositoryData.getNexusRepositoryName(),
                                              repositoryData.getNexusRepositoryRelativePath(), false /* enabled */);
                p2Advices.add( repositoryAdvice );
            }
            
            p2AdviceData.setAdvices( p2Advices );

            request.setP2Advice( p2AdviceData );
        }
        catch ( CannotResolveP2LineupException e )
        {
            throw e;
        }
        catch ( RuntimeException e )
        {
            throw new CannotResolveP2LineupException( e );
        }
    }

    private String getAddRepositoryP2Advice( String nexusRepositoryId, String nexusRepositoryName,
                                             String nexusRepositoryRelativePath,
                                             boolean enabled )
    {
        String repositoryURL = "${nexus.baseURL}/content/repositories/" + nexusRepositoryId;
        if ( nexusRepositoryRelativePath != null && nexusRepositoryRelativePath.trim().length() > 0 )
        {
            repositoryURL += "/" + nexusRepositoryRelativePath;
        }
        while ( repositoryURL.contains( "//" ) )
        {
            repositoryURL = repositoryURL.replace( "//", "/" );
        }
        String repositoryAdvice =
            "configure=addRepository(type:0,enabled:" + enabled + ",location:" + repositoryURL + ",name:"
                + nexusRepositoryName + ");addRepository(type:1,enabled:" + enabled + ",location:" + repositoryURL
                + ",name:" + nexusRepositoryName + ");";
        return repositoryAdvice;
    }

    private String targetEnvironmentToString( IP2LineupTargetEnvironment targetEnvironment )
    {
        StringBuilder buf = new StringBuilder( 128 );

        if ( targetEnvironment.getOsgiOS() != null )
        {
            buf.append( "osgiOS=" );
            buf.append( targetEnvironment.getOsgiOS() );
        }
        if ( targetEnvironment.getOsgiWS() != null )
        {
            buf.append( "/osgiWS=" );
            buf.append( targetEnvironment.getOsgiWS() );
        }
        if ( targetEnvironment.getOsgiArch() != null )
        {
            buf.append( "/osgiArch=" );
            buf.append( targetEnvironment.getOsgiArch() );
        }

        return buf.toString();
    }

    private boolean iuTargetEnvironmentMatchesLineupTargetEnvironments( IP2LineupTargetEnvironment iuTE,
                                                                        Set<IP2LineupTargetEnvironment> lineupTEs )
    {
        if ( iuTE == null )
        {
            return true;
        }

        for ( IP2LineupTargetEnvironment lineupTE : lineupTEs )
        {
            if ( iuTE.getOsgiOS() != null && !iuTE.getOsgiOS().equals( lineupTE.getOsgiOS() ) )
            {
                continue;
            }
            if ( iuTE.getOsgiArch() != null && !iuTE.getOsgiArch().equals( lineupTE.getOsgiArch() ) )
            {
                continue;
            }
            if ( iuTE.getOsgiWS() != null && !iuTE.getOsgiWS().equals( lineupTE.getOsgiWS() ) )
            {
                continue;
            }
            return true;
        }

        return false;
    }

    private void loadTargetEnvironments( Set<IP2LineupTargetEnvironment> targetEnvironments,
                                         Set<Properties> destination )
    {
        for ( IP2LineupTargetEnvironment environment : targetEnvironments )
        {
            Properties environmentProps = new Properties();
            if ( environment.getOsgiOS() != null )
            {
                environmentProps.put( "osgi.os", environment.getOsgiOS() );
            }
            if ( environment.getOsgiWS() != null )
            {
                environmentProps.put( "osgi.ws", environment.getOsgiWS() );
            }
            if ( environment.getOsgiArch() != null )
            {
                environmentProps.put( "osgi.arch", environment.getOsgiArch() );
            }
            destination.add( environmentProps );
        }
    }

    private File createTempDirectory()
        throws IOException
    {
        File dir = File.createTempFile( "p2lineup.", "" );
        dir.delete();
        dir.mkdirs();
        return new File( dir.getAbsolutePath() );
    }
}
