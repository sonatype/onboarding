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

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Assert;
import org.junit.Test;
import org.maven.ide.eclipse.ui.tests.common.AbstractWizardPageTest;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.descriptor.EclipseInstallationLocation;
import com.sonatype.s2.project.model.descriptor.EclipseWorkspaceLocation;
import com.sonatype.s2.project.model.descriptor.Project;
import com.sonatype.s2.project.ui.internal.Messages;

@SuppressWarnings( "restriction" )
public class EclipseInstallationPageTest
    extends AbstractWizardPageTest
{
    private static final String PROJECT_NAME = "Project";

    private WizardDialog dialog;

    private void assertPageComplete( WizardPage page )
    {
        Assert.assertTrue( "Page complete: " + page.isPageComplete(), page.isPageComplete() );
        Assert.assertTrue( "Finish button is disabled", wizard.canFinish() );
    }

    private void assertPageNotComplete( WizardPage page )
    {
        Assert.assertFalse( "Page complete: " + page.isPageComplete(), page.isPageComplete() );
        Assert.assertFalse( "Finish button is enabled", wizard.canFinish() );
    }

    @Test
    public void testNoLocations()
    {
        IS2Project project = new Project();
        project.setName( PROJECT_NAME );
        EclipseInstallationPage page = createTestPage( project );

        assertText( page, EclipseInstallationPage.INSTALLATION_LOCATION_TEXT_NAME,
                    page.createAvailablePath( IS2Project.DEFAULT_INSTALL_PATH ), true, true );
        assertButton( page, EclipseInstallationPage.INSTALLATION_LOCATION_BUTTON_NAME, true, true );
        assertText( page, EclipseInstallationPage.WORKSPACE_LOCATION_TEXT_NAME,
                    page.createAvailablePath( IS2Project.DEFAULT_WORKSPACE_PATH ), true, true );
        assertButton( page, EclipseInstallationPage.WORKSPACE_LOCATION_BUTTON_NAME, true, true );
        assertPageComplete( page );
    }

    @Test
    public void testNotCustomizableLocations()
    {
        EclipseInstallationLocation installationLocation = new EclipseInstallationLocation();
        installationLocation.setDirectory( "foo/eclipse" );
        installationLocation.setCustomizable( false );
        EclipseWorkspaceLocation workspaceLocation = new EclipseWorkspaceLocation();
        workspaceLocation.setDirectory( "foo/workspace" );
        workspaceLocation.setCustomizable( false );
        Project project = new Project();
        project.setName( PROJECT_NAME );
        project.setEclipseInstallationLocation( installationLocation );
        project.setEclipseWorkspaceLocation( workspaceLocation );
        EclipseInstallationPage page = createTestPage( project );

        assertText( page, EclipseInstallationPage.INSTALLATION_LOCATION_TEXT_NAME,
                    page.createAvailablePath( "foo/eclipse" ), false, true );
        assertButton( page, EclipseInstallationPage.INSTALLATION_LOCATION_BUTTON_NAME, false, true );
        assertText( page, EclipseInstallationPage.WORKSPACE_LOCATION_TEXT_NAME,
                    page.createAvailablePath( "foo/workspace" ), false, true );
        assertButton( page, EclipseInstallationPage.WORKSPACE_LOCATION_BUTTON_NAME, false, true );
        assertPageComplete( page );
    }

    @Test
    public void testDefaultInstallationLocation()
    {
        EclipseInstallationLocation location = new EclipseInstallationLocation();
        location.setDirectory( "foo" );
        location.setCustomizable( true );
        Project project = new Project();
        project.setEclipseInstallationLocation( location );
        project.setName( PROJECT_NAME );
        EclipseInstallationPage page = createTestPage( project );

        String foo = page.createAvailablePath( "foo" );

        assertText( page, EclipseInstallationPage.INSTALLATION_LOCATION_TEXT_NAME, foo, true, true );
        assertButton( page, EclipseInstallationPage.INSTALLATION_LOCATION_BUTTON_NAME, true, true );
        assertText( page, EclipseInstallationPage.WORKSPACE_LOCATION_TEXT_NAME,
                    page.createAvailablePath( IS2Project.DEFAULT_WORKSPACE_PATH ), true, true );
        assertButton( page, EclipseInstallationPage.WORKSPACE_LOCATION_BUTTON_NAME, true, true );
        assertPageComplete( page );

        location.setCustomizable( false );
        page = createTestPage( project );

        assertText( page, EclipseInstallationPage.INSTALLATION_LOCATION_TEXT_NAME, foo, false, true );
        assertButton( page, EclipseInstallationPage.INSTALLATION_LOCATION_BUTTON_NAME, false, true );
        assertText( page, EclipseInstallationPage.WORKSPACE_LOCATION_TEXT_NAME,
                    page.createAvailablePath( IS2Project.DEFAULT_WORKSPACE_PATH ), true, true );
        assertButton( page, EclipseInstallationPage.WORKSPACE_LOCATION_BUTTON_NAME, true, true );
        assertPageComplete( page );
    }

    @Test
    public void testDefaultWorkspaceLocation()
    {
        EclipseWorkspaceLocation location = new EclipseWorkspaceLocation();
        location.setDirectory( "foo" );
        location.setCustomizable( true );
        Project project = new Project();
        project.setEclipseWorkspaceLocation( location );
        project.setName( PROJECT_NAME );
        EclipseInstallationPage page = createTestPage( project );

        String foo = page.createAvailablePath( "foo" );

        assertText( page, EclipseInstallationPage.INSTALLATION_LOCATION_TEXT_NAME,
                    page.createAvailablePath( IS2Project.DEFAULT_INSTALL_PATH ), true, true );
        assertButton( page, EclipseInstallationPage.INSTALLATION_LOCATION_BUTTON_NAME, true, true );
        assertText( page, EclipseInstallationPage.WORKSPACE_LOCATION_TEXT_NAME, foo, true, true );
        assertButton( page, EclipseInstallationPage.WORKSPACE_LOCATION_BUTTON_NAME, true, true );
        assertPageComplete( page );

        location.setCustomizable( false );
        page = createTestPage( project );

        assertText( page, EclipseInstallationPage.INSTALLATION_LOCATION_TEXT_NAME,
                    page.createAvailablePath( IS2Project.DEFAULT_INSTALL_PATH ), true, true );
        assertButton( page, EclipseInstallationPage.INSTALLATION_LOCATION_BUTTON_NAME, true, true );
        assertText( page, EclipseInstallationPage.WORKSPACE_LOCATION_TEXT_NAME, foo, false, true );
        assertButton( page, EclipseInstallationPage.WORKSPACE_LOCATION_BUTTON_NAME, false, true );
        assertPageComplete( page );
    }

    @Test
    public void testAbsoluteLocations()
    {
        Project project = new Project();
        EclipseInstallationPage page = createTestPage( project );

        setText( page, EclipseInstallationPage.INSTALLATION_LOCATION_TEXT_NAME, "foo" );
        Assert.assertEquals( NLS.bind( Messages.installationWizard_errors_pathIsNotAbsolute,
                                       Messages.installationWizard_installationPage_installationDirectoryName ),
                             page.getMessage() );
        setText( page, EclipseInstallationPage.INSTALLATION_LOCATION_TEXT_NAME,
                 new File( "/foo" ).getAbsolutePath() );

        setText( page, EclipseInstallationPage.WORKSPACE_LOCATION_TEXT_NAME, "bar" );
        Assert.assertEquals( NLS.bind( Messages.installationWizard_errors_pathIsNotAbsolute,
                                       Messages.installationWizard_installationPage_workspaceLocationName ),
                             page.getMessage() );
        setText( page, EclipseInstallationPage.WORKSPACE_LOCATION_TEXT_NAME, new File( "/bar" ).getAbsolutePath() );

        Assert.assertNull( page.getMessage() );
        assertPageComplete( page );
    }

    @Test
    public void testEmptyWorkspaceLocation()
        throws Exception
    {
        Project project = new Project();
        EclipseInstallationPage page = createTestPage( project );

        // We don't want error about the Installation Location
        setText( page, EclipseInstallationPage.INSTALLATION_LOCATION_TEXT_NAME,
                 new File( "/foo" ).getAbsolutePath() );

        File tempDir = createTempDirectory();

        try
        {
            // Set workspace location to absolute path that does not exist
            tempDir.delete();
            setText( page, EclipseInstallationPage.WORKSPACE_LOCATION_TEXT_NAME, tempDir.getAbsolutePath() );
            Assert.assertNull( page.getMessage() );

            // Set workspace location to absolute path that exists and it's empty
            tempDir = createTempDirectory();
            setText( page, EclipseInstallationPage.WORKSPACE_LOCATION_TEXT_NAME, tempDir.getAbsolutePath() );
            Assert.assertNull( page.getMessage() );

            // Set workspace location to absolute path that exists and it's not empty
            tempDir.delete();
            tempDir = createTempDirectory();
            File tempFile = new File( tempDir, "test" );
            tempFile.createNewFile();
            try
            {
                setText( page, EclipseInstallationPage.WORKSPACE_LOCATION_TEXT_NAME, tempDir.getAbsolutePath() );
                Assert.assertEquals( "Workspace Location is not an empty directory", page.getMessage() );
                assertPageNotComplete( page );
            }
            finally
            {
                tempFile.delete();
            }
        }
        finally
        {
            if ( tempDir.exists() )
            {
                tempDir.delete();
            }
        }
    }

    private File createTempDirectory()
        throws IOException
    {
        File tempDir = File.createTempFile( "testEmptyWorkspaceLocation", "" );
        tempDir.delete();
        tempDir.mkdirs();
        return tempDir;
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

    private EclipseInstallationPage createTestPage( IS2Project project )
    {
        if ( wizard != null )
        {
            wizard.dispose();
        }
        wizard = new DummyWizard();
        EclipseInstallationPage page = new EclipseInstallationPage( project );
        wizard.addPage( page );
        assertPageNotComplete( page );

        Display display = Display.getCurrent();
        if ( display == null )
        {
            display = new Display();
        }
        Shell shell = new Shell( display );

        dialog = new WizardDialog( shell, wizard );
        dialog.create();
        // dialog.open();

        assertPageComplete( page );
        return page;
    }
}
