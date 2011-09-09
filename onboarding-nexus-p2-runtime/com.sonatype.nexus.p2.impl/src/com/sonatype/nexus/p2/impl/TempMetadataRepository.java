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

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;

@SuppressWarnings( "restriction" )
public class TempMetadataRepository
    extends AbstractMetadataRepository
{
    private final Set<IInstallableUnit> installableUnits = new LinkedHashSet<IInstallableUnit>();

    public TempMetadataRepository() {
        type = LocalMetadataRepository.class.getName();
        version = "1";
    }
    
    @Override
    public void initialize( RepositoryState state )
    {
    }

    public Collector query( Query query, Collector collector, IProgressMonitor monitor )
    {
        return query.perform( installableUnits.iterator(), collector );
    }

    @Override
    public boolean isModifiable()
    {
        return true;
    }

    @Override
    public void addInstallableUnits( IInstallableUnit[] installableUnits )
    {
        for ( IInstallableUnit iu : installableUnits )
        {
            this.installableUnits.add( iu );
        }
    }

    public void addInstallableUnit( IInstallableUnit masterIU )
    {
        installableUnits.add( masterIU );
    }
}
