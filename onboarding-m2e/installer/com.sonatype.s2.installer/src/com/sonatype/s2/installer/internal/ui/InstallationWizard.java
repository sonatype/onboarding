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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.SecurityRealmPersistenceException;
import org.maven.ide.eclipse.ui.common.dialogs.SecureStorageLoginDialog;

import com.sonatype.s2.installer.internal.InstallAndLaunchOperation;
import com.sonatype.s2.installer.internal.S2InstallerErrorHandler;
import com.sonatype.s2.nexus.NexusFacade;
import com.sonatype.s2.project.model.IP2LineupLocation;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.internal.Dialog;
import com.sonatype.s2.project.ui.internal.Messages;
import com.sonatype.s2.project.ui.internal.composites.NexusUrlComposite;
import com.sonatype.s2.project.ui.internal.wizards.AbstractMaterializationWizard;
import com.sonatype.s2.project.ui.internal.wizards.EclipseInstallationPage;
import com.sonatype.s2.project.ui.internal.wizards.NexusUrlPage;
import com.sonatype.s2.project.ui.internal.wizards.ProjectLoaderPage;
import com.sonatype.s2.project.ui.internal.wizards.ProjectSelectorPage;
import com.sonatype.s2.project.ui.internal.wizards.ProjectUrlPage;
import com.sonatype.s2.project.validation.api.S2ProjectValidationContext;

