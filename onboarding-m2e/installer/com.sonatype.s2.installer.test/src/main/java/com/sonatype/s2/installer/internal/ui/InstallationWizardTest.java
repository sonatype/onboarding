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
package com.sonatype.s2.installer.internal.ui;

import java.io.File;

import junit.framework.TestCase;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.sonatype.s2.project.model.IMavenSettingsLocation;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.descriptor.MavenSettingsLocation;
import com.sonatype.s2.project.model.descriptor.Project;

public class InstallationWizardTest
    extends TestCase
{
    private InstallationWizard installationWizard;

    private WizardDialog dialog;

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            if ( installationWizard != null )
            {
                installationWizard.dispose();
            }
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testCodebaseRequiresMavenSettings()
    {
        // The codebase does not require maven settings
        IS2Project codebase = new Project();
        createInstallationWizard();
        installationWizard.setProject( codebase );

        // The codebase requires maven settings, but the codebase does not specify a maven settings url and there is no
        // default user maven settings file
        codebase.setRequiresMavenSettings( true );
        createInstallationWizard();
        try
        {
            installationWizard.setProject( codebase );
            fail( "Expected MissingMavenSettingsException" );
        }
        catch ( MissingMavenSettingsException expected )
        {
            assertMissingMavenSettingsException( expected, "The codebase requires maven settings" );
        }

        // The codebase requires maven settings, but the codebase does not specify a maven settings url and the default
        // user maven settings file does not exist
        codebase.setRequiresMavenSettings( true );
        createInstallationWizard();
        File defaultUserMavenSettingsFile = new File( "foo" );
        installationWizard.setDefaultUserMavenSettingsFile( defaultUserMavenSettingsFile );
        try
        {
            installationWizard.setProject( codebase );
            fail( "Expected MissingMavenSettingsException" );
        }
        catch ( MissingMavenSettingsException expected )
        {
            assertMissingMavenSettingsException( expected, "The codebase requires maven settings, but the "
                + defaultUserMavenSettingsFile.getAbsolutePath() + " file does not exist." );
        }

        // The codebase requires maven settings, but the codebase does not specify a maven settings url and the default
        // user maven settings file exists
        codebase.setRequiresMavenSettings( true );
        createInstallationWizard();
        defaultUserMavenSettingsFile = new File( "resources/foosettings.xml" );
        installationWizard.setDefaultUserMavenSettingsFile( defaultUserMavenSettingsFile );
        installationWizard.setProject( codebase );

        // The codebase requires maven settings, the codebase specifies a maven settings url and the default
        // user maven settings file does not exist
        codebase.setRequiresMavenSettings( true );
        IMavenSettingsLocation mavenSettingsLocation = new MavenSettingsLocation();
        mavenSettingsLocation.setUrl( "http://foo/settings.xml" );
        codebase.setMavenSettingsLocation( mavenSettingsLocation );
        createInstallationWizard();
        defaultUserMavenSettingsFile = new File( "foo" );
        installationWizard.setDefaultUserMavenSettingsFile( defaultUserMavenSettingsFile );
        installationWizard.setProject( codebase );
    }

    private void assertMissingMavenSettingsException( MissingMavenSettingsException exception, String errorMessage )
    {
        if ( !errorMessage.equals( exception.getMessage() ) )
        {
            throw exception;
        }
    }

    private void createInstallationWizard()
    {
        Display display = Display.getCurrent();
        if ( display == null )
        {
            display = Display.getDefault();
        }
        Shell shell = new Shell( display );

        if ( installationWizard != null )
        {
            installationWizard.dispose();
        }
        installationWizard = new InstallationWizard();
        dialog = new WizardDialog( shell, installationWizard );
        dialog.create();
        // installationWizard.setDefaultUserMavenSettingsFile( MavenCli.DEFAULT_USER_SETTINGS_FILE );
    }
}
