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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;

@SuppressWarnings( "restriction" )
public abstract class AbstractNexusP2ImplTest
{
    protected IArtifactRepositoryManager getArtifactRepositoryManager()
    {
        IArtifactRepositoryManager repositoryManager =
            (IArtifactRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                   IArtifactRepositoryManager.class.getName() );
        return repositoryManager;
    }

    protected IMetadataRepositoryManager getMetadataRepositoryManager()
    {
        IMetadataRepositoryManager repositoryManager =
            (IMetadataRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                   IMetadataRepositoryManager.class.getName() );
        return repositoryManager;
    }

    protected void assertSameKnownRepositories( URI[] reposArray1, URI[] reposArray2 )
    {
        List<URI> repos2 = new ArrayList<URI>();
        repos2.addAll( Arrays.asList( reposArray2 ) );
        for ( URI repo : reposArray1 )
        {
            if ( !repos2.contains( repo ) )
            {
                Assert.fail( "Repository " + repo + " disappeared!" );
            }
            repos2.remove( repo );
        }
        if ( repos2.size() > 0 )
        {
            Assert.fail( "Repository " + repos2.get( 0 ) + " has leaked!" );
        }
    }

}
