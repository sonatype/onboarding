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
import java.net.MalformedURLException;
import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.model.IP2LineupLocation;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.descriptor.P2LineupLocation;
import com.sonatype.s2.project.validation.api.IS2ProjectValidator;
import com.sonatype.s2.project.validation.api.S2ProjectValidationContext;
import com.sonatype.s2.project.validator.AbstractValidatorTest;

@SuppressWarnings( "restriction" )
public class EclipseInstallationValidatorTest
    extends AbstractValidatorTest
{
    private static final String USERNAME = "username";

    private static final String PASSWORD = "password";

    private static final String ROLE = "role";

    private HttpServer httpServer;

    private String baseUrl;

    @Override
    protected void tearDown()
        throws Exception
    {
        if ( httpServer != null )
        {
            httpServer.stop();
        }
        Thread.sleep( 5000 );
        super.tearDown();
    }

    private void startHttpServer()
        throws Exception
    {
        httpServer = new HttpServer();
        httpServer.addResources( "/", "resources", "xml" );
        httpServer.addSecuredRealm( "/p2lineup/testaccess/*", ROLE );
        httpServer.addUser( USERNAME, PASSWORD, ROLE );
        httpServer.start();
        baseUrl = httpServer.getHttpUrl();
    }

    public void testProjectWithoutP2Lineup()
        throws Exception
    {
        IS2Project s2Project = loadS2Project( "resources/projects/descriptorWithoutP2Lineup.xml" );

        EclipseInstallationValidator validator = new EclipseInstallationValidator();
        IStatus validationStatus = validator.validate( s2Project, new NullProgressMonitor() );
        assertNotNull( validator.getValidationStatus() );

        assertTrue( validationStatus.isOK() );
        assertEquals( IStatus.OK, validationStatus.getCode() );
        assertEquals( "OK (The 'Foo' S2 project does not have a p2 lineup URL.)", validationStatus.getMessage() );
        assertFalse( validator.canRemediate( true ).isOK() );
        assertFalse( validator.canRemediate( false ).isOK() );
    }

    public void testProjectWithP2Lineup_WithP2Profile_NotAuthenticated()
        throws Exception
    {
        String testProfileId = "testProjectWithP2Lineup_NotAuthenticated";
        IS2Project s2Project = loadS2Project( "resources/projects/descriptorWithoutP2Lineup.xml" );
        File p2LineupFile = new File( "resources/p2lineup/dummy" );
        String p2LineupUrlStr = p2LineupFile.toURI().toURL().toString();
        IP2LineupLocation p2LineupLocation = new P2LineupLocation();
        p2LineupLocation.setUrl( p2LineupUrlStr );
        s2Project.setP2LineupLocation( p2LineupLocation );

        EclipseInstallationValidator validator = new EclipseInstallationValidator();
        validator.setProfileIdForUnitTests( testProfileId );
        IStatus validationStatus = validator.validate( s2Project, new NullProgressMonitor() );
        assertNotNull( validator.getValidationStatus() );

        // The p2 lineup IU is not installed, but that's ok if all its requirements are installed
        assertTrue( validationStatus.toString(), validationStatus.isOK() );
        assertEquals( IStatus.OK, validationStatus.getCode() );
        assertEquals( EclipseInstallationValidator.SUCCESS_MESSAGE, validationStatus.getMessage() );
        assertFalse( validator.canRemediate( true ).isOK() );
        assertFalse( validator.canRemediate( false ).isOK() );
    }

    public void testProjectWithP2Lineup_WithP2Profile_Authenticated()
        throws Exception
    {
        String testProfileId = "testProjectWithP2Lineup_Authenticated";
        startHttpServer();

        String projectURL = baseUrl + "/projects/descriptorWithAuthenticatedP2LineupURL.xml";
        IS2Project s2Project = S2ProjectCore.getInstance().loadProject( projectURL, new NullProgressMonitor() );

        IP2LineupLocation p2LineupLocation = s2Project.getP2LineupLocation();
        assertNotNull( p2LineupLocation );
        String p2LineupUrlStr = p2LineupLocation.getUrl();
        assertNotNull( p2LineupUrlStr );
        URI p2LineupUri = new URI( p2LineupUrlStr );
        // Cleanup any existing credentials for this p2 lineup
        P2AuthHelper.removeCredentials( p2LineupUri );

        addRealmAndURL( "EclipseInstallationValidatorTest", httpServer.getHttpUrl(), USERNAME, PASSWORD );

        EclipseInstallationValidator validator = new EclipseInstallationValidator();
        validator.setProfileIdForUnitTests( testProfileId );
        IStatus validationStatus = validator.validate( s2Project, new NullProgressMonitor() );
        assertNotNull( validator.getValidationStatus() );
        assertEquals( validationStatus.getMessage(), EclipseInstallationValidator.CAN_TRY_REMEDIATE_STATUS_CODE,
                      validationStatus.getCode() );

        // The p2 lineup IU requirements are not installed, so we should get an error status
        assertEquals( IStatus.ERROR, validationStatus.getSeverity() );
        assertEquals( "Missing installable units:\n   org.eclipse.core.boot 0.0.0", validationStatus.getMessage() );
        assertFalse( P2AuthHelper.hasCredentialsForURI( p2LineupUri ) );

        assertTrue( validator.canRemediate( false ).isOK() );
        assertFalse( P2AuthHelper.hasCredentialsForURI( p2LineupUri ) );

        // Install the p2 lineup IU
        IStatus installationStatus = validator.remediate( true /* headless */, new NullProgressMonitor() );
        assertNotNull( installationStatus );
        assertTrue( installationStatus.isOK() );
        assertFalse( P2AuthHelper.hasCredentialsForURI( p2LineupUri ) );

        // Validate again
        validationStatus = validator.validate( s2Project, new NullProgressMonitor() );
        assertNotNull( validator.getValidationStatus() );
        assertEquals( IStatus.OK, validationStatus.getSeverity() );
        assertFalse( validator.canRemediate( true ).isOK() );
        assertFalse( validator.canRemediate( false ).isOK() );
    }

    public void testProjectWithInvalidP2LineupUrl()
        throws Exception
    {
        String testProfileId = "testProjectWithInvalidP2Lineup";
        IS2Project s2Project = loadS2Project( "resources/projects/descriptorWithoutP2Lineup.xml" );
        IP2LineupLocation p2LineupLocation = new P2LineupLocation();
        p2LineupLocation.setUrl( "foo" );
        s2Project.setP2LineupLocation( p2LineupLocation );

        EclipseInstallationValidator validator = new EclipseInstallationValidator();
        validator.setProfileIdForUnitTests( testProfileId );
        IStatus validationStatus = validator.validate( s2Project, new NullProgressMonitor() );
        assertNotNull( validator.getValidationStatus() );
        assertNotNull( validationStatus.getException() );
        assertTrue( validationStatus.getException() instanceof MalformedURLException );
        assertTrue( EclipseInstallationValidator.CAN_TRY_REMEDIATE_STATUS_CODE != validationStatus.getCode() );
        assertFalse( validator.canRemediate( true ).isOK() );
        assertFalse( validator.canRemediate( false ).isOK() );
    }

    protected IS2Project loadS2Project( String projectDescriptorFileName )
        throws Exception
    {
        File projectDescriptorFile = new File( projectDescriptorFileName );
        assertTrue( projectDescriptorFile.exists() );
        return S2ProjectCore.getInstance().loadProject( projectDescriptorFile.toURI().toURL().toExternalForm(),
                                                        new NullProgressMonitor() );
    }

    public void testIsApplicable()
        throws Exception
    {
        EclipseInstallationValidator validator = new EclipseInstallationValidator();
        assertTrue( validator.isApplicable( IS2ProjectValidator.NULL_VALIDATION_CONTEXT ) );

        S2ProjectValidationContext validationContext = new S2ProjectValidationContext();
        validationContext.setProperty( S2ProjectValidationContext.FRESH_INSTALL_PROPNAME, "true" );
        assertFalse( validator.isApplicable( validationContext ) );
    }
}
