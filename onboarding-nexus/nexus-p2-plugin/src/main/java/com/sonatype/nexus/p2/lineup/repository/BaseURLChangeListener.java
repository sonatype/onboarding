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

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.sonatype.nexus.configuration.Configurable;
import org.sonatype.nexus.configuration.ConfigurationChangeEvent;
import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.plexus.appevents.ApplicationEventMulticaster;
import org.sonatype.plexus.appevents.Event;
import org.sonatype.plexus.appevents.EventListener;

@Component( role = BaseURLChangeListener.class )
public class BaseURLChangeListener
    implements EventListener, Initializable, Disposable
{
    public static final String TASKNAME_REBUILD_LINEUPS = "Rebuilding P2 Lineups";

    @Requirement
    private Logger logger;

    @Requirement
    private ApplicationEventMulticaster applicationEventMulticaster;

    @Requirement
    private GlobalRestApiSettings globalRestApiSettings;

    private String baseURL;

    public void onEvent( Event<?> evt )
    {
        if ( evt instanceof ConfigurationChangeEvent )
        {
            for ( Configurable c : ( (ConfigurationChangeEvent) evt ).getChanges() )
            {
                if ( c instanceof GlobalRestApiSettings )
                {
                    String baseURL = ( (GlobalRestApiSettings) c ).getBaseUrl();

                    logger.debug( "GlobalRestApiSettings changed. Old baseURL=" + this.baseURL + ". New baseURL=" + baseURL );

                    if ( baseURL != null && !baseURL.equals( this.baseURL ) )
                    {
                        //new code will be put here that will update metadata, not full publish
                        //see https://issues.sonatype.org/browse/MECLIPSE-958
                        //also uncomment the MoveRepositoryBaseUrlIT when this code is complete
                        /**
                        PublishP2LineupTask lineUpTask = scheduler.createTaskInstance( PublishP2LineupTask.class );
                        scheduler.submit( TASKNAME_REBUILD_LINEUPS, lineUpTask );
                        logger.debug( "Submitted " + TASKNAME_REBUILD_LINEUPS );
                        **/
                    }

                    this.baseURL = baseURL;
                }
            }
        }
    }

    public void dispose()
    {
        applicationEventMulticaster.removeEventListener( this );
    }

    public void initialize()
        throws InitializationException
    {
        this.baseURL = globalRestApiSettings.getBaseUrl();

        applicationEventMulticaster.addEventListener( this );

        logger.info( "Initialized nexus/p2 BaseURLChangeListener. baseURL=" + this.baseURL );
    }

}
