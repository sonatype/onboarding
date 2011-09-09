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

import javax.swing.JFileChooser;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.swtvalidation.SwtComponentDecorationFactory;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ValidatorUtils;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;

import com.sonatype.s2.project.model.IEclipseInstallationLocation;
import com.sonatype.s2.project.model.IEclipseWorkspaceLocation;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.S2ProjectFacade;
import com.sonatype.s2.project.ui.internal.Messages;

public class EclipseInstallationPage
    extends WizardPage
{
    private Text installationDirectoryText;

    private Button standaloneRadio;

    private Button sharedRadio;

    private Text workspaceLocationText;

    private final IS2Project project;

    private SwtValidationGroup vg;

    private static Validator<String> REQUIRE_ABSOLUTE_PATH = new AbsolutePathName();

    private static Validator<String> REQUIRE_EMPTY_DIRECTORY = new EmptyDirectory();

    public EclipseInstallationPage( IS2Project project )
    {
        super( Messages.installationWizard_installationPage_title );
        this.project = project;
        setTitle( Messages.installationWizard_installationPage_title );
        setDescription( Messages.installationWizard_installationPage_description );
        setPageComplete( false );
    }

    public static final String INSTALLATION_LOCATION_TEXT_NAME = "installationDirectoryText";

    public static final String INSTALLATION_LOCATION_BUTTON_NAME = "installationDirectoryButton";

    public static final String WORKSPACE_LOCATION_TEXT_NAME = "workspaceLocationText";

    public static final String WORKSPACE_LOCATION_BUTTON_NAME = "workspaceLocationButton";

    @SuppressWarnings( "unchecked" )
    public void createControl( Composite parent )
    {
        Composite container = new Composite( parent, SWT.NULL );
        container.setLayout( new GridLayout( 3, false ) );

        Label installationDirectoryLabel = new Label( container, SWT.NONE );
        installationDirectoryLabel.setText( Messages.installationWizard_installationPage_installationDirectory );

        installationDirectoryText = new Text( container, SWT.BORDER );
        GridData data = new GridData( SWT.FILL, SWT.CENTER, true, false );
        data.horizontalIndent = 4;
        installationDirectoryText.setLayoutData( data );
        installationDirectoryText.setData( "name", INSTALLATION_LOCATION_TEXT_NAME );

        Button browseInstallationDirectoryButton = new Button( container, SWT.PUSH );
        browseInstallationDirectoryButton.setData( "name", INSTALLATION_LOCATION_BUTTON_NAME );
        browseInstallationDirectoryButton.setText( Messages.actions_browse_title );
        addListener( browseInstallationDirectoryButton, installationDirectoryText );

        IEclipseInstallationLocation eclipseInstallationLocation = project.getEclipseInstallationLocation();
        if ( eclipseInstallationLocation != null )
        {
            installationDirectoryText.setText( eclipseInstallationLocation.getDirectory() );
            installationDirectoryText.setEditable( eclipseInstallationLocation.isCustomizable() );
            browseInstallationDirectoryButton.setEnabled( eclipseInstallationLocation.isCustomizable() );
        }

        Label installTypeLabel = new Label( container, SWT.NONE );
        installTypeLabel.setText( Messages.installationWizard_installationPage_installType );
        installTypeLabel.setLayoutData( new GridData( SWT.LEFT, SWT.TOP, false, false, 1, 2 ) );

        standaloneRadio = new Button( container, SWT.RADIO );
        standaloneRadio.setText( Messages.installationWizard_installationPage_standalone );
        standaloneRadio.setLayoutData( new GridData( SWT.LEFT, SWT.TOP, false, false, 2, 1 ) );
        standaloneRadio.setSelection( true );

        sharedRadio = new Button( container, SWT.RADIO );
        sharedRadio.setText( Messages.installationWizard_installationPage_shared );
        sharedRadio.setLayoutData( new GridData( SWT.LEFT, SWT.TOP, false, false, 2, 1 ) );

        Label workspaceLocationLabel = new Label( container, SWT.NONE );
        workspaceLocationLabel.setText( Messages.installationWizard_installationPage_workspaceLocation );

        workspaceLocationText = new Text( container, SWT.BORDER );

        data = new GridData( SWT.FILL, SWT.CENTER, true, false );
        data.horizontalIndent = 4;
        workspaceLocationText.setLayoutData( data );
        workspaceLocationText.setData( "name", WORKSPACE_LOCATION_TEXT_NAME );

        Button browseWorkspaceLocationButton = new Button( container, SWT.PUSH );
        browseWorkspaceLocationButton.setData( "name", WORKSPACE_LOCATION_BUTTON_NAME );
        browseWorkspaceLocationButton.setText( Messages.actions_browse_title );
        addListener( browseWorkspaceLocationButton, workspaceLocationText );

        IEclipseWorkspaceLocation eclipseWorkspaceLocation = project.getEclipseWorkspaceLocation();
        if ( eclipseWorkspaceLocation != null )
        {
            workspaceLocationText.setText( eclipseWorkspaceLocation.getDirectory() );
            workspaceLocationText.setEditable( eclipseWorkspaceLocation.isCustomizable() );
            browseWorkspaceLocationButton.setEnabled( eclipseWorkspaceLocation.isCustomizable() );
        }

        // validate fields
        vg =
            SwtValidationGroup.create( null, SwtComponentDecorationFactory.createLazyFactory(),
                                       SwtValidationUI.createWizardPageValidationUI( this ) );
        SwtValidationGroup.setComponentName( workspaceLocationText,
                                             Messages.installationWizard_installationPage_workspaceLocationName );
        SwtValidationGroup.setComponentName( installationDirectoryText,
                                             Messages.installationWizard_installationPage_installationDirectoryName );

        vg.add( workspaceLocationText,
                ValidatorUtils.merge( StringValidators.REQUIRE_NON_EMPTY_STRING, REQUIRE_ABSOLUTE_PATH,
                                      REQUIRE_EMPTY_DIRECTORY,
                                      new WorkspaceRelativeToInstall( installationDirectoryText ) ) );
        vg.add( installationDirectoryText,
                ValidatorUtils.merge( StringValidators.REQUIRE_NON_EMPTY_STRING, REQUIRE_ABSOLUTE_PATH ) );

        setControl( container );
        IEclipseInstallationLocation location = project.getEclipseInstallationLocation();
        String locationPath = null;
        if ( location != null )
        {
            locationPath = location.getDirectory();
        }
        if ( locationPath == null )
        {
            locationPath = IS2Project.DEFAULT_INSTALL_PATH;
        }
        installationDirectoryText.setText( createAvailablePath( locationPath ) );
        IEclipseWorkspaceLocation workspace = project.getEclipseWorkspaceLocation();
        String workspacePath = null;
        if ( workspace != null )
        {
            workspacePath = workspace.getDirectory();
        }
        if ( workspacePath == null )
        {
            workspacePath = IS2Project.DEFAULT_WORKSPACE_PATH;
        }
        workspaceLocationText.setText( createAvailablePath( workspacePath ) );
    }

    public String createAvailablePath( String path )
    {
        path = S2ProjectFacade.replaceVariables( path, project );

        File result = new File( path );
        int count = 1;
        while ( result.exists() )
        {
            result = new File( path + "_" + count );
            count++;
        }
        return result.getAbsolutePath();
    }

    public String getInstallationDirectory()
    {
        return installationDirectoryText.getText();
    }

    public String getWorkspaceLocation()
    {
        return workspaceLocationText.getText();
    }

    public boolean isInstallShared()
    {
        return sharedRadio.getSelection();
    }

    static class WorkspaceRelativeToInstall
        implements Validator<String>
    {
        private Text txtInstall;

        public WorkspaceRelativeToInstall( Text txtInstall )
        {
            this.txtInstall = txtInstall;
        }

        public void validate( Problems problems, String compName, String model )
        {
            if ( model.trim().length() == 0 )
            {
                return;
            }
            File file = new File( model.trim() );
            if ( file.isAbsolute() )
            {
                File install = new File( txtInstall.getText().trim() );
                String workspacePath = file.getAbsolutePath();
                String installPath = install.getAbsolutePath();
                if ( workspacePath.equals( installPath ) )
                {
                    problems.add( NLS.bind( Messages.installationWizard_errors_workspacePathSameAsEclipsePath, compName ),
                                  Severity.FATAL );
                }
                else
                {
                    if ( isParent( install, file ) )
                    {
                        problems.add( NLS.bind( Messages.installationWizard_errors_workspacePathNestedInEclipsePath,
                                                compName ), Severity.WARNING );
                    }
                }
            }
        }

        private boolean isParent( File install, File file )
        {
            if ( install.equals( file ) )
            {
                return true;
            }
            if ( file.getParentFile() != null )
            {
                return isParent( install, file.getParentFile() );
            }
            return false;
        }

        public Class<String> modelType()
        {
            return String.class;
        }
    }

    // Eventually move this class to a shared place..
    private static class AbsolutePathName
        implements Validator<String>
    {
        public void validate( Problems problems, String compName, String model )
        {
            if ( model.trim().length() == 0 )
            {
                return;
            }
            File file = new File( model.trim() );
            if ( !file.isAbsolute() )
            {
                problems.add( NLS.bind( Messages.installationWizard_errors_pathIsNotAbsolute, compName ),
                              Severity.FATAL );
            }
        }

        public Class<String> modelType()
        {
            return String.class;
        }
    }

    private static class EmptyDirectory
        implements Validator<String>
    {
        public void validate( Problems problems, String compName, String model )
        {
            if ( model.trim().length() == 0 )
            {
                return;
            }
            File file = new File( model.trim() );
            if ( !file.exists() )
            {
                return;
            }
            if ( !file.isDirectory() )
            {
                problems.add( NLS.bind( Messages.installationWizard_errors_pathIsNotADirectory, compName ),
                              Severity.FATAL );
                return;
            }

            String[] directoryContent = file.list();
            if ( directoryContent != null && directoryContent.length > 0 )
            {
                problems.add( NLS.bind( Messages.installationWizard_errors_pathIsNotAnEmptyDirectory, compName ),
                              Severity.FATAL );
            }
        }

        public Class<String> modelType()
        {
            return String.class;
        }
    }

    private void addListener( Button button, final Text text )
    {
        button.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent event )
            {
                String path = null;
                if ( System.getProperty( "osgi.os" ).contains( "linux" ) )
                {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );

                    if ( JFileChooser.APPROVE_OPTION == chooser.showOpenDialog( SWT_AWT.getFrame( getShell() ) ) )
                        path = chooser.getSelectedFile().toString();
                }
                else
                {
                    DirectoryDialog dialog = new DirectoryDialog( getShell() );
                    path = dialog.open();
                }
                if ( path != null )
                {
                    text.setText( path );
                }
            }
        } );
    }

    @Override
    public void setVisible( boolean visible )
    {
        super.setVisible( visible );
        //MECLIPSE-1482 if all other panels are complete, we need to keep this one not complete
        //especially when other panels do work on Next> button presses. Then the last panel safeguards that next 
        //can be pressed but not finish.
        if (!visible) {
            setPageComplete( false );
        } else {
            setPageComplete( true );
            vg.performValidation();
        }
    }
    
}
