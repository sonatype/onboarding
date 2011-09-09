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
package com.sonatype.nexus.p2.facade.internal;

import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public class P2LineupResolutionRequest
{
    private String id;

    private String version;

    private String name;

    private String description;

    private Set<P2RepositoryData> sourceRepositories = new LinkedHashSet<P2RepositoryData>();

    private Set<P2InstallableUnitData> rootInstallableUnits = new LinkedHashSet<P2InstallableUnitData>();

    private P2AdviceData p2Advice;

    private Set<Properties> targetEnvironments = new LinkedHashSet<Properties>();

    private Set<DependencyData> dependencies = new LinkedHashSet<DependencyData>();

    private String localPathPrefix;

    public String getLocalPathPrefix()
    {
        return localPathPrefix;
    }

    public void setLocalPathPrefix( String localPathPrefix )
    {
        this.localPathPrefix = localPathPrefix;
    }

    public Set<P2RepositoryData> getSourceRepositories()
    {
        return sourceRepositories;
    }

    public Set<P2InstallableUnitData> getRootInstallableUnits()
    {
        return rootInstallableUnits;
    }

    public Set<Properties> getTargetEnvironments()
    {
        return targetEnvironments;
    }

    public void cleanup()
    {
        for ( P2RepositoryData repositoryData : sourceRepositories )
        {
            repositoryData.cleanup();
        }
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public Set<DependencyData> getDependencies()
    {
        return dependencies;
    }

    public void setDependencies( Set<DependencyData> dependencies )
    {
        this.dependencies = dependencies;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public P2AdviceData getP2Advice()
    {
        return p2Advice;
    }

    public void setP2Advice( P2AdviceData p2Advice )
    {
        this.p2Advice = p2Advice;
    }

    public P2RepositoryData getSourceRepositoryById( String repositoryId )
    {
        for ( P2RepositoryData repositoryData : sourceRepositories )
        {
            if ( repositoryId.equals( repositoryData.getId() ) )
            {
                return repositoryData;
            }
        }
        return null;
    }
}
