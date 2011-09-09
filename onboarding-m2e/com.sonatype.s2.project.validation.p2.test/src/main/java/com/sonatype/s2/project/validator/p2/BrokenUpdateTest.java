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
package com.sonatype.s2.project.validator.p2;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

import com.sonatype.s2.project.core.ide.IIDEUpdater;
import com.sonatype.s2.project.model.IP2LineupLocation;
import com.sonatype.s2.project.model.descriptor.P2LineupLocation;

public class BrokenUpdateTest
    extends AbstractMavenProjectTestCase
{
    public void testIsUpToDateOfEclipseNotCreatedByALineup()
        throws Exception, CoreException
    {
        String testProfileId = getName();
        File afterLineupFile = new File( "resources/testBrokenInitialState/before/p2Lineup" );
        IP2LineupLocation afterLineupLocation = new P2LineupLocation();
        afterLineupLocation.setUrl( afterLineupFile.toURI().toURL().toString() );

        EclipseInstallationValidator updater = new EclipseInstallationValidator();
        updater.setProfileIdForUnitTests( testProfileId );
        assertFalse( updater.isLineupManaged( monitor ) );
        assertEquals( IIDEUpdater.NOT_LINEUP_MANAGED,
                      updater.isUpToDate( afterLineupLocation, new NullProgressMonitor() ) );
    }

    public void testUpdateOfEclipseNotCreatedByALineup()
        throws Exception
    {
        String testProfileId = getName();
        File afterLineupFile = new File( "resources/testSuccessfullUpdate/after/p2Lineup" );
        IP2LineupLocation afterLineupLocation = new P2LineupLocation();
        afterLineupLocation.setUrl( afterLineupFile.toURI().toURL().toString() );
        EclipseInstallationValidator updater = new EclipseInstallationValidator();
        updater.setProfileIdForUnitTests( testProfileId );
        assertFalse( updater.isLineupManaged( monitor ) );
        IStatus updateResult = updater.performUpdate( afterLineupLocation.getUrl(), monitor );
        assertEquals( updateResult.toString(), IIDEUpdater.NOT_LINEUP_CREATED_STATUS, updateResult );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        Thread.sleep( 5000 );
        super.tearDown();
    }
}
