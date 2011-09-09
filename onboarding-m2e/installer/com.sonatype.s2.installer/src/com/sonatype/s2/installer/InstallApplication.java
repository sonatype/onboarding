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
package com.sonatype.s2.installer;

import java.io.File;
import java.net.URL;

import javax.crypto.spec.PBEKeySpec;

import org.apache.maven.cli.MavenCli;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.m2e.logback.configuration.LogHelper;
import org.eclipse.m2e.logback.configuration.LogPlugin;
import org.eclipse.swt.widgets.Display;
import org.maven.ide.eclipse.authentication.internal.storage.PasswordProvider;
import org.maven.ide.eclipse.authentication.internal.storage.PasswordProviderDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.installer.internal.Activator;
import com.sonatype.s2.installer.internal.S2InstallerErrorHandler;
import com.sonatype.s2.installer.internal.ui.InstallationWizard;
import com.sonatype.s2.nexus.NexusFacade;
import com.sonatype.s2.project.model.IS2ProjectCatalog;

public class InstallApplication
    implements IApplication
{
    private static final String LOG_FILE_PROP_NAME = "com.sonatype.mse.installer.log.file";

    private static final String LOG_FILE_DEFAULT = "${user.home}/.mse/installer/mse-installer.log";

    private static final String INSTALL_DESCRIPTOR_URL_PROPNAME = "com.sonatype.mse.installer.installDescriptorURL";

    private Logger log;

    private String logFile;

    private Throwable error = null;

    public Object start( IApplicationContext appContext )
        throws Exception
    {
        appContext.applicationRunning();

        Display display = Display.getCurrent();
        if ( display == null )
        {
            display = new Display();
        }

        logFile = configureLogback();
        log = LoggerFactory.getLogger( InstallApplication.class );
        LogHelper.logJavaProperties( log );

        S2InstallerErrorHandler.initialize( log, logFile );

        display.syncExec( new Runnable()
        {
            public void run()
            {
                doRun();
            }
        } );

        if ( error != null )
        {
            log.error( error.getMessage(), error );

            IStatus status;
            if ( error instanceof CoreException )
            {
                status = ( (CoreException) error ).getStatus();
            }
            else
            {
                status = new Status( IStatus.ERROR, Activator.PLUGIN_ID, error.getMessage(), error );
            }
            S2InstallerErrorHandler.handleError( null /* title */, status );
        }

        // TODO return some real exit code
        return null;
    }

    private String configureLogback()
        throws Exception
    {
        String logFile = System.getProperty( LOG_FILE_PROP_NAME );
        System.out.println( LOG_FILE_PROP_NAME + "=" + logFile );
        if ( logFile == null || logFile.trim().length() == 0 )
        {
            logFile = LOG_FILE_DEFAULT;
            System.setProperty( LOG_FILE_PROP_NAME, logFile );
            System.out.println( "Set " + LOG_FILE_PROP_NAME + "=" + logFile );
        }

        URL logCfg = null;

        String userhome = System.getProperty( "user.home" );
        if ( userhome != null )
        {
            File configFile = new File( userhome, ".mse/installer-logback.xml" );
            if ( configFile.canRead() )
            {
                logCfg = configFile.toURL();
            }

            logFile = logFile.replace( "${user.home}", userhome );
        }

        if ( logCfg == null )
        {
            logCfg = InstallApplication.class.getClassLoader().getResource( "installer-logback.xml" );
        }

        LogPlugin.loadConfiguration( logCfg );

        return logFile;
    }

    protected void doRun()
    {
        final String installDescriptorUrl = System.getProperty( INSTALL_DESCRIPTOR_URL_PROPNAME );

        if ( installDescriptorUrl == null )
        {
            error =
                new RuntimeException( "An installation descriptor URL must be specified by setting the "
                    + INSTALL_DESCRIPTOR_URL_PROPNAME + " java property" );
            return;
        }
        if ( !installDescriptorUrl.endsWith( ".xml" ) )
        {
            error = new RuntimeException( "Invalid installation descriptor URL: " + installDescriptorUrl );
            return;
        }

        String nexusUrl = System.getProperty( NexusFacade.NEXUS_URL_PROPNAME );
        if ( nexusUrl == null )
        {
            error =
                new RuntimeException( "The Nexus server URL must be specified by setting the "
                    + NexusFacade.NEXUS_URL_PROPNAME
                    + " java property" );
            return;
        }

        String catalogUrl = null;

        String projectDescriptorUrl = null;

        if ( installDescriptorUrl.endsWith( IS2ProjectCatalog.CATALOG_FILENAME ) )
        {
            catalogUrl =
                installDescriptorUrl.substring( 0, installDescriptorUrl.length()
                    - IS2ProjectCatalog.CATALOG_FILENAME.length() - 1 );
        }
        else
        {
            projectDescriptorUrl = installDescriptorUrl;
        }

        final InstallationWizard wizard = new InstallationWizard();
        wizard.setNexusUrl( nexusUrl );
        wizard.setCatalogUrl( catalogUrl );
        wizard.setProjectUrl( projectDescriptorUrl );
        wizard.setDefaultUserMavenSettingsFile( MavenCli.DEFAULT_USER_SETTINGS_FILE );

        WizardDialog dialog = new WizardDialog( null, wizard );

        PasswordProvider.setDelegate( new PasswordProviderDelegate()
        {
            @Override
            public PBEKeySpec getPassword( IPreferencesContainer container, int passwordType )
            {
                boolean newPassword = ( ( passwordType & PasswordProvider.CREATE_NEW_PASSWORD ) != 0 );
                boolean passwordChange = ( ( passwordType & PasswordProvider.PASSWORD_CHANGE ) != 0 );

                return newPassword( wizard.getSecureStorePassword( newPassword, passwordChange ) );
            }
        } );

        try
        {
            dialog.open();
            if ( error == null && wizard.getError() != null )
            {
                error = wizard.getError();
            }
        }
        finally
        {
            PasswordProvider.setDelegate( null );
        }
    }

    public void stop()
    {
    }
}
