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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.sonatype.tycho.p2.facade.internal.P2Logger;

@SuppressWarnings( "restriction" )
public class P2InternalResolutionResult
{
    private final SortedSet<IInstallableUnit> installableUnits =
        new TreeSet<IInstallableUnit>( new Comparator<IInstallableUnit>()
        {
            public int compare( IInstallableUnit iu1, IInstallableUnit iu2 )
            {
                int compareIds = iu1.getId().compareTo( iu2.getId() );
                if ( compareIds != 0 )
                {
                    return compareIds;
                }
                return iu1.getVersion().compareTo( iu2.getVersion() );
            }
        } );

    private Map<String, IArtifactRepository> artifactRepositories = new LinkedHashMap<String, IArtifactRepository>();
    private Set<IArtifactDescriptor> artifactDescriptors;

    private Map<String, Set<IArtifactDescriptor>> artifactDescriptorsByRepository;
    
    public void addInstallableUnit( IInstallableUnit iu )
    {
        installableUnits.add( iu );
    }

    public Set<IInstallableUnit> getInstallableUnits()
    {
        return installableUnits;
    }
    
    public void addArtifactRepository( String repositoryId, IArtifactRepository repository )
    {
        artifactRepositories.put( repositoryId, repository );
    }

    public Set<IArtifactDescriptor> getArtifactDescriptors()
    {
        if (artifactDescriptors != null)
        {
            return artifactDescriptors;
        }
        
        loadArtifactDescriptors();
        return artifactDescriptors;
    }

    public Map<String, Set<IArtifactDescriptor>> getArtifactDescriptorsByRepository()
    {
        if ( artifactDescriptorsByRepository != null )
        {
            return artifactDescriptorsByRepository;
        }

        loadArtifactDescriptors();
        return artifactDescriptorsByRepository;
    }

    private void loadArtifactDescriptors()
    {
        artifactDescriptors = new LinkedHashSet<IArtifactDescriptor>();
        artifactDescriptorsByRepository = new LinkedHashMap<String, Set<IArtifactDescriptor>>();
        for ( IInstallableUnit iu : getInstallableUnits() )
        {
            getLogger().debug( "loadArtifactDescriptors: installable unit: " + iu );
            for ( IArtifactKey artifactKey : iu.getArtifacts() )
            {
                getLogger().debug( "loadArtifactDescriptors: artifact key: " + artifactKey );
                boolean found = false;
                for ( String repositoryId : artifactRepositories.keySet() )
                {
                    IArtifactRepository repository = artifactRepositories.get( repositoryId );
                    Set<IArtifactDescriptor> repositoryArtifactDescriptors =
                        artifactDescriptorsByRepository.get( repositoryId );
                    if ( repositoryArtifactDescriptors == null )
                    {
                        repositoryArtifactDescriptors = new LinkedHashSet<IArtifactDescriptor>();
                        artifactDescriptorsByRepository.put( repositoryId, repositoryArtifactDescriptors );
                    }
                    if ( repository.contains( artifactKey ) )
                    {
                        IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors( artifactKey );
                        for ( IArtifactDescriptor descriptor : descriptors )
                        {
                            artifactDescriptors.add( descriptor );
                            repositoryArtifactDescriptors.add( descriptor );
                        }
                        found = true;
                        getLogger().debug(
                                           "loadArtifactDescriptors: found artifact key: " + artifactKey + " in repository: "
                                               + repository.getName() );
                        break;
                    }
                }
                if ( !found )
                {
                    throw new RuntimeException( "Could not find artifact " + artifactKey );
                }
            }
        }
    }

    public IArtifactRepository getArtifactRepositoryById( String repositoryId )
    {
        return artifactRepositories.get( repositoryId );
    }

    public void merge( P2InternalResolutionResult newP2ResolutionResult )
    {
        // Very incomplete, but good enough for now :)
        installableUnits.addAll( newP2ResolutionResult.installableUnits );
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
