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
package com.sonatype.s2.project.core.test;

import java.util.List;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

public class MECLIPSE409Test
    extends AbstractMavenProjectMaterializationTest
{

    /**
     * Tests that project importing does not contact remote repositories to resolve dependencies which are part of the
     * workspace.
     */
    public void testWorkspaceResolutionOfInterModuleDependenciesDuringImport()
        throws Exception
    {
        HttpServer httpServer = newHttpServer();
        httpServer.enableRecording( "/remote-repo/test/.*" );
        httpServer.start();

        List<String> requests;

        IJobChangeListener jobChangeListener = new JobChangeAdapter()
        {
            public void scheduled( IJobChangeEvent event )
            {
                if ( event.getJob().getClass().getName().endsWith( "MavenProjectManagerRefreshJob" ) )
                {
                    // cancel all those concurrent refresh jobs, we want to monitor the main thread only
                    event.getJob().cancel();
                }
            }
        };
        Job.getJobManager().addJobChangeListener( jobChangeListener );
        try
        {
            materialize( httpServer.getHttpUrl() + "/projects/MECLIPSE-409-inter-mod-deps/s2.xml" );

            requests = httpServer.getRecordedRequests();
        }
        finally
        {
            Job.getJobManager().removeJobChangeListener( jobChangeListener );
        }

        assertWorkspaceProjects( 4 );

        assertTrue( "Dependency resolution was attempted from remote repository: " + requests, requests.isEmpty() );
    }

}
