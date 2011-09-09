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

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryRefreshJob;

public abstract class MECLIPSE544Test
    extends AbstractMavenProjectMaterializationTest
{
    /**
     * Tests that import of a multi-module project does not contact remote repositories more than once (per refresh) to
     * resolve a snapshot version but uses a cache.
     */
    public void _testRepositoryMetadataCacheUsed()
        throws Exception
    {
        HttpServer httpServer = newHttpServer();
        httpServer.enableRecording( "/remote-repo/meclipse544/snapshot/0.0.1-SNAPSHOT/.*" );
        httpServer.start();

        List<String> requests;

        IJobChangeListener jobChangeListener = new JobChangeAdapter()
        {
            public void scheduled( IJobChangeEvent event )
            {
                if ( event.getJob() instanceof ProjectRegistryRefreshJob )
                {
                    // cancel all those concurrent refresh jobs, we want to monitor the main thread only
                    event.getJob().cancel();
                }
            }
        };
        Job.getJobManager().addJobChangeListener( jobChangeListener );
        try
        {
            materialize( httpServer.getHttpUrl() + "/projects/MECLIPSE-544-metadata-cache/s2.xml" );

            requests = httpServer.getRecordedRequests();
        }
        finally
        {
            Job.getJobManager().removeJobChangeListener( jobChangeListener );
        }

        assertWorkspaceProjects( 6 );

        assertFalse( requests.isEmpty() );
        requests.retainAll( Arrays.asList( "GET /remote-repo/meclipse544/snapshot/0.0.1-SNAPSHOT/maven-metadata.xml" ) );
        assertFalse( requests.isEmpty() );
        assertTrue( "Accessed metadata " + requests.size() + " times", requests.size() == 1 );
    }

}
