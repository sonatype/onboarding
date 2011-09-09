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
package com.sonatype.s2.project.integration.test.svn;

import junit.framework.TestCase;

import org.eclipse.team.svn.core.SVNTeamPlugin;

import com.sonatype.m2e.subversive.SubversiveHelper;
import com.sonatype.m2e.subversive.internal.NonInteractiveOptionProvider;

public class SubversiveHelperTest
    extends TestCase
{
    public void testInstallAndRestoreNonInteractiveOptionProvider()
    {
        assertSvnUsesInteractiveOptionProvider();

        SubversiveHelper.installNonInteractiveOptionProvider();
        assertSvnUsesNonInteractiveOptionProvider();
        SubversiveHelper.installNonInteractiveOptionProvider();
        assertSvnUsesNonInteractiveOptionProvider();

        SubversiveHelper.restoreOptionProvider();
        assertSvnUsesNonInteractiveOptionProvider();
        SubversiveHelper.restoreOptionProvider();
        assertSvnUsesInteractiveOptionProvider();

        // Should not fail
        SubversiveHelper.restoreOptionProvider();
        assertSvnUsesInteractiveOptionProvider();
    }

    public void testInstallAndRestoreNonInteractiveOptionProvider_MultiThreaded()
        throws Throwable
    {
        assertSvnUsesInteractiveOptionProvider();

        InstallRestoreOptionProviderThread[] threads = new InstallRestoreOptionProviderThread[10];
        for ( int i = 0; i < threads.length; i++ )
        {
            threads[i] = new InstallRestoreOptionProviderThread( 10 );
        }
        for ( int i = 0; i < threads.length; i++ )
        {
            threads[i].start();
        }
        for ( int i = 0; i < threads.length; i++ )
        {
            threads[i].join();
        }
        for ( int i = 0; i < threads.length; i++ )
        {
            if ( threads[i].error != null )
            {
                throw threads[i].error;
            }
        }

        assertSvnUsesInteractiveOptionProvider();
    }

    private static class InstallRestoreOptionProviderThread
        extends Thread
    {
        public Throwable error;

        private int count;

        private static int threadId = 0;

        InstallRestoreOptionProviderThread( int count )
        {
            super( "InstallRestoreOptionProviderThread" + ( threadId++ ) );
            this.count = count;
        }

        @Override
        public void run()
        {
            try
            {
                for ( int i = 0; i < count; i++ )
                {
                    SubversiveHelper.installNonInteractiveOptionProvider();
                    Thread.sleep( 50 );
                }
                for ( int i = 0; i < count; i++ )
                {
                    SubversiveHelper.restoreOptionProvider();
                    Thread.sleep( 50 );
                }
            }
            catch ( Throwable t )
            {
                error = t;
            }
        }
    }

    private void assertSvnUsesNonInteractiveOptionProvider()
    {
        assertTrue( "Expected NonInteractiveOptionProvider",
                    SVNTeamPlugin.instance().getOptionProvider() instanceof NonInteractiveOptionProvider );
    }

    private void assertSvnUsesInteractiveOptionProvider()
    {
        assertFalse( "Expected original option provider",
                     SVNTeamPlugin.instance().getOptionProvider() instanceof NonInteractiveOptionProvider );
    }
}
