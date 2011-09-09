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
package com.sonatype.s2.project.ui.internal.wizards;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.Assert;
import org.junit.Test;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.io.ForbiddenException;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputComposite;
import org.maven.ide.eclipse.ui.tests.common.AbstractWizardPageTest;

import com.sonatype.s2.nexus.NexusFacade;
import com.sonatype.s2.nexus.securityrealm.persistence.NexusSecurityRealmPersistence;
import com.sonatype.s2.project.core.test.DummySecurityRealmPersistence;
import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.ui.internal.composites.NexusUrlComposite;

public class NexusUrlPageTest
    extends AbstractWizardPageTest
{
    private HttpServer httpServer;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        NexusFacade.removeMainNexusServerURL();
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

    @Test
    public void testNullNexusUrl()
    {
        NexusUrlPage page = createTestPage( null, 0 );

        assertCombo( page, UrlInputComposite.URL_CONTROL_NAME, "", true );
        assertText( page, UrlInputComposite.USERNAME_TEXT_NAME, "", true, true );
        assertText( page, UrlInputComposite.PASSWORD_TEXT_NAME, "", true, true );
    }

    @Test
    public void testBogusNexusUrlNoCredentials()
    {
        String nexusUrl = "http://foo/NexusUrlPageTest/testBogusNexusUrlNoCredentials";
        Assert.assertNotSame( nexusUrl, NexusFacade.getMainNexusServerURL() );

        NexusUrlPage page = createTestPage( nexusUrl, 0 );

        assertCombo( page, UrlInputComposite.URL_CONTROL_NAME, nexusUrl, true );
        assertText( page, UrlInputComposite.USERNAME_TEXT_NAME, "", true, true );
        assertText( page, UrlInputComposite.PASSWORD_TEXT_NAME, "", true, true );

        IStatus status = page.checkNexus( nexusUrl, monitor );
        Assert.assertEquals( IStatus.ERROR, status.getSeverity() );
        // System.out.println( status.getMessage() );
        Assert.assertNotSame( nexusUrl, NexusFacade.getMainNexusServerURL() );
    }

    @Test
    public void testUrlControlDisabled()
    {
        String nexusUrl = "http://foo/NexusUrlPageTest/testUrlControlDisabled";
        Assert.assertNotSame( nexusUrl, NexusFacade.getMainNexusServerURL() );

        NexusUrlPage page = createTestPage( nexusUrl, NexusUrlComposite.READ_ONLY_URL );

        assertText( page, UrlInputComposite.URL_CONTROL_NAME, nexusUrl, false, true );
        assertText( page, UrlInputComposite.USERNAME_TEXT_NAME, "", true, true );
        assertText( page, UrlInputComposite.PASSWORD_TEXT_NAME, "", true, true );

        IStatus status = page.checkNexus( nexusUrl, monitor );
        Assert.assertEquals( IStatus.ERROR, status.getSeverity() );
        // System.out.println( status.getMessage() );
        Assert.assertNotSame( nexusUrl, NexusFacade.getMainNexusServerURL() );
    }

    @Test
    public void testBogusNexusUrlWithCredentials()
    {
        String nexusUrl = "http://foo/NexusUrlPageTest/testBogusNexusUrlWithCredentials";
        Assert.assertNotSame( nexusUrl, NexusFacade.getMainNexusServerURL() );

        String username = "testusername";
        String password = "testpassword";
        AuthFacade.getAuthService().save( nexusUrl, username, password );
        NexusUrlPage page = createTestPage( nexusUrl, 0 );

        assertCombo( page, UrlInputComposite.URL_CONTROL_NAME, nexusUrl, true );
        assertText( page, UrlInputComposite.USERNAME_TEXT_NAME, username, true, true );
        assertText( page, UrlInputComposite.PASSWORD_TEXT_NAME, password, true, true );

        IStatus status = page.checkNexus( nexusUrl, monitor );
        Assert.assertEquals( IStatus.ERROR, status.getSeverity() );
        // System.out.println( status.getMessage() );
        Assert.assertNotSame( nexusUrl, NexusFacade.getMainNexusServerURL() );
    }

    @Test
    public void testNexusUrlGoodCredentials()
        throws Exception
    {
        String username = "testusername";
        String password = "testpassword";

        httpServer = new HttpServer();
        httpServer.addResources( "/", "resources/NexusUrlPageTest/testNexusUrlGoodCredentials", "" );
        httpServer.addSecuredRealm( "/*", "role" );
        httpServer.addUser( username, password, "role" );
        httpServer.start();

        String nexusUrl = httpServer.getHttpUrl();
        Assert.assertNotSame( nexusUrl, NexusFacade.getMainNexusServerURL() );
        AuthFacade.getAuthService().save( nexusUrl, username, password );
        NexusUrlPage page = createTestPage( nexusUrl, 0 );

        assertCombo( page, UrlInputComposite.URL_CONTROL_NAME, nexusUrl, true );
        assertText( page, UrlInputComposite.USERNAME_TEXT_NAME, username, true, true );
        assertText( page, UrlInputComposite.PASSWORD_TEXT_NAME, password, true, true );

        IStatus status = page.checkNexus( nexusUrl, monitor );
        Assert.assertTrue( status.isOK() );
        // System.out.println( status.getMessage() );
        Assert.assertSame( nexusUrl, NexusFacade.getMainNexusServerURL() );
    }

    @Test
    public void testNexusUrlBadCredentials()
        throws Exception
    {
        String username = "testusername";
        String password = "testpassword";

        httpServer = new HttpServer();
        httpServer.addResources( "/", "resources/NexusUrlPageTest/testNexusUrlBadCredentials", "" );
        httpServer.addSecuredRealm( "/*", "role" );
        httpServer.addUser( username, password, "role" );
        httpServer.start();

        String nexusUrl = httpServer.getHttpUrl();
        Assert.assertNotSame( nexusUrl, NexusFacade.getMainNexusServerURL() );
        AuthFacade.getAuthService().save( nexusUrl, username, "bad password" );
        NexusUrlPage page = createTestPage( nexusUrl, 0 );

        assertCombo( page, UrlInputComposite.URL_CONTROL_NAME, nexusUrl, true );
        assertText( page, UrlInputComposite.USERNAME_TEXT_NAME, username, true, true );
        assertText( page, UrlInputComposite.PASSWORD_TEXT_NAME, "bad password", true, true );

        IStatus status = page.checkNexus( nexusUrl, monitor );
        Assert.assertEquals( IStatus.ERROR, status.getSeverity() );
        Assert.assertEquals( status.toString(), "Login failed, please check your username/password.",
                             status.getMessage() );
        // System.out.println( status.getMessage() );
        Assert.assertNotSame( nexusUrl, NexusFacade.getMainNexusServerURL() );
    }

    /*
     * Test what occurs when the user doesn't have permission to access security realms
     */
    @Test
    public void testNexusUrlNoRealmPermissions()
        throws Exception
    {
        String username = "testusername";
        String password = "testpassword";

        httpServer = new HttpServer();
        httpServer.addResources( "/", "resources/NexusUrlPageTest/testNexusUrlGoodCredentials", "" );
        httpServer.start();
        try
        {
            DummySecurityRealmPersistence.setException( new ForbiddenException( "403" ) );
            Assert.assertFalse( NexusSecurityRealmPersistence.isInUse() );
            new NexusSecurityRealmPersistence().setActive( true );

            String nexusUrl = httpServer.getHttpUrl();
            Assert.assertNotSame( nexusUrl, NexusFacade.getMainNexusServerURL() );

            // AuthFacade.getAuthService().save( nexusUrl, username, password );
            NexusUrlPage page =
                createTestPage( nexusUrl, NexusUrlComposite.READ_ONLY_URL | NexusUrlComposite.ALLOW_ANONYMOUS );
            setText( page, UrlInputComposite.PASSWORD_TEXT_NAME, password );
            setText( page, UrlInputComposite.USERNAME_TEXT_NAME, username );
            Assert.assertNotNull( page.getNextPage() );
            Assert.assertTrue( "Page message:" + page.getMessage(), page.getMessage() == null
                || page.getMessage().trim().length() == 0 );
        }
        finally
        {
            DummySecurityRealmPersistence.setException( null );
            new NexusSecurityRealmPersistence().setActive( false );
        }
    }

    private static class DummyWizard
        extends Wizard
    {
        @Override
        public boolean performFinish()
        {
            return false;
        }
    }

    private static class DummyPage
        extends WizardPage
    {
        protected DummyPage()
        {
            super( "DummyPage" );
        }

        public void createControl( Composite parent )
        {
            setControl( new Label( parent, SWT.NONE ) );
        }
    }

    private NexusUrlPage createTestPage( String nexusUrl, int urlInputStyle )
    {
        if ( wizard != null )
        {
            wizard.dispose();
        }
        wizard = new DummyWizard();
        NexusUrlPage page = new NexusUrlPage( nexusUrl, urlInputStyle );
        wizard.addPage( page );
        // Add a second page so we can check getNextPage()
        wizard.addPage( new DummyPage() );

        Display display = Display.getCurrent();
        if ( display == null )
        {
            display = new Display();
        }
        Shell shell = new Shell( display );

        WizardDialog dialog = new WizardDialog( shell, wizard );
        dialog.create();
        return page;
    }
}
