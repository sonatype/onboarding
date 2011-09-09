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
package com.sonatype.nexus.p2.lineup.task;

import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.feeds.FeedRecorder;
import org.sonatype.nexus.scheduling.AbstractNexusTask;
import org.sonatype.scheduling.SchedulerTask;

import com.sonatype.nexus.p2.lineup.persist.P2Gav;
import com.sonatype.nexus.p2.lineup.persist.P2LineupManager;
import com.sonatype.nexus.p2.lineup.repository.P2LineupRepository;
import com.sonatype.nexus.p2.lineup.resolver.CannotResolveP2LineupException;
import com.sonatype.nexus.p2.lineup.resolver.P2LineupResolver;
import com.sonatype.s2.p2lineup.model.P2Lineup;

/**
 * Publish p2 lineup repository.
 * 
 * @author velo
 */
@Component( role = SchedulerTask.class, hint = PublishP2LineupTaskDescriptor.ID, instantiationStrategy = "per-lookup" )
public class PublishP2LineupTask
    extends AbstractNexusTask<Object>
{
    @Requirement
    private P2LineupResolver lineupResolver;

    @Requirement
    private P2LineupManager lineupManager;

    @Override
    protected Object doRun()
        throws Exception
    {
        P2LineupRepository repo = lineupManager.getDefaultP2LineupRepository();
        Set<P2Lineup> lineups = lineupManager.getLineups();
        for ( P2Lineup lineup : lineups )
        {
            try
            {
                lineupResolver.resolveLineup( repo, lineup, false );
            }
            catch ( CannotResolveP2LineupException e )
            {
                // the task should not fail/stop if a lineup cannot be resolved
                if ( e.isFatal() )
                {
                    getLogger().error( "Error resolving lineup: " + new P2Gav( lineup ) + ": " + e.getMessage(), e );
                }
                else
                {
                    getLogger().debug( "Warning resolving lineup: " + new P2Gav( lineup ) + ": " + e.getMessage() );
                }
            }
            catch ( Exception e )
            {
                // the task should not fail/stop if a lineup cannot be resolved
                getLogger().error( "Error resolving lineup: " + new P2Gav( lineup ) + ": " + e.getMessage(), e );
            }
        }

        return null;
    }

    @Override
    protected String getAction()
    {
        return FeedRecorder.SYSTEM_PUBLISHINDEX_ACTION;
    }

    @Override
    protected String getMessage()
    {
        return "Publishing P2Lineup for repositories";
    }
}
