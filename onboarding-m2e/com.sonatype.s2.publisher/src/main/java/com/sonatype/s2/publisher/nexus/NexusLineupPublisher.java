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
package com.sonatype.s2.publisher.nexus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.maven.ide.eclipse.io.ByteArrayRequestEntity;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.maven.ide.eclipse.io.ServerResponse;
import org.maven.ide.eclipse.io.TransferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.nexus.p2.rest.model.P2LineupError;
import com.sonatype.nexus.p2.rest.model.P2LineupErrorResponse;
import com.sonatype.nexus.p2.rest.model.P2LineupRepositoryError;
import com.sonatype.nexus.p2.rest.model.P2LineupSummaryDto;
import com.sonatype.nexus.p2.rest.model.P2LineupUnresolvedInstallableUnit;
import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.IP2LineupSourceRepository;
import com.sonatype.s2.p2lineup.model.P2LineupHelper;
import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Reader;
import com.sonatype.s2.p2lineup.model.io.xstream.P2LineupXstreamIO;
import com.sonatype.s2.publisher.Activator;
import com.sonatype.s2.publisher.IS2Publisher;
import com.sonatype.s2.publisher.S2PublishRequest;
import com.sonatype.s2.publisher.S2PublisherException;
import com.sonatype.s2.publisher.internal.Messages;

/**
 * https://docs.sonatype.com/display/M2E/MSE+REST+Messages
 * 
 * @author igor
 */
