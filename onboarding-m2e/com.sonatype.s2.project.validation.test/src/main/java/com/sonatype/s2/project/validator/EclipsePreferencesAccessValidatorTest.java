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
package com.sonatype.s2.project.validator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.model.IS2Project;

public class EclipsePreferencesAccessValidatorTest
    extends AbstractValidatorTest
{
    private HttpServer httpServer;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        httpServer = new HttpServer();
        httpServer.addResources( "/", "resources/eclipse-preferences", "xml" );
        httpServer.addSecuredRealm( "/prefs/*", "dev" );
        httpServer.addUser( "testuser", "testpass", "dev" );
        httpServer.addUser( "guest", "guest", "anon" );
        httpServer.setHttpsPort( -1 );
        httpServer.start();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        if ( httpServer != null )
        {
            httpServer.stop();
        }

        super.tearDown();
    }

    public void testNoLocation()
        throws Exception
    {
        IS2Project project = loadProject( "no-preferences-location" );

        assertNull( project.getEclipsePreferencesLocation() );

        IStatus status = validate( project, null );
        assertNull( status );
    }

    public void testGoodCredentials()
        throws Exception
    {
        IS2Project project = loadProject( "credentials" );

        addRealmAndURL( "s2.test.eclipse-preferences", httpServer.getHttpUrl(), "testuser", "testpass" );
        IStatus status = validate( project, null );
        assertStatus( status, null );

        status = validate( project, "s2.test.eclipse-preferences" );
        assertStatus( status, null );

        status = validate( project, "s2.test.eclipse-preferences.unknown" );
        assertNull( status );
    }

    public void testUnauthorized()
        throws Exception
    {
        IS2Project project = loadProject( "credentials" );

        addRealmAndURL( "s2.test.eclipse-preferences", httpServer.getHttpUrl(), "testuser", "badpass" );
        IStatus status = validate( project, null );
        assertStatus( status, "401" );

        status = validate( project, "s2.test.eclipse-preferences" );
        assertStatus( status, "401" );

        status = validate( project, "s2.test.eclipse-preferences.unknown" );
        assertNull( status );
    }

    public void testForbidden()
        throws Exception
    {
        IS2Project project = loadProject( "credentials" );

        addRealmAndURL( "s2.test.eclipse-preferences", httpServer.getHttpUrl(), "guest", "guest" );
        IStatus status = validate( project, null );
        assertStatus( status, "403" );

        status = validate( project, "s2.test.eclipse-preferences" );
        assertStatus( status, "403" );

        status = validate( project, "s2.test.eclipse-preferences.unknown" );
        assertNull( status );
    }

    public void testBadLocation()
        throws Exception
    {
        IS2Project project = loadProject( "bad-location" );

        addRealmAndURL( "s2.test.eclipse-preferences", httpServer.getHttpUrl(), "guest", "guest" );
        IStatus status = validate( project, null );
        assertStatus( status, "404" );

        status = validate( project, "s2.test.eclipse-preferences" );
        assertStatus( status, "404" );

        status = validate( project, "s2.test.eclipse-preferences.unknown" );
        assertNull( status );
    }

    private IS2Project loadProject( String name )
        throws Exception
    {
        String pmdUrl = httpServer.getHttpUrl() + "/" + name + ".xml";
        return S2ProjectCore.getInstance().loadProject( pmdUrl, new NullProgressMonitor() );
    }

    private IStatus validate( IS2Project project, String securityRealmIdFilter )
        throws Exception
    {
        EclipsePreferencesAccessValidator validator = new EclipsePreferencesAccessValidator();

        IStatus status = validator.validate( project, securityRealmIdFilter, new NullProgressMonitor() );

        assertTrue( status == null || status.isOK() || status instanceof AccessValidationStatus );

        assertFalse( validator.canRemediate( true ).isOK() );
        assertFalse( validator.canRemediate( false ).isOK() );

        return status;
    }

    private void assertStatus( IStatus status, String msg )
    {
        if ( msg == null )
        {
            assertTrue( status.toString(), status.isOK() );
        }
        else
        {
            assertFalse( status.toString(), status.isOK() );
            assertEquals( IStatus.ERROR, status.getSeverity() );
            assertTrue( status.toString(), status.toString().contains( msg ) );
        }
    }

}
