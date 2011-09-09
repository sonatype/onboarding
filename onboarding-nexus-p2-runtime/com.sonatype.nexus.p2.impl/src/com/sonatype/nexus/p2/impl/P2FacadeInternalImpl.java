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
package com.sonatype.nexus.p2.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.repository.ICompositeRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.sonatype.tycho.p2.facade.internal.P2Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sonatype.nexus.p2.auth.P2AuthSession;
import com.sonatype.nexus.p2.facade.internal.P2FacadeInternal;
import com.sonatype.nexus.p2.facade.internal.P2FacadeInternalException;
import com.sonatype.nexus.p2.facade.internal.P2LineupResolutionRequest;
import com.sonatype.nexus.p2.facade.internal.P2LineupResolutionResult;
import com.sonatype.nexus.p2.facade.internal.P2RepositoryData;
import com.sonatype.nexus.p2.facade.internal.P2Resolver;
import com.sonatype.nexus.p2.impl.bugzilla268893.LocalUpdateSiteAction;
import com.sonatype.nexus.p2.xmlio.ArtifactsIO;
import com.sonatype.nexus.p2.xmlio.MetadataIO;

@SuppressWarnings( "restriction" )
public class P2FacadeInternalImpl
    implements P2FacadeInternal
{
    /**
     * Gathers all SimpleArtifactRepositories referenced from the specified repository (recursively). If the specified
     * artifact repository is a SimpleArtifactRepository, the returned list will only contain the specified artifact
     * repository.
     * 
     * @param artifactRepositoryManager The artifact repository manager that will be used to load all repositories
     * @param repository The start artifact repository
     * @param result The list of all referenced simple artifact repositories
     */
    private void getAllSimpleArtifactRepositories( IArtifactRepositoryManager artifactRepositoryManager,
                                                   IArtifactRepository repository,
                                                   Set<SimpleArtifactRepository> result, IProgressMonitor monitor,
                                                   String indent )
        throws ProvisionException
    {
        indent += "   ";
        if ( repository instanceof SimpleArtifactRepository )
        {
            getLogger().debug( indent + "SimpleArtifactRepository: " + repository.getLocation() );
            result.add( (SimpleArtifactRepository) repository );
            return;
        }
        if ( repository instanceof ICompositeRepository )
        {
            getLogger().debug( indent + "CompositeRepository: " + repository.getLocation() );
            ICompositeRepository compositeRepository = (ICompositeRepository) repository;
            List<URI> childURIs = compositeRepository.getChildren();
            for ( URI childURI : childURIs )
            {
                IArtifactRepository childRepository = artifactRepositoryManager.loadRepository( childURI, monitor );
                getAllSimpleArtifactRepositories( artifactRepositoryManager, childRepository, result, monitor, indent );
            }
            return;
        }
        throw new RuntimeException( "Unknown repository type " + repository.getClass().getCanonicalName() );
    }

    public void getRepositoryArtifacts( String uri, String username, String password, File destination,
                                        File artifactMappingsXmlFile )
    {
        IArtifactRepositoryManager artifactRepositoryManager =
            (IArtifactRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                   IArtifactRepositoryManager.class.getName() );
        if ( artifactRepositoryManager == null )
        {
            throw new IllegalStateException( "No artifact repository manager found" ); //$NON-NLS-1$
        }

        IProgressMonitor monitor = new NullProgressMonitor();

        boolean isNewRepository = false;
        P2AuthSession p2AuthSession = new P2AuthSession();
        try
        {
            URI location = new URI( uri );
            p2AuthSession.setCredentials( location, username, password );

            isNewRepository = !artifactRepositoryManager.contains( location );
            try
            {
                // if ( !isNewRepository )
                // {
                // HACK Start - This is an ugly hack: we refresh the repository
                // even if it is not known to the
                // artifactRepositoryManager only to get it removed from
                // internal p2 caches (like not found repos cache)
                artifactRepositoryManager.refreshRepository( location, monitor );
                // HACK End
                // }
            }
            catch ( Exception e )
            {
                // We get an exception here if the repo is not known to the
                // artifactRepositoryManager
                // Ignore it
            }
            try
            {
                IArtifactRepository remoteRepository = artifactRepositoryManager.loadRepository( location, monitor );
                if ( artifactRepositoryManager.contains( destination.toURI() ) )
                {
                    artifactRepositoryManager.removeRepository( destination.toURI() );
                }

                Set<SimpleArtifactRepository> allSimpleArtifactRepositories =
                    new LinkedHashSet<SimpleArtifactRepository>();
                getLogger().debug( "getAllSimpleArtifactRepositories: " + remoteRepository.getLocation() );
                getAllSimpleArtifactRepositories( artifactRepositoryManager, remoteRepository,
                                                  allSimpleArtifactRepositories, monitor, "" /* indent */);
                Map<Object, Object> allSimpleArtifactRepositoryProperties = new LinkedHashMap<Object, Object>();
                boolean publishPackFilesAsSiblings = false;
                for ( SimpleArtifactRepository repository : allSimpleArtifactRepositories )
                {
                    Map<Object, Object> repositoryProperties = repository.getProperties();
                    if ( repositoryProperties != null )
                    {
                        if ( "true".equals( repositoryProperties.get( "publishPackFilesAsSiblings" ) ) )
                        {
                            publishPackFilesAsSiblings = true;
                        }
                        allSimpleArtifactRepositoryProperties.putAll( repositoryProperties );
                    }
                }
                if ( allSimpleArtifactRepositories.size() > 1 )
                {
                    // If we have more than one source artifact repository, we
                    // cannot use the source repository
                    // properties
                    allSimpleArtifactRepositoryProperties.remove( IRepository.PROP_MIRRORS_URL );
                    allSimpleArtifactRepositoryProperties.put( IRepository.PROP_COMPRESSED, "false" );
                    allSimpleArtifactRepositoryProperties.put( IRepository.PROP_TIMESTAMP,
                                                               "" + System.currentTimeMillis() );
                    if ( publishPackFilesAsSiblings )
                    {
                        allSimpleArtifactRepositoryProperties.put( "publishPackFilesAsSiblings", "true" );
                    }
                }

                SimpleArtifactRepository localRepository =
                    new SimpleArtifactRepository( remoteRepository.getName(), destination.toURI(),
                                                  allSimpleArtifactRepositoryProperties )
                    {
                        @Override
                        public void save( boolean compress )
                        {
                            // do nothing
                        }
                    };

                List<IArtifactDescriptor> allArtifactDescriptors = new ArrayList<IArtifactDescriptor>();
                for ( IArtifactKey key : remoteRepository.getArtifactKeys() )
                {
                    IArtifactDescriptor[] remoteArtifactDescriptors = remoteRepository.getArtifactDescriptors( key );
                    localRepository.addDescriptors( remoteArtifactDescriptors );
                    allArtifactDescriptors.addAll( Arrays.asList( remoteArtifactDescriptors ) );
                }
                // Merge all rules
                Map<String, String> rules = new LinkedHashMap<String, String>();
                mergeArtifactRepositoryRules( rules, localRepository.getRules() );
                for ( SimpleArtifactRepository repository : allSimpleArtifactRepositories )
                {
                    mergeArtifactRepositoryRules( rules, repository.getRules() );
                }
                String[][] rulesArray = new String[rules.size()][2];
                int ruleIndex = 0;
                for ( String filter : rules.keySet() )
                {
                    rulesArray[ruleIndex][0] = filter;
                    rulesArray[ruleIndex][1] = rules.get( filter );
                    ruleIndex++;
                }
                localRepository.setRules( rulesArray );

                generateArtifactsByRepository( allArtifactDescriptors, allSimpleArtifactRepositories,
                                               artifactMappingsXmlFile );

                ArtifactsIO artifactsWriter = new ArtifactsIO();
                artifactsWriter.writeXML( localRepository, destination );
            }
            finally
            {
                if ( isNewRepository )
                {
                    artifactRepositoryManager.removeRepository( location );
                }
            }
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            p2AuthSession.cleanup();
        }
    }

    private void mergeArtifactRepositoryRules( Map<String, String> rules1, String[][] rules2 )
    {
        for ( String[] rule2 : rules2 )
        {
            String filter2 = rule2[0];
            String output2 = rule2[1];
            String output1 = rules1.get( filter2 );
            if ( output1 == null )
            {
                rules1.put( filter2, output2 );
            }
            else if ( !output1.equals( output2 ) )
            {
                throw new RuntimeException( "Incompatible artifact repository rules for filter '" + filter2
                    + "': output1='" + output1 + "', output2='" + output2 + "'" );
            }
        }
    }

    public void getRepositoryContent( String uri, String username, String password, File destination )
    {
        IMetadataRepositoryManager metadataRepositoryManager =
            (IMetadataRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                   IMetadataRepositoryManager.class.getName() );
        if ( metadataRepositoryManager == null )
        {
            throw new IllegalStateException( "No metadata repository manager found" ); //$NON-NLS-1$
        }

        NullProgressMonitor monitor = new NullProgressMonitor();

        P2AuthSession p2AuthSession = new P2AuthSession();
        try
        {
            URI location = new URI( uri );
            p2AuthSession.setCredentials( location, username, password );

            boolean isNewRepository = !metadataRepositoryManager.contains( location );
            try
            {
                // if ( !isNewRepository )
                // {
                // HACK Start - This is an ugly hack: we refresh the repository
                // even if it is not known to the
                // artifactRepositoryManager only to get it removed from
                // internal p2 caches (like not found repos cache)
                metadataRepositoryManager.refreshRepository( location, monitor );
                // HACK End
                // }
            }
            catch ( Exception e )
            {
                // We get an exception here if the repo is not known to the
                // artifactRepositoryManager
                // Ignore it
            }
            try
            {
                IMetadataRepository metadataRepository = metadataRepositoryManager.loadRepository( location, monitor );
                MetadataIO metadataWriter = new MetadataIO();
                metadataWriter.writeXML( metadataRepository, destination );
            }
            finally
            {
                if ( isNewRepository )
                {
                    metadataRepositoryManager.removeRepository( location );
                }
            }
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            p2AuthSession.cleanup();
        }
    }

    public void setProxySettings( String proxyHostname, int proxyPort, String username, String password,
                                  Set<String> nonProxyHosts )
    {
        getLogger().debug( "Start setProxySettings(), proxyHostname=" + proxyHostname + ", proxyPort=" + proxyPort
                               + ", username=" + username );
        IProxyService proxyService =
            (IProxyService) ServiceHelper.getService( Activator.getContext(), IProxyService.class.getName() );

        if ( proxyHostname == null )
        {
            proxyService.setProxiesEnabled( false );
            return;
        }

        boolean requireAuthentication = username != null;

        ProxyData httpProxyData =
            new ProxyData( IProxyData.HTTP_PROXY_TYPE, proxyHostname, proxyPort, requireAuthentication, null );
        ProxyData httpsProxyData =
            new ProxyData( IProxyData.HTTPS_PROXY_TYPE, proxyHostname, proxyPort, requireAuthentication, null );

        if ( requireAuthentication )
        {
            httpProxyData.setUserid( username );
            httpProxyData.setPassword( password );

            httpsProxyData.setUserid( username );
            httpsProxyData.setPassword( password );
        }

        proxyService.setProxiesEnabled( true );
        proxyService.setSystemProxiesEnabled( false );

        try
        {
            if ( nonProxyHosts != null && nonProxyHosts.size() > 0 )
            {
                proxyService.setNonProxiedHosts( nonProxyHosts.toArray( new String[0] ) );
            }
            else
            {
                proxyService.setNonProxiedHosts( new String[0] );
            }

            proxyService.setProxyData( new IProxyData[] { httpProxyData, httpsProxyData, } );
        }
        catch ( CoreException e )
        {
            throw new P2FacadeInternalException( "Could not set proxy configuration", e );
        }
    }

    public void generateSiteMetadata( File location, File metadataDir, String name )
    {
        PublisherInfo info = new PublisherInfo();

        try
        {
            info.setArtifactRepository( Publisher.createArtifactRepository( metadataDir.toURI(), name,
                                                                            false /* append */, false /* compress */,
                                                                            true /* reusePackedFiles */) );

            info.setMetadataRepository( Publisher.createMetadataRepository( metadataDir.toURI(), name,
                                                                            false /* append */, false /* compress */) );

            ArrayList<IPublisherAction> actions = new ArrayList<IPublisherAction>();
            actions.add( new LocalUpdateSiteAction( location.getAbsolutePath(), null ) );
            // actions.add( new BundlesAction( new File[] { location } ) );
            // actions.add( new FeaturesAction( new File[] { location } ) );
            // actions.add( new SiteXMLAction( location.toURI(), null ) );

            new Publisher( info ).publish( actions.toArray( new IPublisherAction[actions.size()] ),
                                           new NullProgressMonitor() );

        }
        catch ( ProvisionException e )
        {
            throw new P2FacadeInternalException( "Could not generate Eclipse Update Site P2 metadata", e );
        }
    }

    public void resolveP2Lineup( P2LineupResolutionRequest request, P2Resolver p2Resolver,
                                 P2LineupResolutionResult result )
    {
        if ( request.getTargetEnvironments().size() == 0 )
        {
            // There must be at least one target environment
            request.getTargetEnvironments().add( new Properties() );
        }

        String p2LineupId = request.getId();

        String masterIUName = request.getName();
        if ( masterIUName == null || masterIUName.trim().length() == 0 )
        {
            masterIUName = p2LineupId + " Lineup";
        }

        P2ResolverImpl p2ResolverImpl = (P2ResolverImpl) p2Resolver;
        IInstallableUnit masterIU =
            p2ResolverImpl.createMasterInstallableUnit( p2LineupId, request.getVersion(), masterIUName,
                                                        request.getDescription(), request.getP2Advice() );

        P2InternalResolutionResult p2ResolutionResult = null;
        int count = 0;
        for ( Properties targetEnvironment : request.getTargetEnvironments() )
        {
            if ( !targetEnvironment.containsKey( "org.eclipse.update.install.features" ) )
            {
                targetEnvironment.put( "org.eclipse.update.install.features", "true" );
            }
            p2Resolver.setProperties( targetEnvironment );
            P2InternalResolutionResult newP2ResolutionResult = (P2InternalResolutionResult) p2Resolver.resolve( result );
            if ( !result.isSuccess() )
            {
                return;
            }
            if ( count == 0 )
            {
                p2ResolutionResult = newP2ResolutionResult;
            }
            else
            {
                p2ResolutionResult.merge( newP2ResolutionResult );
            }
            count++;
        }

        try
        {
            // Generate content.xml
            TempMetadataRepository tempMetadataRepository = new TempMetadataRepository();
            tempMetadataRepository.setName( p2LineupId );
            tempMetadataRepository.addInstallableUnits( p2ResolutionResult.getInstallableUnits().toArray( new IInstallableUnit[0] ) );
            tempMetadataRepository.addInstallableUnit( masterIU );
            IInstallableUnit catgoryIU = createP2LineupCategoryIU( masterIU );
            tempMetadataRepository.addInstallableUnit( catgoryIU );

            MetadataIO metadataWriter = new MetadataIO();
            metadataWriter.writeXML( tempMetadataRepository, result.getContentFile() );

            // Generate artifacts.xml
            Map<Object, Object> artifactRepositoryProperties = new LinkedHashMap<Object, Object>();
            artifactRepositoryProperties.put( IRepository.PROP_COMPRESSED, "false" );
            artifactRepositoryProperties.put( IRepository.PROP_TIMESTAMP, "" + System.currentTimeMillis() );
            artifactRepositoryProperties.put( "publishPackFilesAsSiblings", "true" );
            TempArtifactRepository tempArtifactRepository = new TempArtifactRepository( artifactRepositoryProperties );
            tempArtifactRepository.setName( p2LineupId );
            tempArtifactRepository.addDescriptors( p2ResolutionResult.getArtifactDescriptors().toArray( new IArtifactDescriptor[0] ) );
            ArtifactsIO artifactsWriter = new ArtifactsIO();
            artifactsWriter.writeXML( tempArtifactRepository, result.getArtifactsFile() );

            generateArtifactsByRepository( p2ResolutionResult, tempArtifactRepository, request,
                                           result.getArtifactMappingsFile() );
        }
        catch ( IOException e )
        {
            throw new P2FacadeInternalException( "Could not generate P2 lineup metadata", e );
        }
        catch ( URISyntaxException e )
        {
            throw new P2FacadeInternalException( "Could not generate P2 lineup metadata", e );
        }
    }

    private IInstallableUnit createP2LineupCategoryIU( IInstallableUnit p2LineupMasterIU )
    {
        InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
        iud.setId( p2LineupMasterIU.getId() + ".category" );
        iud.setVersion( p2LineupMasterIU.getVersion() );
        iud.setProperty( IInstallableUnit.PROP_NAME, p2LineupMasterIU.getProperty( IInstallableUnit.PROP_NAME ) );
        iud.setProperty( IInstallableUnit.PROP_DESCRIPTION,
                         p2LineupMasterIU.getProperty( IInstallableUnit.PROP_DESCRIPTION ) );

        iud.setProperty( IInstallableUnit.PROP_TYPE_CATEGORY, "true" );

        ArrayList<IRequiredCapability> capabilities = new ArrayList<IRequiredCapability>();
        VersionRange range = new VersionRange( "0.0.0" );
        capabilities.add( MetadataFactory.createRequiredCapability( IInstallableUnit.NAMESPACE_IU_ID,
                                                                    p2LineupMasterIU.getId(), range,
                                                                    p2LineupMasterIU.getFilter(), false /* optional */,
                                                                    !p2LineupMasterIU.isSingleton() /* multiple */,
                                                                    true /* greedy */) );
        iud.setRequiredCapabilities( capabilities.toArray( new IRequiredCapability[capabilities.size()] ) );

        IProvidedCapability providedCapability =
            MetadataFactory.createProvidedCapability( IInstallableUnit.NAMESPACE_IU_ID, iud.getId(), iud.getVersion() );
        iud.setCapabilities( new IProvidedCapability[] { providedCapability } );

        return MetadataFactory.createInstallableUnit( iud );
    }

    private void generateArtifactsByRepository( P2InternalResolutionResult p2ResolutionResult,
                                                TempArtifactRepository tempArtifactRepository,
                                                P2LineupResolutionRequest request, File destination )
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try
        {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();
            doc.setXmlStandalone( true );
            Element rootElement = doc.createElement( "repositories" );
            doc.appendChild( rootElement );
            Map<String, Set<IArtifactDescriptor>> artifactDescriptorsByRepository =
                p2ResolutionResult.getArtifactDescriptorsByRepository();
            for ( String repositoryId : artifactDescriptorsByRepository.keySet() )
            {
                getLogger().debug( "generateArtifactsByRepository: repositoryId:" + repositoryId );
                IArtifactRepository repository = p2ResolutionResult.getArtifactRepositoryById( repositoryId );
                URI repositoryURI = repository.getLocation();
                P2RepositoryData requestRepository = request.getSourceRepositoryById( repositoryId );
                Element repositoryElement = doc.createElement( "repository" );
                repositoryElement.setAttribute( "nexusRepositoryId", requestRepository.getNexusRepositoryId() );
                if ( requestRepository.getNexusRepositoryRelativePath() != null )
                {
                    repositoryElement.setAttribute( "nexusRepositoryRelativePath",
                                                    requestRepository.getNexusRepositoryRelativePath() );
                }
                rootElement.appendChild( repositoryElement );

                Set<IArtifactDescriptor> artifactDescriptors = artifactDescriptorsByRepository.get( repositoryId );
                if ( artifactDescriptors == null )
                {
                    continue;
                }
                nextArtifact: for ( IArtifactDescriptor artifactDescriptor : artifactDescriptors )
                {
                    getLogger().debug( "generateArtifactsByRepository: artifactDescriptor:" + artifactDescriptor );
                    Element artifactElement = doc.createElement( "artifact" );

                    String localPath;
                    String remotePath;
                    if ( repository instanceof SimpleArtifactRepository )
                    {
                        // p2 repository
                        SimpleArtifactRepository simpleRepository = (SimpleArtifactRepository) repository;
                        URI remoteArtifactUri = simpleRepository.getLocation( artifactDescriptor );
                        getLogger().debug( "generateArtifactsByRepository: remoteArtifactUri:" + remoteArtifactUri );
                        if ( remoteArtifactUri == null )
                        {
                            if ( "packed".equals( artifactDescriptor.getProperty( "format" ) ) )
                            {
                                // Some repositories contain packed artifacts,
                                // but they don't have rules to handle them,
                                // so the packed artifacts cannot be reached,
                                // but that's usually fine because the same
                                // artifact is available unpacked too (usually).
                                getLogger().info( "generateArtifactsByRepository: artifactDescriptor:"
                                                      + artifactDescriptor
                                                      + " is in packed format, but the repository cannot handle packed artifacts. Ignoring the artifact..." );
                                continue nextArtifact;
                            }
                            else
                            {
                                throw new P2FacadeInternalException( "Cannot get remote path for repository '"
                                    + repositoryId + "', artifact '" + artifactDescriptor + "'." );
                            }
                        }
                        else
                        {
                            remotePath = remoteArtifactUri.getPath();
                            if ( remotePath.startsWith( repositoryURI.getPath() ) )
                            {
                                remotePath = remotePath.substring( repositoryURI.getPath().length() );
                                localPath = remotePath;
                            }
                            else
                            {
                                throw new RuntimeException( "Could not get remote path for artifact "
                                    + artifactDescriptor );
                            }
                        }
                    }
                    else
                    {
                        throw new RuntimeException( "Unknown repository type "
                            + repository.getClass().getCanonicalName() );
                    }

                    artifactElement.setAttribute( "localPath", request.getLocalPathPrefix() + localPath );
                    artifactElement.setAttribute( "remotePath", remotePath );
                    repositoryElement.appendChild( artifactElement );
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            // transformerFactory.setAttribute( "indent-number", "2" );
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
            transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" );
            DOMSource source = new DOMSource( doc );
            OutputStream os = new FileOutputStream( destination );
            try
            {
                StreamResult result = new StreamResult( os );
                transformer.transform( source, result );
            }
            finally
            {
                os.close();
            }
        }
        catch ( Exception e )
        {
            throw new P2FacadeInternalException( "Could not generate p2 lineup artifact mappings file", e );
        }
    }

    private void generateArtifactsByRepository( List<IArtifactDescriptor> artifactDescriptors,
                                                Set<SimpleArtifactRepository> artifactRepositories, File destination )
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try
        {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();
            doc.setXmlStandalone( true );
            Element rootElement = doc.createElement( "repositories" );
            doc.appendChild( rootElement );
            for ( SimpleArtifactRepository repository : artifactRepositories )
            {
                getLogger().debug( "generateArtifactsByRepository: repositoryName:" + repository.getName() );
                URI repositoryURI = repository.getLocation();

                Element repositoryElement = doc.createElement( "repository" );
                rootElement.appendChild( repositoryElement );
                repositoryElement.setAttribute( "uri", repositoryURI.toString() );
                String mirrorsURL = (String) repository.getProperties().get( IRepository.PROP_MIRRORS_URL );
                if ( mirrorsURL != null )
                {
                    repositoryElement.setAttribute( IRepository.PROP_MIRRORS_URL, mirrorsURL );
                }

                Iterator<IArtifactDescriptor> iterArtifactDescriptors = artifactDescriptors.iterator();
                while ( iterArtifactDescriptors.hasNext() )
                {
                    IArtifactDescriptor artifactDescriptor = iterArtifactDescriptors.next();
                    getLogger().debug( "generateArtifactsByRepository: artifactDescriptor:" + artifactDescriptor );
                    if ( repository.contains( artifactDescriptor ) )
                    {
                        Element artifactElement = doc.createElement( "artifact" );
                        if ( "0".equals( artifactDescriptor.getProperty( "download.size" ) ) )
                        {
                            getLogger().info( "generateArtifactsByRepository: artifactDescriptor:" + artifactDescriptor
                                                  + " has download.size="
                                                  + artifactDescriptor.getProperty( "download.size" )
                                                  + ". Ignoring the artifact..." );
                            continue;
                        }
                        URI remoteArtifactUri = repository.getLocation( artifactDescriptor );
                        getLogger().debug( "generateArtifactsByRepository: remoteArtifactUri:" + remoteArtifactUri );
                        if ( remoteArtifactUri == null )
                        {
                            if ( "packed".equals( artifactDescriptor.getProperty( "format" ) ) )
                            {
                                // Some repositories contain packed artifacts,
                                // but they don't have rules to handle them,
                                // so the packed artifacts cannot be reached,
                                // but that's usually fine because the same
                                // artifact is available unpacked too (usually).
                                getLogger().info( "generateArtifactsByRepository: artifactDescriptor:"
                                                      + artifactDescriptor
                                                      + " is in packed format, but the repository cannot handle packed artifacts. Ignoring the artifact..." );
                            }
                            else
                            {
                                throw new P2FacadeInternalException( "Cannot get remote path for repository '"
                                    + repository.getName() + "', artifact '" + artifactDescriptor + "'." );
                            }
                        }
                        else
                        {
                            String remotePath = remoteArtifactUri.getPath();
                            if ( remotePath.startsWith( repositoryURI.getPath() ) )
                            {
                                remotePath = remotePath.substring( repositoryURI.getPath().length() );
                                if ( !remotePath.startsWith( "/" ) )
                                {
                                    remotePath = "/" + remotePath;
                                }
                                artifactElement.setAttribute( "remotePath", remotePath );

                                String md5 = artifactDescriptor.getProperty( "download.md5" );
                                if ( md5 != null )
                                {
                                    artifactElement.setAttribute( "md5", md5 );
                                }

                                repositoryElement.appendChild( artifactElement );
                            }
                            else
                            {
                                throw new RuntimeException( "Could not get remote path for artifact "
                                    + artifactDescriptor );
                            }

                            iterArtifactDescriptors.remove();
                        }
                    }
                }
            }
            rootElement.setAttribute( "size", "" + artifactRepositories.size() );

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            // transformerFactory.setAttribute( "indent-number", "2" );
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
            transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" );
            DOMSource source = new DOMSource( doc );
            OutputStream os = new FileOutputStream( destination );
            try
            {
                StreamResult result = new StreamResult( os );
                transformer.transform( source, result );
            }
            finally
            {
                os.close();
            }
        }
        catch ( Exception e )
        {
            throw new P2FacadeInternalException( "Could not generate p2 lineup artifact mappings file", e );
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

    public boolean containsExecutable( File metadataRepository )
    {
        IMetadataRepositoryManager metadataRepositoryManager =
            (IMetadataRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                   IMetadataRepositoryManager.class.getName() );
        IMetadataRepository repo = null;
        URI metadataRepositoryURI;
        try
        {
            metadataRepositoryURI = file2Url( metadataRepository );
        }
        catch ( URISyntaxException e )
        {
            // This should not happen since the repository is generated by us
            throw new P2FacadeInternalException( "Exception while loading the repository "
                + metadataRepository.toString() + ": " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            // This should not happen since the repository is generated by us
            throw new P2FacadeInternalException( "Exception while loading the repository "
                + metadataRepository.toString() + ": " + e.getMessage(), e );
        }
        boolean alreadyLoaded = metadataRepositoryManager.contains( metadataRepositoryURI );
        try
        {
            try
            {
                repo = metadataRepositoryManager.loadRepository( metadataRepositoryURI, new NullProgressMonitor() );
            }
            catch ( ProvisionException e )
            {
                // This should not happen since the repository is generated by us
                throw new P2FacadeInternalException( "Exception while loading the repository " + metadataRepositoryURI
                    + ": " + e.getMessage(), e );
            }
            return !repo.query( InstallableUnitQuery.ANY, new Collector()
            {
                @Override
                public boolean accept( Object object )
                {
                    if ( !( object instanceof IInstallableUnit ) )
                    {
                        return true;
                    }
                    IInstallableUnit iu = (IInstallableUnit) object;
                    if ( iu.getId().startsWith( "org.eclipse.rcp.configuration_root" ) )
                    {
                        super.accept( object );
                        return false;
                    }
                    return true;
                }
            }, new NullProgressMonitor() ).isEmpty();
        }
        finally
        {
            if ( !alreadyLoaded )
            {
                metadataRepositoryManager.removeRepository( metadataRepositoryURI );
            }
        }
    }

    public P2Resolver createResolver()
    {
        return new P2ResolverImpl();
    }

    private P2Logger logger = null;

    private P2Logger getLogger()
    {
        if ( logger != null )
        {
            return logger;
        }

        logger = new P2Logger()
        {
            public void info( String message )
            {
                System.out.println( message );
            }

            public void debug( String message )
            {
                System.out.println( message );
            }
        };
        return logger;
    }

    public void setLogger( P2Logger logger )
    {
        this.logger = logger;
    }
}