public class NexusLineupPublisher
    implements IS2Publisher
{
    private final Logger log = LoggerFactory.getLogger( NexusLineupPublisher.class );

    // see com.sonatype.nexus.p2.rest.P2LineupListPlexusResource.RESOURCE_URI
    public static final String P2_RESOURCE_URI = "/service/local/p2/lineups/";

    public static final String STATUS_RESOURCE_URI = "/service/local/status";

    public static final String PREVALIDATE_QUERY = "?pre-validate=true";

    public static final String DRYRUN_QUERY = "?dry-run=true";

    private static P2LineupXstreamIO serializer = new P2LineupXstreamIO();

    public void publish( S2PublishRequest s2PublishRequest, IProgressMonitor monitor )
        throws IOException, URISyntaxException, CoreException
    {
        for ( IPath s2ProjectLocation : s2PublishRequest.getS2Projects() )
        {
            log.debug( "Publishing project {}", s2ProjectLocation.makeAbsolute().toString() );
            IPath lineupFile = s2ProjectLocation.append( IP2Lineup.LINEUP_FILENAME );
            if ( !lineupFile.toFile().exists() )
            {
                String message = lineupFile.toString() + " does not exist.";
                log.error( message );
                throw new S2PublisherException( message );
            }

            IP2Lineup lineup = loadLineup( lineupFile, monitor );

            publishLineup( s2PublishRequest.getNexusBaseUrl(), lineup, monitor );
        }
    }

    private IP2Lineup loadLineup( IPath lineupFile, IProgressMonitor monitor )
        throws CoreException
    {
        File file = lineupFile.toFile();

        try
        {
            InputStream is = new FileInputStream( file );
            try
            {
                return new P2LineupXpp3Reader().read( is, false /* strict */);
            }
            finally
            {
                IOUtil.close( is );
            }
        }
        catch ( XmlPullParserException e )
        {
            // TODO pull root-cause IOException!
            throw new S2PublisherException( "Could not read lineup description file", e );
        }
        catch ( IOException e )
        {
            throw new S2PublisherException( "Could not read lineup description file", e );
        }
    }

    public P2LineupSummaryDto publishLineup( String serverUrl, IP2Lineup lineup, IProgressMonitor monitor )
        throws CoreException
    {
        return uploadLineup( serverUrl, lineup, false, monitor );
    }

    public IStatus validateLineup( String serverUrl, IP2Lineup lineup, IProgressMonitor monitor )
    {
        try
        {
            uploadLineup( serverUrl, lineup, true, monitor );

            return Status.OK_STATUS;
        }
        catch ( CoreException e )
        {
            return e.getStatus();
        }
    }


    protected P2LineupSummaryDto uploadLineup( String serverUrl, IP2Lineup lineup, boolean dryRun,
                                               IProgressMonitor monitor )
        throws CoreException
    {
        return uploadLineup( serverUrl, lineup, dryRun ? DRYRUN_QUERY : null, monitor );
    }

    protected P2LineupSummaryDto uploadLineup( String serverUrl, IP2Lineup lineup, String query, IProgressMonitor monitor )
        throws CoreException
    {
        try
        {
            serverUrl = cleanupUrl( serverUrl );
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            P2LineupHelper.replaceNexusServerURLInLineupRepositories( lineup, serverUrl,
                                                                      IP2LineupSourceRepository.NEXUS_BASE_URL );
            try
            {
                try
                {
                    serializer.writeLineup( lineup, os );
                }
                finally
                {
                    IOUtil.close( os );
                }
            }
            finally
            {
                // Restore the URLs of the lineup repositories
                P2LineupHelper.replaceNexusServerURLInLineupRepositories( lineup,
                                                                          IP2LineupSourceRepository.NEXUS_BASE_URL,
                                                                          serverUrl );
            }

            if ( log.isDebugEnabled() )
            {
                log.debug( new String( os.toByteArray(), "UTF8" ) );
            }

            ByteArrayRequestEntity entity = new ByteArrayRequestEntity( os.toByteArray(), "application/xml" );

            int timeoutInMilliseconds = 5 * 60 * 1000; // 5 minutes
            ServerResponse response =
                S2IOFacade.put( entity, getLineupUrl( serverUrl, lineup, query ), timeoutInMilliseconds, monitor,
                                ( query != null ? "Validating lineup" : "Uploading lineup" ) );

            if ( query == null )
            {
                return serializer.readLineupSummary( new ByteArrayInputStream( response.getResponseData() ) );
            }

            return null;
        }
        catch ( TransferException e )
        {
            if ( e.hasNexusError() )
            {
                throw new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                                                     NLS.bind( Messages.validation_error, e.getNexusError() ) ) );
            }

            P2LineupErrorResponse response =
                serializer.readErrorResponse( new ByteArrayInputStream( e.getServerResponse().getResponseData() ) );

            MultiStatus status =
                new MultiStatus( Activator.PLUGIN_ID, -1, Messages.nexusLineupPublisher_p2_validation_errors, null );
            for ( P2LineupError error : response.getErrors() )
            {
                if ( error instanceof P2LineupUnresolvedInstallableUnit || error instanceof P2LineupRepositoryError )
                {
                    P2LineupHelper.replaceNexusServerURLInLineupError( error, IP2LineupSourceRepository.NEXUS_BASE_URL,
                                                                       serverUrl );
                    status.add( new P2LineupValidationStatus( error ) );
                }
                else
                {
                    throw new CoreException( new P2LineupValidationStatus( error ) );
                }
            }

            throw new CoreException( status );
        }
        catch ( IOException e )
        {
            throw newValidationException( e );
        }
        catch ( URISyntaxException e )
        {
            throw newValidationException( e );
        }
    }

    private CoreException newValidationException( Exception e )
    {
        return new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e ) );
    }

    private static String getLineupUrl( String serverUrl, IP2Lineup lineup, String query )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( cleanupUrl( serverUrl ) );
        sb.append( P2_RESOURCE_URI );
        sb.append( lineup.getGroupId().replace( '.', '/' ) ).append( '/' );
        sb.append( lineup.getId() ).append( '/' );
        sb.append( lineup.getVersion() );

        if ( query != null )
        {
            assert query.startsWith( "?" );
            sb.append( query );
        }

        return sb.toString();
    }

    public IStatus preValidateUpload( String serverUrl, final IP2Lineup lineup, final IProgressMonitor monitor )
    {
        try
        {
            uploadLineup( serverUrl, lineup, PREVALIDATE_QUERY, monitor );
            return Status.OK_STATUS;
        }
        catch ( CoreException e )
        {
            return e.getStatus();
        }
    }

    /**
     * server url checks.. cannot end with slash
     */
    private static String cleanupUrl( String serverUrl )
    {
        return serverUrl.endsWith( "/" ) ? serverUrl.substring( 0, serverUrl.length() - 1 ) : serverUrl;
    }
    
    public String getName() {
    	return Messages.lineup;
    }
}