public class InstallationWizard
    extends AbstractMaterializationWizard
{
    private ProjectLoaderPage projectLoaderPage;

    private ProjectUrlPage projectUrlPage;

    protected ProjectSelectorPage projectSelectorPage;

    protected EclipseInstallationPage eclipseInstallationPage;

    private String nexusUrl;

    private String projectUrl;

    private String catalogUrl;

    private File defaultUserMavenSettingsFile;

    public InstallationWizard()
    {
        S2ProjectValidationContext validationContext = new S2ProjectValidationContext();
        validationContext.setProperty( S2ProjectValidationContext.FRESH_INSTALL_PROPNAME, "true" ); //$NON-NLS-1$
        setValidationContext( validationContext );

        setWindowTitle( Messages.installationWizard_title );
    }

    public void setProjectUrl( String url )
    {
        projectUrl = url;
    }

    private void validateRequiresMavenSettings( IS2Project project )
    {
        if ( !project.isRequiresMavenSettings() )
        {
            return;
        }

        String mavenSettingsUrl = null;
        if ( project.getMavenSettingsLocation() != null )
        {
            mavenSettingsUrl = project.getMavenSettingsLocation().getUrl();
            if ( mavenSettingsUrl != null && mavenSettingsUrl.trim().length() == 0 )
            {
                mavenSettingsUrl = null;
            }
        }
        if ( mavenSettingsUrl == null
            && ( defaultUserMavenSettingsFile == null || !defaultUserMavenSettingsFile.exists() ) )
        {
            throw new MissingMavenSettingsException( defaultUserMavenSettingsFile );
        }
    }

    @Override
    public void setProject( IS2Project project )
    {
        validateRequiresMavenSettings( project );

        super.setProject( project );
    }

    public void setCatalogUrl( String url )
    {
        catalogUrl = url;
    }

    @Override
    public boolean performFinish()
    {
        final String installationDirectory = eclipseInstallationPage.getInstallationDirectory();
        final String workspaceLocation = eclipseInstallationPage.getWorkspaceLocation();
        final boolean sharedInstall = eclipseInstallationPage.isInstallShared();
        final String pmdUrl = this.projectDescriptorUrl;
        final IS2Project project = this.project;

        IP2LineupLocation lineupLocation = project.getP2LineupLocation();

        IRunnableWithProgress op =
            new InstallAndLaunchOperation( nexusUrl, lineupLocation.getUrl(), pmdUrl, workspaceLocation,
                                           installationDirectory, sharedInstall );

        try
        {
            op = new SWTRunnableWithProgressWrapper( getContainer().getShell().getDisplay(), op );
            getContainer().run( true, true, op );
        }
        catch ( Exception e )
        {
            S2InstallerErrorHandler.handleError( Messages.installationWizard_couldNotCreateEclipseInstallation, e );

            return false;
        }

        return true;
    }

    @Override
    public void addPages()
    {
        // secureStoragePasswordPage = new SecureStoreLoginPage();
        // addPage( secureStoragePasswordPage );
        boolean canAccessNexusUrl = false;
        IAuthData nexusAuthData = AuthFacade.getAuthService().select( nexusUrl );
        try
        {
            if ( nexusAuthData != null )
            {
                IStatus status =
                    NexusFacade.validateCredentials( nexusUrl, nexusAuthData.getUsername(),
                                                     nexusAuthData.getPassword(),
                                                     nexusAuthData.getAnonymousAccessType(), new NullProgressMonitor() );
                if ( status.isOK() )
                {
                    NexusFacade.setMainNexusServerData( nexusUrl, nexusAuthData.getUsername(),
                                                        nexusAuthData.getPassword(), new NullProgressMonitor() );
                    canAccessNexusUrl = true;
                }
            }
        }
        catch ( SecurityRealmPersistenceException e )
        {
            canAccessNexusUrl = false;
        }
        catch ( Exception e )
        {
            S2InstallerErrorHandler.handleError( NLS.bind( Messages.installationWizard_errorAccessingNexusUrl, nexusUrl ),
                                                 e );
            throw new RuntimeException( e );
        }

        if ( !canAccessNexusUrl )
        {
            NexusUrlPage nexusPage =
                new NexusUrlPage( nexusUrl, NexusUrlComposite.READ_ONLY_URL | NexusUrlComposite.ALLOW_ANONYMOUS );
            nexusPage.setTitle( Messages.installationWizard_nexusPage_title );
            nexusPage.setDescription( Messages.installationWizard_nexusPage_description );
            addPage( nexusPage );
        }

        if ( projectUrl != null )
        {
            projectLoaderPage = new ProjectLoaderPage( projectUrl );
            addPage( projectLoaderPage );
        }
        else if ( catalogUrl != null )
        {
            projectSelectorPage = new ProjectSelectorPage( catalogUrl, new ProjectFilter() );
            addPage( projectSelectorPage );
        }
        else
        {
            projectUrlPage = new ProjectUrlPage();
            addPage( projectUrlPage );
        }
    }

    @Override
    protected void addMaterializationPages()
    {
        super.addMaterializationPages();

        eclipseInstallationPage = new EclipseInstallationPage( project );
        addPage( eclipseInstallationPage );

        if ( projectLoaderPage != null )
        {
            getContainer().showPage( projectLoaderPage.getNextPage() );
        }
    }

    @Override
    public boolean canFinish()
    {
        return project != null && super.canFinish();
    }

    public String getSecureStorePassword( boolean newPassword, boolean passwordChange )
    {
        final SecureStorageLoginDialog dialog = new SecureStorageLoginDialog( getShell(), newPassword, passwordChange );

        final String[] password = new String[1];

        getShell().getDisplay().syncExec( new Runnable()
        {
            public void run()
            {
                if ( dialog.open() == Dialog.OK )
                {
                    password[0] = dialog.getPassword();
                }
            }
        } );

        return password[0];
    }

    @Override
    public IWizardPage getNextPage( IWizardPage page )
    {
        IWizardPage nextPage = super.getNextPage( page );
        if ( nextPage != null && nextPage == projectValidationPage )
        {
            IStatus validationStatus = projectValidationPage.validate();

            if ( validationStatus != null && validationStatus.isOK() )
            {
                projectValidationPage.setPageComplete( true );
                return super.getNextPage( nextPage );
            }
        }
        return nextPage;
    }

    public void setNexusUrl( String nexusUrl )
    {
        this.nexusUrl = nexusUrl;
    }

    public void setDefaultUserMavenSettingsFile( File defaultUserMavenSettingsFile )
    {
        this.defaultUserMavenSettingsFile = defaultUserMavenSettingsFile;
    }
}
