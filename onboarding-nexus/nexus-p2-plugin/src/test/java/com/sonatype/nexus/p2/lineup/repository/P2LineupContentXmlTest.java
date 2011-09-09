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
package com.sonatype.nexus.p2.lineup.repository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.Nexus;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.AbstractNexusTestCase;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.security.SecuritySystem;

import com.sonatype.nexus.p2.P2Constants;

public class P2LineupContentXmlTest
    extends AbstractNexusTestCase
{
    protected Nexus nexus;

    protected NexusConfiguration nexusConfiguration;

    private P2LineupRepository p2LineupRepository;

    private static final String CONTENT = "some string with ${nexus.baseURL} and some other text";

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

        this.p2LineupRepository = (P2LineupRepository) lookup( RepositoryRegistry.class ).getRepository( "lineups" );

        this.lookup( SecuritySystem.class ).setSecurityEnabled( false );
    }

    @Override
    protected void copyDefaultConfigToPlace()
        throws IOException
    {
        this.copyResource( "/nexus.xml", new File( getConfHomeDir(), "nexus.xml" ).getAbsolutePath() );
    }

    @Test
    public void testNoInterpolation()
        throws Exception
    {

        ResourceStoreRequest request = new ResourceStoreRequest( "/content.xml" );
        request.getRequestContext().put( P2Constants.PROP_SKIP_INTERPOLATION, "bar" );

        String resultContentXml = this.getContentXml( request );
        Assert.assertEquals( CONTENT, resultContentXml );
    }

    @Test
    public void testInterpolationFailure()
        throws Exception
    {

        ResourceStoreRequest request = new ResourceStoreRequest( "/content.xml" );

        try
        {
            String contentXml = this.getContentXml( request );
            Assert.fail( "expected StorageException. content.xml:\n"+ contentXml );
        }
        catch ( StorageException e )
        {
            // expected
        }

    }

    @Test
    public void testInterpolation()
        throws Exception
    {

        ResourceStoreRequest request = new ResourceStoreRequest( "/content.xml" );
        request.setRequestAppRootUrl( "http://localhost:8081/foobar/" );

        // NOTE: the trailing '/' is stripped
        String expectedResult = "some string with http://localhost:8081/foobar and some other text";

        String resultContentXml = this.getContentXml( request );
        Assert.assertEquals( expectedResult, resultContentXml );
    }

    private String getContentXml( ResourceStoreRequest request )
        throws IOException, AccessDeniedException, IllegalOperationException, ItemNotFoundException
    {
        File contextXml = new File( new URL( this.p2LineupRepository.getLocalUrl() ).getFile(), "content.xml" );
        FileUtils.fileWrite( contextXml.getAbsolutePath(), CONTENT );

        StorageFileItem storageItem = (StorageFileItem) this.p2LineupRepository.retrieveItem( request );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        InputStream in = null;
        try
        {
            in = storageItem.getInputStream();
            IOUtil.copy( in, baos );
        }
        finally
        {
            IOUtil.close( in );
        }

        return baos.toString();
    }

}
