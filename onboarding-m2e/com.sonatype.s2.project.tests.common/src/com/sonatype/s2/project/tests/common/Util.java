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
package com.sonatype.s2.project.tests.common;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.eclipse.core.runtime.jobs.Job;

import com.sonatype.s2.project.core.S2ProjectCore;

public class Util
{

    public static void resetProjectCore()
    {
        for ( Field field : S2ProjectCore.class.getDeclaredFields() )
        {
            if ( Modifier.isStatic( field.getModifiers() ) && field.getType() == S2ProjectCore.class )
            {
                field.setAccessible( true );
                try
                {
                    field.set( null, new S2ProjectCore() );
                }
                catch ( IllegalAccessException e )
                {
                    throw new IllegalStateException( e );
                }
                return;
            }
        }
    }

    public static void waitForTeamSharingJobs()
    {
        /*
         * NOTE: Project sharing happens by jobs and we better wait for these jobs to finish before we assert any
         * project is shared. In the case of Subversive, the additional issue is that its "Reconnect project" job would
         * fail with a blocking UI error dialog in case it gets run after the test has been teared down and cleaned the
         * workspace.
         */
        waitForJobs( "(.*\\.AutoBuild.*)" + "|(org\\.eclipse\\.team\\.svn\\.core.*)"
            + "|(org\\.tigris\\.subversion\\.subclipse\\.core.*)"
            + "|(org\\.eclipse\\.team\\.internal\\.ccvs\\.core.*)", 10000 );
    }

    private static void waitForJobs( String classNameRegex, int maxWaitMillis )
    {
        final int waitMillis = 250;
        for ( int i = maxWaitMillis / waitMillis; i >= 0; i-- )
        {
            if ( !hasJob( classNameRegex ) )
            {
                return;
            }
            try
            {
                Thread.sleep( waitMillis );
            }
            catch ( InterruptedException e )
            {
                // ignore
            }
        }
    }

    private static boolean hasJob( String classNameRegex )
    {
        Job[] jobs = Job.getJobManager().find( null );
        for ( Job job : jobs )
        {
            if ( job.getClass().getName().matches( classNameRegex ) )
            {
                System.out.println( "Found job " + job.getClass().getName() );
                return true;
            }
        }
        System.out.println( "No jobs that match " + classNameRegex );
        return false;
    }

}
