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
package com.sonatype.nexus.p2.proxy;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.Nexus;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.AbstractNexusTestCase;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;
import org.sonatype.security.SecuritySystem;

import com.sonatype.nexus.p2.P2Constants;

public class P2ProxyMirrorsTest
    extends AbstractNexusTestCase
{
    protected Nexus nexus;

    protected NexusConfiguration nexusConfiguration;

    private P2ProxyRepository repository;

    @Override
    protected void customizeContainerConfiguration( ContainerConfiguration configuration )
    {
        super.customizeContainerConfiguration( configuration );
        configuration.setClassPathScanning( PlexusConstants.SCANNING_ON );
    }

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        this.copyDefaultConfigToPlace();

        nexus = lookup( Nexus.class );

        nexusConfiguration = lookup( NexusConfiguration.class );

        this.repository = (P2ProxyRepository) lookup( RepositoryRegistry.class ).getRepository( "p2-repo" );
        this.repository.setChecksumPolicy( ChecksumPolicy.IGNORE );
        String remoteUrl = this.repository.getRemoteUrl();
        MockRemoteStorage mockStorage = (MockRemoteStorage) this.lookup( RemoteRepositoryStorage.class, "mock" );
        this.repository.setRemoteUrl( remoteUrl );
        this.repository.setRemoteStorage( mockStorage );

        this.lookup( SecuritySystem.class ).setSecurityEnabled( false );
    }

    private void copyFileToDotNexus( String fileName, String targetFileName )
        throws IOException
    {
        String localStorageDir = new URL( this.repository.getLocalUrl() ).getPath();

        String filePrefix = this.getClass().getName().replaceAll( "\\.", "/" ) + "-";
        String resource = filePrefix + fileName;

        String destinationPath = P2Constants.PRIVATE_ROOT + "/" + targetFileName;
        File destination = new File( localStorageDir, destinationPath );

        destination.getParentFile().mkdirs();
        FileUtils.copyFile( new File( "target/test-classes", resource ), destination );

    }

    @Override
    protected void copyDefaultConfigToPlace()
        throws IOException
    {
        this.copyResource( "/nexus.xml", new File( getConfHomeDir(), "nexus.xml" ).getAbsolutePath() );
    }

    @Test
    public void testVerifyBlackList()
        throws AccessDeniedException, IllegalOperationException, ItemNotFoundException, IOException
    {

        this.copyFileToDotNexus( "mirrors.xml", "mirrors.xml" );
        this.copyFileToDotNexus( "artifact-mappings.xml", "artifact-mappings.xml" );

        MockRemoteStorage remoteStorage = (MockRemoteStorage) this.repository.getRemoteStorage();
        remoteStorage.getDownUrls().add( "http://remote3/" );
        remoteStorage.getValidUrls().add( "http://default/test/remote3/file1.txt" );
        remoteStorage.getValidUrls().add( "http://default/test/remote3/file2.txt" );

        // not found with bad mirror
        ResourceStoreRequest request = new ResourceStoreRequest( "/remote3/file1.txt" );

        this.repository.retrieveItem( request );

        // make sure we hit the mirror
        Assert.assertTrue( remoteStorage.getRequests().contains(
                                                                 new MockRemoteStorage.MockRequestRecord(
                                                                                                          this.repository,
                                                                                                          request,
                                                                                                          "http://remote3/" ) ) );
        Assert.assertTrue( remoteStorage.getRequests().contains(
                                                                 new MockRemoteStorage.MockRequestRecord(
                                                                                                          this.repository,
                                                                                                          request,
                                                                                                          "http://remote2/" ) ) );
        Assert.assertTrue( remoteStorage.getRequests().contains(
                                                                 new MockRemoteStorage.MockRequestRecord(
                                                                                                          this.repository,
                                                                                                          request,
                                                                                                          "http://remote1/" ) ) );
        // clear the requests
        remoteStorage.getRequests().clear();

        request = new ResourceStoreRequest( "/remote3/file2.txt" );

        this.repository.retrieveItem( request );

        // make sure we did NOT hit the mirror
        Assert.assertFalse( remoteStorage.getRequests().contains(
                                                                  new MockRemoteStorage.MockRequestRecord(
                                                                                                           this.repository,
                                                                                                           request,
                                                                                                           "http://remote3/" ) ) );
        Assert.assertTrue( remoteStorage.getRequests().contains(
                                                                 new MockRemoteStorage.MockRequestRecord(
                                                                                                          this.repository,
                                                                                                          request,
                                                                                                          "http://remote2/" ) ) );
        Assert.assertTrue( remoteStorage.getRequests().contains(
                                                                 new MockRemoteStorage.MockRequestRecord(
                                                                                                          this.repository,
                                                                                                          request,
                                                                                                          "http://remote1/" ) ) );
    }

    @Test
    public void testVerifyBlackListWithCompositeRepo()
        throws AccessDeniedException, IllegalOperationException, ItemNotFoundException, IOException
    {
        // all we need is a different mirrors.xml
        this.copyFileToDotNexus( "mirrors-composite.xml", "mirrors.xml" );
        this.copyFileToDotNexus( "artifact-mappings-composite.xml", "artifact-mappings.xml" );

        this.testVerifyBlackList();
    }

    @Test
    public void testCompositeRepoWithNoMirrors()
        throws AccessDeniedException, IllegalOperationException, ItemNotFoundException, IOException
    {
        this.copyFileToDotNexus( "mirrors-none-composite.xml", "mirrors.xml" );
        this.copyFileToDotNexus( "artifact-mappings-composite.xml", "artifact-mappings.xml" );

        MockRemoteStorage remoteStorage = (MockRemoteStorage) this.repository.getRemoteStorage();
        // remoteStorage.getDownUrls().add( "http://remote3/" );
        remoteStorage.getValidUrls().add( "http://default/member2/remote2/file1.txt" );
        // remoteStorage.getValidUrls().add( "http://default/test/remote3/file2.txt" );

        // not found with bad mirror
        ResourceStoreRequest request = new ResourceStoreRequest( "/remote2/file1.txt" );

        this.repository.retrieveItem( request );
    }
}
