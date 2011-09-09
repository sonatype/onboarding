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
package com.sonatype.s2.publisher;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.io.UnauthorizedException;

import com.sonatype.nexus.p2.rest.model.P2LineupSummaryDto;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupInstallableUnit;
import com.sonatype.s2.p2lineup.model.P2LineupSourceRepository;
import com.sonatype.s2.publisher.nexus.NexusLineupPublisher;

public class NexusLineupPublisherIT
    extends TestCase
{
    private IProgressMonitor monitor = new NullProgressMonitor();

    private static final String NEXUS_URL = "http://localhost:8081/nexus";

    private static final String USERNAME = "admin";

    private static final String PASSWORD = "admin123";

    public void testValidateAccess()
    {
        AuthFacade.getAuthService().save( NEXUS_URL, USERNAME, PASSWORD );

        String groupId = "NexusLineupPublisherIT";
        String artifactId = "testValidateAccess";
        String version = "1.0.0";
        P2Lineup lineup = createLineup( groupId, artifactId, version );

        NexusLineupPublisher publisher = new NexusLineupPublisher();
        IStatus status = publisher.preValidateUpload( NEXUS_URL, lineup, monitor );
        assertNotNull( status );
        assertTrue( status.getMessage(), status.isOK() );

        // Bad username
        AuthFacade.getAuthService().save( NEXUS_URL, "bad username", PASSWORD );
        status = publisher.preValidateUpload( NEXUS_URL, lineup, monitor );
        assertNotNull( status );
        assertNotNull( status.getException() );
        assertEquals( IStatus.ERROR, status.getSeverity() );
        assertTrue( status.getException().getClass().getCanonicalName(),
                    status.getException() instanceof UnauthorizedException );
        assertTrue( status.getMessage(), status.getMessage().startsWith( "HTTP status code 401: Unauthorized:" ) );

        // Bad password
        AuthFacade.getAuthService().save( NEXUS_URL, USERNAME, "bad password" );
        status = publisher.preValidateUpload( NEXUS_URL, lineup, monitor );
        assertNotNull( status );
        assertNotNull( status.getException() );
        assertEquals( IStatus.ERROR, status.getSeverity() );
        assertTrue( status.getException().getClass().getCanonicalName(),
                    status.getException() instanceof UnauthorizedException );
        assertTrue( status.getMessage(), status.getMessage().startsWith( "HTTP status code 401: Unauthorized:" ) );

        // Anonymous
        AuthFacade.getAuthService().save( NEXUS_URL, "", "" );
        status = publisher.preValidateUpload( NEXUS_URL, lineup, monitor );
        assertNotNull( status );
        assertNotNull( status.getException() );
        assertEquals( IStatus.ERROR, status.getSeverity() );
        assertTrue( status.getException().getClass().getCanonicalName(),
                    status.getException() instanceof UnauthorizedException );
        assertTrue( status.getMessage(), status.getMessage().startsWith( "HTTP status code 401: Unauthorized:" ) );
    }

    public void testValidateLineup()
        throws CoreException
    {
        AuthFacade.getAuthService().save( NEXUS_URL, USERNAME, PASSWORD );

        String groupId = "NexusLineupPublisherIT";
        String artifactId = "testValidateLineup";
        String version = "1.0.0";
        P2Lineup lineup = createLineup( groupId, artifactId, version );

        NexusLineupPublisher publisher = new NexusLineupPublisher();
        IStatus status = publisher.validateLineup( NEXUS_URL, lineup, monitor );
        assertNotNull( status );
        assertTrue( status.getMessage(), status.isOK() );
    }

    public void testPublishLineup()
        throws CoreException
    {
        AuthFacade.getAuthService().save( NEXUS_URL, USERNAME, PASSWORD );

        String groupId = "NexusLineupPublisherIT";
        String artifactId = "testPublishLineup";
        String version = "" + System.currentTimeMillis();
        P2Lineup lineup = createLineup( groupId, artifactId, version );

        NexusLineupPublisher publisher = new NexusLineupPublisher();
        P2LineupSummaryDto lineupDto = publisher.publishLineup( NEXUS_URL, lineup, monitor );
        assertNotNull( lineupDto );
        assertEquals( groupId, lineupDto.getGroupId() );
        assertEquals( artifactId, lineupDto.getId() );
        assertEquals( version, lineupDto.getVersion() );
    }

//    public void testProjectPublish()
//        throws Exception
//    {
//        AuthFacade.getAuthService().save( NEXUS_URL, USERNAME, PASSWORD );
//
//        String groupId = "NexusLineupPublisherIT";
//        String artifactId = "testProjectPublish";
//        String version = "" + System.currentTimeMillis();
//        P2Lineup lineup = createLineup( groupId, artifactId, version );
//
//        IProject project = new NewLineupProjectOperation( "foo", lineup ).createProject( monitor );
//
//        NexusLineupPublisher publisher = new NexusLineupPublisher();
//        S2PublishRequest request = new S2PublishRequest();
//
//        request.setNexusBaseUrl( NEXUS_URL );
//        request.addS2Project( project.getFolder( S2PublisherConstants.PMD_PATH ).getLocation() );
//
//        publisher.publish( request, monitor );
//    }

    public void testPreValidateUpload_LineupGAVExists()
        throws Exception
    {
        AuthFacade.getAuthService().save( NEXUS_URL, USERNAME, PASSWORD );

        String groupId = "NexusLineupPublisherIT";
        String artifactId = "testPreValidateUpload_LineupGAVExists";
        String version = "" + System.currentTimeMillis();
        P2Lineup lineup = createLineup( groupId, artifactId, version );

        // Publish the lineup
        NexusLineupPublisher publisher = new NexusLineupPublisher();
        P2LineupSummaryDto lineupDto = publisher.publishLineup( NEXUS_URL, lineup, monitor );
        assertNotNull( lineupDto );
        assertEquals( groupId, lineupDto.getGroupId() );
        assertEquals( artifactId, lineupDto.getId() );
        assertEquals( version, lineupDto.getVersion() );

        // Should fail because the lineup GAV exists
        IStatus status = publisher.preValidateUpload( NEXUS_URL, lineup, monitor );
        assertNotNull( status );
        assertEquals( IStatus.ERROR, status.getSeverity() );
        assertTrue( status.getMessage(),
                    status.getMessage().startsWith( "Lineup coordinates NexusLineupPublisherIT:testPreValidateUpload_LineupGAVExists:"
                                                        + version + " cannot be modified." ) );
    }

    private P2Lineup createLineup( String groupId, String artifactId, String version )
    {

        P2Lineup lineup = new P2Lineup();

        lineup.setGroupId( groupId );
        lineup.setId( artifactId );
        lineup.setVersion( version );

        lineup.addRepository( new P2LineupSourceRepository( "http://download.eclipse.org/releases/helios" ) );
        lineup.addRootInstallableUnit( new P2LineupInstallableUnit( "org.eclipse.osgi", "0.0.0" ) );

        return lineup;
    }
}
