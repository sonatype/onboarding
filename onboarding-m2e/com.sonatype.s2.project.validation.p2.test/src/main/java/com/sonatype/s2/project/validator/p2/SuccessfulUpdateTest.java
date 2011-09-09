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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.model.IP2LineupLocation;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.descriptor.P2LineupLocation;
import com.sonatype.s2.project.validator.AbstractValidatorTest;

public class SuccessfulUpdateTest
    extends AbstractValidatorTest
{
    public void testSuccesfullUpdate()
        throws Exception
    {
        // Create the initial state
        String testProfileId = getName();

        String projectURL =
            new File( "resources/testSuccessfullUpdate/before/project/beforeCodebaseDescriptor.xml" ).toURI().toURL().toString();
        IS2Project s2Project = S2ProjectCore.getInstance().loadProject( projectURL, new NullProgressMonitor() );

        IP2LineupLocation p2LineupLocation = s2Project.getP2LineupLocation();
        assertNotNull( p2LineupLocation );
        p2LineupLocation.setUrl( new File( "resources/testSuccessfullUpdate/before/p2Lineup" ).toURI().toURL().toString() );

        EclipseInstallationValidator validator = new EclipseInstallationValidator();
        IP2Lineup beforeLineup = validator.loadP2Lineup( p2LineupLocation.getUrl(), new NullProgressMonitor() );
        validator.setProfileIdForUnitTests( testProfileId );
        IStatus validationStatus = validator.validate( s2Project, new NullProgressMonitor() );
        assertNotNull( validator.getValidationStatus() );
        assertEquals( validationStatus.getMessage(), EclipseInstallationValidator.CAN_TRY_REMEDIATE_STATUS_CODE,
                      validationStatus.getCode() );

        validator.canRemediate( true );
        IStatus installationStatus = validator.remediate( true /* headless */, new NullProgressMonitor() );
        assertNotNull( installationStatus );
        assertTrue( installationStatus.isOK() );

        // Now start the real testing for update
        File afterLineupFile = new File( "resources/testSuccessfullUpdate/after/p2Lineup" );
        IP2LineupLocation afterLineupLocation = new P2LineupLocation();
        afterLineupLocation.setUrl( afterLineupFile.toURI().toURL().toString() );
        EclipseInstallationValidator updater = new EclipseInstallationValidator();
        updater.setProfileIdForUnitTests( testProfileId );
        assertTrue( updater.isLineupManaged( monitor ) );
        IStatus updateResult = updater.performUpdate( afterLineupLocation.getUrl(), new NullProgressMonitor() );
        assertEquals( IStatus.OK, updateResult.getSeverity() );
        assertTrue( updater.isLineupManaged( monitor ) );
    }
    
    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        Thread.sleep( 5000 );
    }
}
