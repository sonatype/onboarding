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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.director.Projector;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.director.SimplePlanner;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.p2.director.Explanation.MissingIU;
import org.eclipse.equinox.internal.p2.publisher.SingleElementCollector;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.ITouchpointData;
import org.eclipse.equinox.internal.provisional.p2.metadata.ITouchpointType;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.osgi.framework.InvalidSyntaxException;
import org.sonatype.tycho.p2.facade.internal.P2Logger;

import com.sonatype.nexus.p2.auth.P2AuthSession;
import com.sonatype.nexus.p2.facade.internal.P2AdviceData;
import com.sonatype.nexus.p2.facade.internal.P2LineupResolutionResult;
import com.sonatype.nexus.p2.facade.internal.P2Resolver;
import com.sonatype.nexus.p2.rest.model.P2LineupUnresolvedInstallableUnit;

@SuppressWarnings( "restriction" )
public class P2ResolverImpl
    implements P2Resolver
{
    private static final IInstallableUnit[] IU_ARRAY = new IInstallableUnit[0];

    private static final IRequiredCapability[] REQUIRED_CAPABILITY_ARRAY = new IRequiredCapability[0];

    /**
     * All known P2 metadata repositories
     */
    private List<IMetadataRepository> metadataRepositories = new ArrayList<IMetadataRepository>();

    /**
     * All known P2 artifact repositories
     */
    private Map<String, IArtifactRepository> artifactRepositories = new LinkedHashMap<String, IArtifactRepository>();
    
    private Properties properties;

    private List<IRequiredCapability> additionalRequirements = new ArrayList<IRequiredCapability>();

    private IProgressMonitor monitor = new NullProgressMonitor();
    
    private Set<IRequiredCapability> requiredCapabilities = new LinkedHashSet<IRequiredCapability>();

    private List<URI> metadataRepositoriesToRemove = new ArrayList<URI>();

    private List<URI> artifactRepositoriesToRemove = new ArrayList<URI>();

    public void addMetadataRepository( IMetadataRepository repository )
    {
        getLogger().debug(
                           "Adding metadata repository: name=" + repository.getName() + ", URI="
                               + repository.getLocation()
            + ", class=" + repository.getClass() );
        metadataRepositories.add( repository );
    }

    public void addMetadataRepository( URI location )
    {
        IMetadataRepositoryManager metadataRepositoryManager =
            (IMetadataRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                   IMetadataRepositoryManager.class.getName() );
        if ( metadataRepositoryManager == null )
        {
            throw new IllegalStateException( "No metadata repository manager found" ); //$NON-NLS-1$
        }
        
        try
        {
            if ( !metadataRepositoryManager.contains( location ) )
            {
                metadataRepositoriesToRemove.add( location );
            }
            IMetadataRepository metadataRepository = metadataRepositoryManager.loadRepository( location, monitor );
            addMetadataRepository( metadataRepository );

            // processPartialIUs( metadataRepository, artifactRepository );
        }
        catch ( ProvisionException e )
        {
            throw new RuntimeException( e );
        }
    }

    public void addArtifactRepository( String repositoryId, IArtifactRepository repository )
    {
        getLogger().debug(
                           "Adding artifact repository: name=" + repository.getName() + ", URI="
                               + repository.getLocation()
            + ", class=" + repository.getClass() );
        artifactRepositories.put( repositoryId, repository );
    }

    public void addArtifactRepository( String repositoryId, URI location )
    {
        IArtifactRepositoryManager artifactRepositoryManager =
            (IArtifactRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                   IArtifactRepositoryManager.class.getName() );
        if ( artifactRepositoryManager == null )
        {
            throw new IllegalStateException( "No artifact repository manager found" ); //$NON-NLS-1$
        }
        
        try
        {
            if ( !artifactRepositoryManager.contains( location ) )
            {
                artifactRepositoriesToRemove.add( location );
            }
            IArtifactRepository artifactRepository = artifactRepositoryManager.loadRepository( location, monitor );
            addArtifactRepository( repositoryId, artifactRepository );

            // processPartialIUs( metadataRepository, artifactRepository );
        }
        catch ( ProvisionException e )
        {
            throw new RuntimeException( e );
        }
    }

    public void addP2Repository( String repositoryId, URI location )
    {
        addMetadataRepository( location );
        addArtifactRepository( repositoryId, location );
    }

    public void cleanupRepositories()
    {
        if ( artifactRepositoriesToRemove.size() > 0 )
        {
            IArtifactRepositoryManager artifactRepositoryManager =
                (IArtifactRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                       IArtifactRepositoryManager.class.getName() );
            for ( URI uri : artifactRepositoriesToRemove )
            {
                artifactRepositoryManager.removeRepository( uri );
            }
        }
        if ( metadataRepositoriesToRemove.size() > 0 )
        {
            IMetadataRepositoryManager metadataRepositoryManager =
                (IMetadataRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                       IMetadataRepositoryManager.class.getName() );
            for ( URI uri : metadataRepositoriesToRemove )
            {
                metadataRepositoryManager.removeRepository( uri );
            }
        }
    }

    private P2Logger logger = null;
    private P2Logger getLogger()
    {
        if (logger != null)
        {
            return logger;
        }
        
        logger = new P2Logger() {
            public void info( String message )
            {
                System.out.println(message);
            }
            
            public void debug( String message )
            {
                System.out.println(message);
            }
        };
        return logger;
    }
    
    public void setLogger( P2Logger logger )
    {
        this.logger = logger;
        this.monitor = new LoggingProgressMonitor( logger );
    }

    private IRequiredCapability createRequiredCapability( String iuId, String iuVersion, String filter )
    {
        VersionRange versionRange = null;
        if ( iuVersion != null )
        {
            versionRange = new VersionRange( iuVersion );
        }

        IRequiredCapability requiredCapability =
            MetadataFactory.createRequiredCapability( IInstallableUnit.NAMESPACE_IU_ID, iuId, versionRange,
                                                      filter /* filter */, false /* optional */, true /* multiple */,
                                                      true /* greedy */);
        return requiredCapability;
    }

    private IRequiredCapability createRequiredCapability( String iuId, String iuVersion,
                                                          Set<Properties> iuTargetEnvironments )
    {
        if ( iuTargetEnvironments.size() == 0 )
        {
            iuTargetEnvironments.add( new Properties() );
        }
        String filter = targetEnvironmentToFilterString( iuTargetEnvironments );

        return createRequiredCapability( iuId, iuVersion, filter );
    }

    public void addRootInstallableUnit( IInstallableUnit installableUnit )
    {
        IRequiredCapability requiredCapability =
            createRequiredCapability( installableUnit.getId(), installableUnit.getVersion().toString(),
                                      installableUnit.getFilter() );
        addRequiredCapability( requiredCapability );
    }

    private void addRequiredCapability( IRequiredCapability requiredCapability )
    {
        getLogger().debug(
                           "Adding required capability: id=" + requiredCapability.getName() + ", version="
                               + requiredCapability.getRange() + ", filter=" + requiredCapability.getFilter() );
        requiredCapabilities.add( requiredCapability );
    }
    
    public void addRootInstallableUnit( String iuId, String iuName, String iuVersion,
                                        Set<Properties> iuTargetEnvironments,
                                        P2LineupResolutionResult result )
    {
        getLogger().debug( "Adding root IU: id=" + iuId + ", version=" + iuVersion );

        VersionRange versionRange = null;
        if ( iuVersion != null )
        {
            versionRange = new VersionRange( iuVersion );
        }

        IRequiredCapability requiredCapability =
            createRequiredCapability( iuId, versionRange.toString(), iuTargetEnvironments );
        addRequiredCapability( requiredCapability );

        Query query = null;
        if ( versionRange != null && !VersionRange.emptyRange.equals( versionRange ) )
        {
            query = new InstallableUnitQuery( iuId, versionRange );
        }
        else
        {
            query = new InstallableUnitQuery( iuId );
        }

        if ( iuTargetEnvironments.size() == 0 )
        {
            iuTargetEnvironments.add( new Properties() );
        }

        Set<Properties> unresolvedIUTargetEnvironments = new LinkedHashSet<Properties>();
        for ( Properties iuTargetEnvironment : iuTargetEnvironments )
        {
            IInstallableUnit selectedIU = findIU( versionRange, iuTargetEnvironment, query );
            if ( selectedIU == null )
            {
                unresolvedIUTargetEnvironments.add( iuTargetEnvironment );
            }
        }

        if ( unresolvedIUTargetEnvironments.size() > 0 )
        {
            String message = "Missing requirement: '" + iuName + "', version " + versionRange.toString();
            for ( Properties iuTargetEnvironment : unresolvedIUTargetEnvironments )
            {
                if ( iuTargetEnvironment.size() > 0 )
                {
                    message += ", target environment " + iuTargetEnvironment;
                }
            }
            result.addUnresolvedInstallableUnit( new P2LineupUnresolvedInstallableUnit( iuId, versionRange.toString(),
                                                                                        message ) );
        }
    }

    private IInstallableUnit findIU( VersionRange versionRange, Properties iuTargetEnvironment, Query query )
    {
        IInstallableUnit selectedIU = null;
        for ( IMetadataRepository repository : metadataRepositories )
        {
            Collector collector = null;
            if ( versionRange != null && !VersionRange.emptyRange.equals( versionRange ) )
            {
                collector = new SingleElementCollector();
            }
            else
            {
                collector = new Collector();
            }

            Collector newCollector = repository.query( query, collector, monitor );
            if ( !newCollector.isEmpty() )
            {
                Iterator<IInstallableUnit> iuIter = collector.iterator();
                while ( iuIter.hasNext() )
                {
                    IInstallableUnit matchingIU = iuIter.next();
                    if ( isApplicable( iuTargetEnvironment, matchingIU.getFilter() ) )
                    {
                        getLogger().debug(
                                           "Matching installable unit: " + matchingIU + ", filter "
                                               + matchingIU.getFilter() );
                        if ( selectedIU == null || selectedIU.compareTo( matchingIU ) < 0 )
                        {
                            selectedIU = matchingIU;
                        }
                    }
                }
            }
        }

        if ( selectedIU != null )
        {
            getLogger().debug( "Selected installable unit: " + selectedIU + ", filter " + selectedIU.getFilter() );
        }
        return selectedIU;
    }

    private String targetEnvironmentToFilterString( Set<Properties> targetEnvironments )
    {
        StringBuilder filterBuffer = new StringBuilder();
        if ( targetEnvironments.size() > 1 )
        {
            filterBuffer.append( "(|" );
        }
        for ( Properties targetEnvironment : targetEnvironments )
        {
            if ( targetEnvironment.size() == 0 )
            {
                // Empty target environment
                continue;
            }

            if ( targetEnvironment.size() > 1 )
            {
                filterBuffer.append( "(&" );
            }
            for ( Object keyObject : targetEnvironment.keySet() )
            {
                String key = (String) keyObject;
                String value = targetEnvironment.getProperty( key );
                if ( value != null && value.trim().length() > 0 )
                {
                    filterBuffer.append( '(' ).append( key ).append( '=' ).append( value ).append( ')' );
                }
            }
            if ( targetEnvironment.size() > 1 )
            {
                filterBuffer.append( ')' );
            }
        }
        if ( targetEnvironments.size() > 1 )
        {
            filterBuffer.append( ')' );
        }

        if ( filterBuffer.length() == 0 )
        {
            return null;
        }

        try
        {
            return DirectorActivator.context.createFilter( filterBuffer.toString() ).toString();
        }
        catch ( InvalidSyntaxException e )
        {
            throw new RuntimeException( e );
        }
    }

    public void setProperties( Properties properties )
    {
        this.properties = new Properties();
        this.properties.putAll( properties );
    }
    
    private Properties getProperties()
    {
        if ( properties == null )
        {
            properties = new Properties();
        }
        return properties;
    }

    public P2InternalResolutionResult resolve(P2LineupResolutionResult externalResult)
    {
        long start = System.currentTimeMillis();

        P2InternalResolutionResult result = new P2InternalResolutionResult();
        result.setLogger( getLogger() );

        // Add all known artifact repositories to the resolution result
        for ( String repositoryId : artifactRepositories.keySet() )
        {
            result.addArtifactRepository( repositoryId, artifactRepositories.get( repositoryId ) );
        }

        Dictionary<?, ?> newSelectionContext = SimplePlanner.createSelectionContext( getProperties() );

        IInstallableUnit[] availableIUs = gatherAvailableInstallableUnits();

        Set<IInstallableUnit> extraIUs = createAdditionalRequirementsIU();

        Set<IInstallableUnit> rootWithExtraIUs = new LinkedHashSet<IInstallableUnit>();
        IInstallableUnit masterIU = getMasterInstallableUnit();
        rootWithExtraIUs.add( masterIU );
        rootWithExtraIUs.addAll( extraIUs );

        Slicer slicer = new Slicer( new QueryableArray( availableIUs ), newSelectionContext, false );
        IQueryable slice = slicer.slice( rootWithExtraIUs.toArray( IU_ARRAY ), monitor );

        InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
        iud.setId(  Long.toString( System.currentTimeMillis() ) );
        iud.setSingleton(true);

        ArrayList<IRequiredCapability> capabilities = new ArrayList<IRequiredCapability>();
        capabilities.add( createRequiredCapability(masterIU.getId(), new VersionRange(masterIU.getVersion(), true, masterIU.getVersion(), true).toString(), (String) null) );

        iud.setRequiredCapabilities( capabilities.toArray( new IRequiredCapability[capabilities.size()] ) );
        
        IInstallableUnit fakeMasterIU = MetadataFactory.createInstallableUnit( iud );
        
        if ( slice != null )
        {
            Projector projector = new Projector( slice, newSelectionContext, false );
            projector.encode( fakeMasterIU,
                              IU_ARRAY, //extraIUs.toArray( IU_ARRAY ) /* alreadyExistingRoots */,
                              IU_ARRAY /* newRoots */,
                              monitor );
            IStatus s = projector.invokeSolver( monitor );
            if ( s.getSeverity() == IStatus.ERROR )
            {
                Set<Explanation> explanations = projector.getExplanation( monitor );
                getLogger().debug( "" + explanations );

                for ( Explanation explanation : explanations )
                {
                    int type = -1;
                    try
                    {
                        type = explanation.shortAnswer();
                    }
                    catch ( UnsupportedOperationException e )
                    {
                        // That's ok
                    }
                    if ( type == -1 )
                    {
                        continue;
                    }
                    if ( type == Explanation.MISSING_REQUIREMENT )
                    {
                        IRequiredCapability missingIU = ( (MissingIU) explanation ).req;
                        externalResult.addUnresolvedInstallableUnit( new P2LineupUnresolvedInstallableUnit(
                                                                                                            missingIU.getName(),
                                                                                                            missingIU.getRange().toString(),
                                                                                                            explanation.toString() ) );
                    }
                    else
                    {
                        throw new RuntimeException( new ProvisionException( s ) );
                    }
                }

                return null;
            }
            Collection<IInstallableUnit> newState = projector.extractSolution();

            fixSWT( newState, availableIUs, newSelectionContext );

            getLogger().debug( "P2 resolution result contains " + newState.size() + " installable units:" );
            for ( IInstallableUnit iu : newState )
            {
                getLogger().debug( "\t" + iu.toString() );
                result.addInstallableUnit( iu );
            }
        }

        getLogger().debug( "P2ResolverImpl.resolve finished in " + ( System.currentTimeMillis() - start ) + " ms." );

        return result;
    }
    
    public IInstallableUnit[] gatherAvailableInstallableUnits()
    {
        Set<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();
        
        SubMonitor sub = SubMonitor.convert( monitor, metadataRepositories.size() * 200 );
        for ( IMetadataRepository repository : metadataRepositories )
        {
            Collector matches = repository.query( InstallableUnitQuery.ANY, new Collector(), sub.newChild( 100 ) );
            if ( matches.isEmpty() )
            {
                throw new RuntimeException( "Repository '" + repository.getName() + "' does not contain any installable units." );
            }
            getLogger().debug( "Repository '" + repository.getName() + "': available installable units:" );
            for ( Iterator<IInstallableUnit> it = matches.iterator(); it.hasNext(); )
            {
                IInstallableUnit iu = it.next();
                getLogger().debug( "\t" + iu );

                if ( !PartialInstallableUnitsQuery.isPartialIU( iu ) )
                {
                    result.add( iu );
                }
                else
                {
                    getLogger().debug( "PARTIAL IU: " + iu );
                }
            }
        }
        sub.done();
        return result.toArray( IU_ARRAY );
    }

    private IInstallableUnit masterInstallableUnit;

    public IInstallableUnit getMasterInstallableUnit()
    {
        if ( masterInstallableUnit == null )
        {
            masterInstallableUnit = createMasterInstallableUnit();
        }
        return masterInstallableUnit;
    }

    private IInstallableUnit createMasterInstallableUnit()
    {
        return createMasterInstallableUnit( null /* installableUnitId */, null /* version */, null /* name */,
                                            null /* description */, null /* p2Advice */);
    }

    public IInstallableUnit createMasterInstallableUnit( String installableUnitId, String version, String name,
                                                         String description, P2AdviceData p2Advice )
    {
        InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
        if ( installableUnitId == null )
        {
            installableUnitId = Long.toString( System.currentTimeMillis() );
        }
        iud.setId( installableUnitId );

        if ( version == null )
        {
            iud.setVersion( Version.createOSGi( 0, 0, 0, installableUnitId ) );
        }
        else
        {
            iud.setVersion( Version.parseVersion( version ) );
        }
        if ( name != null )
        {
            iud.setProperty( IInstallableUnit.PROP_NAME, name );
        }
        if ( description != null )
        {
            iud.setProperty( IInstallableUnit.PROP_DESCRIPTION, description );
        }
        iud.setSingleton(true);
        iud.setProperty( IInstallableUnit.PROP_TYPE_GROUP, "true" );

        ArrayList<IRequiredCapability> capabilities = new ArrayList<IRequiredCapability>();
        capabilities.addAll( requiredCapabilities );
        capabilities.addAll( additionalRequirements );

        iud.setRequiredCapabilities( capabilities.toArray( new IRequiredCapability[capabilities.size()] ) );

        IProvidedCapability providedCapability =
            MetadataFactory.createProvidedCapability( IInstallableUnit.NAMESPACE_IU_ID, iud.getId(), iud.getVersion() );
        iud.setCapabilities( new IProvidedCapability[] { providedCapability } );

        if ( p2Advice != null )
        {
            getLogger().debug(
                               "Found p2 advice for touchpoint id=" + p2Advice.getTouchpointId() + ", version="
                                   + p2Advice.getTouchpointVersion() );
            ITouchpointType touchpoint =
                MetadataFactory.createTouchpointType( p2Advice.getTouchpointId(),
                                                      Version.parseVersion( p2Advice.getTouchpointVersion() ) );
            iud.setTouchpointType( touchpoint );

            try
            {
                for ( String advice : p2Advice.getAdvices() )
                {
                    Map<String, String> touchpointDataMap = new LinkedHashMap<String, String>();
                    getLogger().debug( "p2 advice:" + advice );
                    Properties adviceProperties = new Properties();
                    InputStream adviceReader = new ByteArrayInputStream( advice.getBytes( "UTF-8" ) );
                    try
                    {
                        adviceProperties.load( adviceReader );
                        for ( Object key : adviceProperties.keySet() )
                        {
                            String advicePropertyKey = (String) key;
                            touchpointDataMap.put( advicePropertyKey, adviceProperties.getProperty( advicePropertyKey ) );
                        }
                    }
                    finally
                    {
                        adviceReader.close();
                    }
                    ITouchpointData touchpointData = MetadataFactory.createTouchpointData( touchpointDataMap );
                    iud.addTouchpointData( touchpointData );
                }
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
        else
        {
            getLogger().debug( "p2Advice=null" );
        }

        masterInstallableUnit = MetadataFactory.createInstallableUnit( iud );
        return masterInstallableUnit;
    }

    private void fixSWT( Collection<IInstallableUnit> ius, IInstallableUnit[] availableIUs, Dictionary<?, ?> selectionContext )
    {
        boolean swt = false;
        for ( IInstallableUnit iu : ius )
        {
            if ( "org.eclipse.swt".equals( iu.getId() ) )
            {
                swt = true;
                break;
            }
        }

        if ( !swt )
        {
            return;
        }

        IInstallableUnit swtFragment = null;

        all_ius: for ( IInstallableUnit iu : availableIUs )
        {
            if ( iu.getId().startsWith( "org.eclipse.swt" ) && isApplicable( selectionContext, iu.getFilter() ) )
            {
                for ( IProvidedCapability provided : iu.getProvidedCapabilities() )
                {
                    if ( "osgi.fragment".equals( provided.getNamespace() )
                        && "org.eclipse.swt".equals( provided.getName() ) )
                    {
                        if ( swtFragment == null || swtFragment.getVersion().compareTo( iu.getVersion() ) < 0 )
                        {
                            swtFragment = iu;
                        }
                        continue all_ius;
                    }
                }
            }
        }

        if ( swtFragment == null )
        {
            throw new RuntimeException( "Could not determine SWT implementation fragment bundle" );
        }

        ius.add( swtFragment );
    }

    protected boolean isApplicable( Dictionary<?, ?> selectionContext, String filter )
    {
        if ( filter == null )
        {
            return true;
        }

        try
        {
            return DirectorActivator.context.createFilter( filter ).match( selectionContext );
        }
        catch ( InvalidSyntaxException e )
        {
            return false;
        }
    }

    public void addDependency( String type, String id, String version )
    {
        if ( P2Resolver.TYPE_INSTALLABLE_UNIT.equals( type ) )
        {
            additionalRequirements.add( MetadataFactory.createRequiredCapability( IInstallableUnit.NAMESPACE_IU_ID,
                                                                                  id,
                                                                                  new VersionRange( version ),
                                                                                  null,
                                                                                  false,
                                                                                  true ) );
        }
        else if ( P2Resolver.TYPE_OSGI_BUNDLE.equals( type ) )
        {
            // BundlesAction#CAPABILITY_NS_OSGI_BUNDLE
            additionalRequirements.add( MetadataFactory.createRequiredCapability( "osgi.bundle",
                                                                                  id,
                                                                                  new VersionRange( version ),
                                                                                  null,
                                                                                  false,
                                                                                  true ) );
        }
    }

    private Set<IInstallableUnit> createAdditionalRequirementsIU()
    {
        LinkedHashSet<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();

        if ( !additionalRequirements.isEmpty() )
        {
            InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
            String time = Long.toString( System.currentTimeMillis() );
            iud.setId( "extra-" + time );
            iud.setVersion( Version.createOSGi( 0, 0, 0, time ) );
            iud.setRequiredCapabilities( additionalRequirements.toArray( REQUIRED_CAPABILITY_ARRAY ) );

            result.add( MetadataFactory.createInstallableUnit( iud ) );
        }

        return result;
    }

    private P2AuthSession authSession;

    public void setCredentials( URI location, String username, String password )
    {
        if ( authSession == null )
        {
            authSession = new P2AuthSession();
        }
        authSession.setCredentials( location, username, password );
    }

    public void cleanCredentials()
    {
        if ( authSession == null )
        {
            return;
        }
        authSession.cleanup();
    }
}
