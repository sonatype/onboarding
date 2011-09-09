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
import org.junit.Test;

@SuppressWarnings( "restriction" )
public class P2FacadeInternalImplTest
    extends AbstractNexusP2ImplTest
{
    @Test
    public void getRepositoryArtifacts()
        throws Exception
    {
        URI repoURI = new File( "resources/p2repo" ).toURI();

        IArtifactRepositoryManager repositoryManager = getArtifactRepositoryManager();
        repositoryManager.removeRepository( repoURI );
        URI[] knownRepositories = repositoryManager.getKnownRepositories( IArtifactRepositoryManager.REPOSITORIES_ALL );

        P2FacadeInternalImpl p2FacadeInternalImpl = new P2FacadeInternalImpl();

        File destination = File.createTempFile( "unittest_P2FacadeInternalImplTest_getRepositoryArtifacts", ".xml" );
        destination.deleteOnExit();
        File artifactMappingsXmlFile =
            File.createTempFile( "unittest_P2FacadeInternalImplTest_getRepositoryArtifacts", ".xml" );
        artifactMappingsXmlFile.deleteOnExit();
        p2FacadeInternalImpl.getRepositoryArtifacts( repoURI.toString(),
                                                     null /* username */,
                                                     null /* password */,
                                                     destination, artifactMappingsXmlFile );

        assertSameKnownRepositories(
                                     knownRepositories,
                                     repositoryManager.getKnownRepositories( IArtifactRepositoryManager.REPOSITORIES_ALL ) );
    }
}
