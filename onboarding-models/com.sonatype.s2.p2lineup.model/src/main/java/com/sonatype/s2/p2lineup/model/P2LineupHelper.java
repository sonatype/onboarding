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
package com.sonatype.s2.p2lineup.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.nexus.p2.rest.model.P2LineupError;
import com.sonatype.nexus.p2.rest.model.P2LineupRepositoryError;

public class P2LineupHelper
{
    private static final Logger log = LoggerFactory.getLogger( P2LineupHelper.class );

    public static String getMasterInstallableUnitId( IP2Lineup lineup )
    {
        if ( lineup.getGroupId() == null || lineup.getGroupId().trim().length() == 0 )
        {
            throw new IllegalArgumentException( "Lineup group id cannot be null or empty" );
        }
        if ( lineup.getId() == null || lineup.getId().trim().length() == 0 )
        {
            throw new IllegalArgumentException( "Lineup artifact id cannot be null or empty" );
        }

        return lineup.getGroupId() + "." + lineup.getId() + ".p2Lineup";
    }

    public static void replaceNexusServerURLInLineupRepositories( IP2Lineup lineup, String nexusServerURL,
                                                                  String replaceWith )
    {
        log.debug( "Replacing nexus server URL in lineup repositories: {}-->{}", nexusServerURL, replaceWith );
        for ( IP2LineupSourceRepository lineupRepository : lineup.getRepositories() )
        {
            String lineupRepositoryURL = lineupRepository.getUrl();
            if ( lineupRepositoryURL.startsWith( nexusServerURL ) )
            {
                String newLineupRepositoryURL = replaceWith + lineupRepositoryURL.substring( nexusServerURL.length() );
                lineupRepository.setUrl( newLineupRepositoryURL );
                log.debug( "Replaced {}-->{}", lineupRepositoryURL, newLineupRepositoryURL );
            }
        }
    }

    public static void replaceNexusServerURLInLineupError( P2LineupError error, String nexusServerURL,
                                                           String replaceWith )
    {
        if ( !( error instanceof P2LineupRepositoryError ) )
        {
            return;
        }
        log.debug( "Replacing nexus server URL in lineup error: {}-->{}", nexusServerURL, replaceWith );
        P2LineupRepositoryError repositoryError = (P2LineupRepositoryError) error;
        String repositoryUrl = repositoryError.getRepositoryURL();
        if ( repositoryUrl.startsWith( nexusServerURL ) )
        {
            String newRepositoryURL = replaceWith + repositoryUrl.substring( nexusServerURL.length() );
            repositoryError.setRepositoryURL( newRepositoryURL );
            log.debug( "Replaced {}-->{}", repositoryUrl, newRepositoryURL );
        }
    }
}
