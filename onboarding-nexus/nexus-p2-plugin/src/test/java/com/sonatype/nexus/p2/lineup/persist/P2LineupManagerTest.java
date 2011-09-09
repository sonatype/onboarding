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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Set;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.ByteArrayContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Writer;

public class P2LineupManagerTest
    extends AbstractP2LineupManagerTest
{
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        nexusConfiguration.setSecurityEnabled( false );

        nexusConfiguration.saveConfiguration();
    }

    @Test
    public void testAddLineup()
        throws Exception
    {
        P2Lineup lineup = new P2Lineup();
        lineup.setDescription( "description" );
        lineup.setGroupId( "groupId-testAddLineup" );
        lineup.setId( "id-testAddLineup" );
        lineup.setVersion( "version-testAddLineup" );

        P2Lineup result = this.getP2LineupManager().addLineup( lineup );
        Assert.assertEquals( lineup, result );
        this.validateLineup( lineup );
    }

    @Test
    public void testAddLineupAlreadyExists()
        throws Exception
    {
        P2Lineup lineup = new P2Lineup();
        lineup.setDescription( "description" );
        lineup.setGroupId( "groupId-testAddLineupAlreadyExists" );
        lineup.setId( "id-testAddLineupAlreadyExists" );
        lineup.setVersion( "version-testAddLineupAlreadyExists" );

        this.getP2LineupManager().addLineup( lineup );

        try
        {
            this.getP2LineupManager().addLineup( lineup );
            Assert.fail( "expected P2LineupStorageException" );
        }
        catch ( P2LineupStorageException expected )
        {
        }
    }

    @Test
    public void testUpdateLineup()
        throws Exception
    {
        P2Lineup lineup = new P2Lineup();
        lineup.setDescription( "description" );
        lineup.setGroupId( "groupId-testUpdateLineup" );
        lineup.setId( "id-testUpdateLineup" );
        lineup.setVersion( "version-testUpdateLineup" );

        this.getP2LineupManager().addLineup( lineup );
        lineup.setName( "testUpdateLineup" );
        P2Lineup result = this.getP2LineupManager().updateLineup( lineup );
        Assert.assertEquals( lineup, result );
        this.validateLineup( lineup );
    }

    @Test
    public void testUpdateLineupDoesNotExist()
        throws Exception
    {
        P2Lineup lineup = new P2Lineup();
        lineup.setDescription( "description" );
        lineup.setGroupId( "groupId-testUpdateLineupDoesNotExist" );
        lineup.setId( "id-testUpdateLineupDoesNotExist" );
        lineup.setVersion( "version-testUpdateLineupDoesNotExist" );
        try
        {
            this.getP2LineupManager().updateLineup( lineup );
            Assert.fail( "Expected NoSuchP2LineupException" );
        }
        catch ( NoSuchP2LineupException e )
        {
            // expected
        }
    }

    @Test
    public void testDeleteLineup()
        throws Exception
    {

        P2Lineup lineup = new P2Lineup();
        lineup.setDescription( "description" );
        lineup.setGroupId( "groupId-testDeleteLineup" );
        lineup.setId( "id-testDeleteLineup" );
        lineup.setVersion( "version-testDeleteLineup" );

        this.getP2LineupManager().addLineup( lineup );
        this.getP2LineupManager().deleteLineup( new P2Gav( lineup.getGroupId(), lineup.getId(), lineup.getVersion() ) );
        Assert.assertFalse( "Lineup should have been removed: " + this.getLineupFile( lineup ), this.getLineupFile(
            lineup ).exists() );
    }

    @Test
    public void testDeleteLineupDoesNotExist()
        throws Exception
    {
        try
        {
            this.getP2LineupManager().deleteLineup( new P2Gav( "INVALID", "INVALID", "INVALID" ) );
            Assert.fail( "Expected NoSuchP2LineupException" );
        }
        catch ( NoSuchP2LineupException e )
        {
            // expected
        }
    }

    @Test
    public void testGetLineup()
        throws Exception
    {
        P2Lineup lineup = new P2Lineup();
        lineup.setDescription( "description" );
        lineup.setGroupId( "groupId-testGetLineup" );
        lineup.setId( "id-testGetLineup" );
        lineup.setVersion( "version-testGetLineup" );

        this.getP2LineupManager().addLineup( lineup );

        P2Lineup result =
            this.getP2LineupManager().getLineup( new P2Gav( lineup.getGroupId(), lineup.getId(), lineup.getVersion() ) );

        Assert.assertEquals( lineup, result );

    }

    @Test
    public void testGetLineupDoesNotExist()
        throws Exception
    {
        try
        {
            this.getP2LineupManager().getLineup( new P2Gav( "INVALID", "INVALID", "INVALID" ) );
            Assert.fail( "Expected NoSuchP2LineupException" );
        }
        catch ( NoSuchP2LineupException e )
        {
            // expected
        }
    }

    @Test
    public void testGetInvalidXml()
        throws Exception
    {
        FileWriter fileWriter = null;
        try
        {
            P2Gav gav = new P2Gav( "testGetInvalidXml", "testGetInvalidXml", "testGetInvalidXml" );

            File lineupFile = this.getLineupFile( "testGetInvalidXml", "testGetInvalidXml", "testGetInvalidXml" );
            lineupFile.getParentFile().mkdirs();

            fileWriter =  new FileWriter( lineupFile );

            fileWriter.write( "<invalid XML file" );
            fileWriter.close();

            this.getP2LineupManager().getLineup( gav );
            Assert.fail( "Expected NoSuchP2LineupException" );
        }
        catch ( NoSuchP2LineupException e )
        {
            // expected
        }
        finally
        {
            IOUtil.close( fileWriter );
        }
    }

    @Test
    public void testListLineups()
        throws Exception
    {
        int numberOfDefaults = 3;

        // write an invalid one
        this.testGetInvalidXml();

        // and add a couple more
        P2Lineup lineup1 = new P2Lineup();
        lineup1.setDescription( "description" );
        lineup1.setGroupId( "groupId-testListLineups1" );
        lineup1.setId( "id-testListLineups1" );
        lineup1.setVersion( "version-testListLineups1" );
        this.getP2LineupManager().addLineup( lineup1 );

        P2Lineup lineup2 = new P2Lineup();
        lineup2.setDescription( "description" );
        lineup2.setGroupId( "groupId-testListLineups2" );
        lineup2.setId( "id-testListLineups2" );
        lineup2.setVersion( "version-testListLineups2" );
        this.getP2LineupManager().addLineup( lineup2 );

        Set<P2Lineup> lineups = this.getP2LineupManager().getLineups();
        Assert.assertEquals( numberOfDefaults + 2, lineups.size() );

        // find the first one
        P2Lineup lineupResult1 = null;
        P2Lineup lineupResult2 = null;

        for ( P2Lineup p2Lineup : lineups )
        {
            if ( p2Lineup.getId().equals( "id-testListLineups1" ) )
            {
                lineupResult1 = p2Lineup;
            }
            else if ( p2Lineup.getId().equals( "id-testListLineups2" ) )
            {
                lineupResult2 = p2Lineup;
            }
        }

        Assert.assertEquals( lineup1, lineupResult1 );
        Assert.assertEquals( lineup2, lineupResult2 );
    }

    @Test
    public void testWriteDirectlyToRepository() throws IOException, UnsupportedStorageOperationException, IllegalOperationException
    {

        P2Lineup lineup = new P2Lineup();
        lineup.setDescription( "description" );
        lineup.setGroupId( "groupId-testWriteDirectlyToRepository" );
        lineup.setId( "id-testWriteDirectlyToRepository" );
        lineup.setVersion( "version-testWriteDirectlyToRepository" );

        P2Gav p2Gav = new P2Gav( lineup );

        Repository p2LineupRepository = this.getP2LineupRepository();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter osWriter = new OutputStreamWriter( baos );

        // write to buffer
        P2LineupXpp3Writer writer = new P2LineupXpp3Writer();
        writer.write( osWriter, lineup );

        ByteArrayContentLocator contentLocator = new ByteArrayContentLocator( baos.toByteArray(), "application/xml" );
        DefaultStorageFileItem fileItem = new DefaultStorageFileItem( p2LineupRepository, new ResourceStoreRequest( p2Gav.toPathString() + "/p2lineup.xml" ), true, false, contentLocator );

        // write and expect failure
        try
        {
            p2LineupRepository.storeItem( false, fileItem );
            Assert.fail( "Expected IllegalRequestException" );
        }
        catch( IllegalOperationException e )
        {
            // expected
        }

        // change the path and it should work
        fileItem = new DefaultStorageFileItem( p2LineupRepository, new ResourceStoreRequest( p2Gav.toPathString() + "/something.xml" ), true, false, contentLocator );
        p2LineupRepository.storeItem( false, fileItem );

    }

}
