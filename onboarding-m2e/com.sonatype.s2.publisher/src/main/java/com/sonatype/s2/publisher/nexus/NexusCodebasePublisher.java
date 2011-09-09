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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.maven.ide.eclipse.io.ByteArrayRequestEntity;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.p2lineup.model.IP2LineupSourceRepository;
import com.sonatype.s2.project.common.S2ProjectCommon;
import com.sonatype.s2.project.model.IEclipsePreferencesLocation;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.IUrlLocation;
import com.sonatype.s2.project.model.S2ProjectFacade;
import com.sonatype.s2.project.model.descriptor.EclipsePreferencesLocation;
import com.sonatype.s2.publisher.IS2Publisher;
import com.sonatype.s2.publisher.S2PublishRequest;
import com.sonatype.s2.publisher.S2PublisherConstants;
import com.sonatype.s2.publisher.S2PublisherException;
import com.sonatype.s2.publisher.internal.Messages;

@SuppressWarnings( "restriction" )
public class NexusCodebasePublisher
    implements S2PublisherConstants, IS2Publisher
{
    private static final Logger log = LoggerFactory.getLogger( NexusCodebasePublisher.class );

    public void publish( S2PublishRequest s2PublishRequest, IProgressMonitor monitor )
        throws IOException, URISyntaxException, CoreException
    {
        if ( s2PublishRequest == null )
        {
            String message = "s2PublishRequest cannot be null";
            log.error( message );
            throw new S2PublisherException( message );
        }

        s2PublishRequest.validate();

        URI s2CatalogRepositoryURL = new URI( s2PublishRequest.getNexusBaseUrl() + IS2Project.PROJECT_REPOSITORY_PATH );
        log.debug( "Publishing {} projects to {}", s2PublishRequest.getS2Projects().size(),
                   s2CatalogRepositoryURL.toString() );

        String s2CatalogRepositoryURLString = s2CatalogRepositoryURL.normalize() + "/";

        for ( IPath s2ProjectLocation : s2PublishRequest.getS2Projects() )
        {
            log.debug( "Publishing project {}", s2ProjectLocation.makeAbsolute().toString() );

            IPath s2ProjectPMDFile = s2ProjectLocation.append( PMD_FILENAME );
            if ( !s2ProjectPMDFile.toFile().exists() )
            {
                String message = s2ProjectPMDFile.toString() + " does not exist.";
                log.error( message );
                throw new S2PublisherException( message );
            }

            IS2Project s2Project = loadProject( s2ProjectPMDFile, monitor );

            String projectUploadBaseURI =
                getProjectUploadBaseURI( s2ProjectLocation, s2CatalogRepositoryURLString, s2Project );
            log.debug( "Publishing project upload base uri {}", projectUploadBaseURI );

            // Ensure that no codebase exists at the URI
            if ( !s2Project.getVersion().endsWith( '-' + IS2Project.HEAD_VERSION_SUFFIX )
                && S2IOFacade.exists( projectUploadBaseURI, monitor ) )
            {
                throw new S2PublisherException( Messages.NexusCodebasePublisher_error_codebaseExists );
            }

            String uploadURIString = projectUploadBaseURI + IS2Project.PROJECT_DESCRIPTOR_FILENAME;

            // Publish the eclipse preferences file
            IPath s2ProjectPreferencesFile = s2ProjectLocation.append( IS2Project.PROJECT_PREFERENCES_FILENAME );
            if ( s2ProjectPreferencesFile.toFile().exists() )
            {
                String preferencesUrl = projectUploadBaseURI + IS2Project.PROJECT_PREFERENCES_FILENAME;
                IEclipsePreferencesLocation location = s2Project.getEclipsePreferencesLocation();
                if ( location == null )
                {
                    location = new EclipsePreferencesLocation();
                    s2Project.setEclipsePreferencesLocation( location );
                }
                location.setUrl( preferencesUrl );

                log.debug( "Publishing eclipsePreferences {}", s2ProjectPreferencesFile.toString() );
                S2IOFacade.putFile( s2ProjectPreferencesFile.toFile(), preferencesUrl, monitor );
            }

            replaceNexusBaseURL( s2Project.getP2LineupLocation(), s2PublishRequest.getNexusBaseUrl() );
            replaceNexusBaseURL( s2Project.getMavenSettingsLocation(), s2PublishRequest.getNexusBaseUrl() );
            replaceNexusBaseURL( s2Project.getEclipsePreferencesLocation(), s2PublishRequest.getNexusBaseUrl() );

            // Publish the project descriptor file
            log.debug( "Publishing project descriptor {}", s2ProjectPMDFile.toString() );
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            S2ProjectFacade.writeProject( s2Project, buf );

            S2IOFacade.put( new ByteArrayRequestEntity( buf.toByteArray(), null ), uploadURIString, monitor );

            // Publish the project icon file
            IPath s2ProjectIconFile = s2ProjectLocation.append( IS2Project.PROJECT_ICON_FILENAME );
            if ( s2ProjectIconFile.toFile().exists() )
            {
                log.debug( "Publishing project icon {}", s2ProjectIconFile.toString() );
                uploadURIString = projectUploadBaseURI + IS2Project.PROJECT_ICON_FILENAME; // TODO constant
                S2IOFacade.putFile( s2ProjectIconFile.toFile(), uploadURIString, monitor );
            }
        }
    }

    private void replaceNexusBaseURL( IUrlLocation location, String baseurl )
        throws URISyntaxException
    {
        if ( location == null || location.getUrl() == null )
        {
            return;
        }

        if ( baseurl.endsWith( "/" ) )
        {
            baseurl = baseurl.substring( 0, baseurl.length() - 1 );
        }
        String basestr = new URI( baseurl ).normalize().toString();
        if ( location.getUrl().startsWith( basestr ) )
        {
            location.setUrl( IP2LineupSourceRepository.NEXUS_BASE_URL + location.getUrl().substring( basestr.length() ) );
        }
    }

    private String getProjectUploadBaseURI( IPath projectLocation, String baseUri, IS2Project s2Project )
        throws S2PublisherException
    {
        String groupId = s2Project.getGroupId();
        String artifactId = s2Project.getArtifactId();
        String version = s2Project.getVersion();

        if ( groupId == null || groupId.trim().length() == 0 )
        {
            String message = "groupId is not specified for project " + projectLocation;
            log.error( message );
            throw new S2PublisherException( message );
        }
        groupId = groupId.trim().replace( '.', '/' );

        if ( artifactId == null || artifactId.trim().length() == 0 )
        {
            String message = "artifactId is not specified for project " + projectLocation;
            log.error( message );
            throw new S2PublisherException( message );
        }
        artifactId = artifactId.trim();

        if ( version == null || version.trim().length() == 0 )
        {
            String message = "version is not specified for project " + projectLocation;
            log.error( message );
            throw new S2PublisherException( message );
        }
        version = version.trim();

        StringBuilder sb = new StringBuilder( baseUri );
        if ( !baseUri.endsWith( "/" ) )
        {
            sb.append( '/' );
        }
        sb.append( groupId ).append( '/' );
        sb.append( artifactId ).append( '/' );
        sb.append( version ).append( '/' );

        return sb.toString();
    }

    private IS2Project loadProject( IPath pmdPath, IProgressMonitor monitor )
        throws CoreException, IOException, URISyntaxException
    {
        File pmdFile = pmdPath.toFile();
        log.debug( "Loading s2 project file: {}", pmdFile.getAbsolutePath() );
        SubMonitor progress =
            SubMonitor.convert( monitor, "Loading project descriptor " + pmdFile.toURI().toString(), 1 );
        IS2Project project;

        InputStream is = S2IOFacade.openStream( pmdFile.toURI().toString(), progress.newChild( 1 ) );
        try
        {
            project = S2ProjectCommon.loadProject( is, true /* validate */);
        }
        finally
        {
            IOUtil.close( is );
        }

        log.debug( "Loaded s2 project: {}", project.getName() );
        return project;
    }
    
    public String getName() {
    	return Messages.codebase;
    }

}
