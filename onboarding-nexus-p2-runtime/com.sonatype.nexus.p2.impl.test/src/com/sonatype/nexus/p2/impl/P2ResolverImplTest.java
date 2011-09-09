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
import java.net.URI;

import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.junit.Test;

@SuppressWarnings( "restriction" )
public class P2ResolverImplTest
    extends AbstractNexusP2ImplTest
{
    @Test
    public void testCleanupRepositories()
    {
        URI repoURI = new File( "resources/p2repo" ).toURI();

        IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
        artifactRepositoryManager.removeRepository( repoURI );
        URI[] knownArtifactRepositories =
            artifactRepositoryManager.getKnownRepositories( IArtifactRepositoryManager.REPOSITORIES_ALL );

        IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
        metadataRepositoryManager.removeRepository( repoURI );
        URI[] knownMetadataRepositories =
            metadataRepositoryManager.getKnownRepositories( IMetadataRepositoryManager.REPOSITORIES_ALL );

        P2ResolverImpl p2Resolver = new P2ResolverImpl();
        p2Resolver.addP2Repository( "abc" /* repositoryId */, repoURI );
        p2Resolver.cleanupRepositories();

        assertSameKnownRepositories(
                                     knownArtifactRepositories,
                                     artifactRepositoryManager.getKnownRepositories( IArtifactRepositoryManager.REPOSITORIES_ALL ) );

        assertSameKnownRepositories(
                                     knownMetadataRepositories,
                                     metadataRepositoryManager.getKnownRepositories( IMetadataRepositoryManager.REPOSITORIES_ALL ) );
    }
}
