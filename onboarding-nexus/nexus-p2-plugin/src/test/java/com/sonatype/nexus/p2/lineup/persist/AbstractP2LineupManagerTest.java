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
package com.sonatype.nexus.p2.lineup.persist;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;
import org.sonatype.nexus.Nexus;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.AbstractNexusTestCase;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.storage.local.fs.DefaultFSLocalRepositoryStorage;

import com.sonatype.nexus.p2.lineup.repository.P2LineupConstants;
import com.sonatype.nexus.p2.lineup.repository.P2LineupRepository;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Reader;

public abstract class AbstractP2LineupManagerTest
    extends AbstractNexusTestCase
{
    protected Nexus nexus;

    protected NexusConfiguration nexusConfiguration;

    private P2LineupManager p2LineupManager;

    private P2LineupRepository p2LineupRepository;

    private File lineupStorageFolder;

    @Override
    protected void customizeContainerConfiguration( ContainerConfiguration configuration )
    {
        super.customizeContainerConfiguration( configuration );
        configuration.setClassPathScanning( PlexusConstants.SCANNING_ON );

        // to force creating the proxies/lineups with no onboarding plugin
        System.setProperty( "p2.lineups.create", "true" );
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

        // clear the repo
        DefaultFSLocalRepositoryStorage storage =
            (DefaultFSLocalRepositoryStorage) this.p2LineupRepository.getLocalStorage();
        lineupStorageFolder = storage.getBaseDir( this.p2LineupRepository, new ResourceStoreRequest( "/" ) );
        if ( !lineupStorageFolder.getAbsolutePath().contains( "lineups" ) )
        {
            Assert.fail( "I don't think the test could find the root repository folder, it was trying to delete: "
                + lineupStorageFolder.getAbsolutePath() + ", the test was stopped, your mp3's and photos will thank me.");
        }
        else
        {
            lineupStorageFolder.delete();
        }


        this.p2LineupManager = this.lookup( P2LineupManager.class );
    }

    public P2LineupRepository getP2LineupRepository()
    {
        return p2LineupRepository;
    }

    @Override
    protected void copyDefaultConfigToPlace()
        throws IOException
    {
        this.copyResource( "/nexus.xml", new File( getConfHomeDir(), "nexus.xml" ).getAbsolutePath() );
    }

    public P2LineupManager getP2LineupManager()
    {
        return p2LineupManager;
    }

    protected File getLineupFile( P2Lineup lineup )
    {
        return this.getLineupFile( lineup.getGroupId(), lineup.getId(), lineup.getVersion() );
    }

    protected File getLineupFile( String groupId, String id, String version )
    {
        String fileName = groupId.replaceAll( "\\.", "/" ) + "/" + id + "/" + version + P2LineupConstants.LINEUP_DESCRIPTOR_XML;
        return new File( lineupStorageFolder, fileName );
    }

    protected void validateLineup( P2Lineup lineup )
        throws Exception
    {
        File lineupFile = this.getLineupFile( lineup );
        Assert.assertTrue( "Lineup does not exists at: " + lineupFile.getAbsolutePath(), lineupFile.exists() );

        FileReader fileReader = null;
        try
        {
            fileReader = new FileReader( lineupFile );
            P2LineupXpp3Reader reader = new P2LineupXpp3Reader();
            P2Lineup resultLineup = reader.read( fileReader );
            Assert.assertEquals( "lineup on disk differs.", lineup, resultLineup );
        }
        finally
        {
            IOUtil.close( fileReader );
        }

    }
}
