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
package com.sonatype.s2.project.ui.test;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.tests.common.JobHelpers;

public class UiHelpers
{

    public static void waitForAllUiJobsToComplete( int maxWaitMillis )
    {
        JobHelpers.waitForJobs( new JobHelpers.IJobMatcher()
        {
            public boolean matches( Job job )
            {
                return job.getClass().getName().startsWith( "com.sonatype.s2.project.ui" );
            }
        }, maxWaitMillis );
    }

}
