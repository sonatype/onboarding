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
package com.sonatype.nexus.p2.rest;

import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.data.Request;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;

import com.sonatype.nexus.p2.lineup.persist.P2ConfigurationException;
import com.sonatype.nexus.p2.lineup.persist.P2Gav;
import com.sonatype.nexus.p2.lineup.persist.P2LineupManager;
import com.sonatype.nexus.p2.lineup.resolver.CannotResolveP2LineupException;
import com.sonatype.nexus.p2.rest.model.P2LineupError;
import com.sonatype.nexus.p2.rest.model.P2LineupErrorResponse;
import com.sonatype.nexus.p2.rest.model.P2LineupRepositoryError;
import com.sonatype.nexus.p2.rest.model.P2LineupSummaryDto;
import com.sonatype.nexus.p2.rest.model.P2LineupUnresolvedInstallableUnit;
import com.sonatype.s2.p2lineup.model.P2Lineup;

public abstract class AbstractP2LineupPlexusResource
extends AbstractNexusPlexusResource
{
    @Requirement
    private P2LineupManager lineupManager;

    protected String getRepositoryUrl( Request request, P2Gav gav )
        throws P2ConfigurationException
    {
        StringBuffer urlBuf = new StringBuffer();
        urlBuf.append( "content/repositories/").append( this.lineupManager.getDefaultP2LineupRepository().getId() ).append( "/" ).append( gav.toPathString()).append( "/" );
        return this.createRootReference( request, urlBuf.toString() ).toString();
    }

    public P2LineupManager getLineupManager()
    {
        return lineupManager;
    }

    protected P2LineupSummaryDto newLineupSummary( Request request, P2Lineup lineup )
        throws P2ConfigurationException
    {
        P2Gav gav = new P2Gav( lineup.getGroupId(), lineup.getId(), lineup.getVersion() );
        P2LineupSummaryDto summary = new P2LineupSummaryDto();
        summary.setId( lineup.getId() );
        summary.setGroupId( lineup.getGroupId() );
        summary.setVersion( lineup.getVersion() );
        summary.setDescription( lineup.getDescription() );
        summary.setName( lineup.getName() );
    
        // set the URI
        String uri = this.createChildReference( request, this, "" ).toString() + gav.toPathString();
        summary.setResourceUri( uri );
    
        StringBuffer urlBuf = new StringBuffer();
        urlBuf.append( "content/repositories/" ).append( this.getLineupManager().getDefaultP2LineupRepository().getId() ).append("/" ).append( gav.toPathString() ).append( "/" );
        String repositoryUrl = this.createRootReference( request, urlBuf.toString() ).toString();
        summary.setRepositoryUrl( repositoryUrl );
        return summary;
    }
    
    protected P2LineupErrorResponse createErrorResponse( String message )
    {
        P2LineupErrorResponse response = new P2LineupErrorResponse();
        response.addError( new P2LineupError( message ) );
        return response;
    }
    
    protected P2LineupErrorResponse createErrorResponse( CannotResolveP2LineupException e )
    {
        P2LineupErrorResponse response = new P2LineupErrorResponse();
        if ( e.getCause() != null )
        {
            response.addError( new P2LineupError( e.getCause().getMessage() ) );
        }
        if ( e.getError() != null )
        {
            response.addError( e.getError() );
        }
        for ( P2LineupRepositoryError repositoryError : e.getRepositoryErrors() )
        {
            response.addError( repositoryError );
        }
        for ( P2LineupUnresolvedInstallableUnit unresolvedInstallableUnit : e.getUnresolvedInstallableUnits() )
        {
            response.addError( unresolvedInstallableUnit );
        }
        return response;
    }
    
    protected P2LineupErrorResponse createErrorResponse( Throwable t )
    {
        return this.createErrorResponse( t.getMessage() );
    }
}
