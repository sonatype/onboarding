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
package com.sonatype.s2.project.ui.codebase.composites;

import static org.maven.ide.eclipse.ui.common.FormUtils.nvl;

import java.util.Collection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.authentication.IRealmChangeListener;
import org.maven.ide.eclipse.ui.common.authentication.RealmComposite;
import org.maven.ide.eclipse.ui.common.authentication.RealmUrlCollector;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.maven.ide.eclipse.ui.common.validation.SonatypeValidators;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;

import com.sonatype.s2.project.model.IEclipsePreferencesLocation;
import com.sonatype.s2.project.model.IEclipseWorkspaceLocation;
import com.sonatype.s2.project.model.IMavenSettingsLocation;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.descriptor.EclipsePreferencesLocation;
import com.sonatype.s2.project.model.descriptor.EclipseWorkspaceLocation;
import com.sonatype.s2.project.model.descriptor.MavenSettingsLocation;
import com.sonatype.s2.project.prefs.PreferenceGroup;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.codebase.dialogs.EclipsePreferencesDialog;
import com.sonatype.s2.project.ui.codebase.wizard.SelectMavenSettingsWizard;
import com.sonatype.s2.project.ui.internal.Dialog;

@SuppressWarnings( "restriction" )
abstract public class EclipseWorkspaceComposite
    extends CodebaseComposite
{
    private static final String SERVICE_LOCAL_TEMPLATES_SETTINGS = "/service/local/templates/settings"; //$NON-NLS-1$

    private Text workspacePathText;

    private Button workspacePathCheckbox;

    private Text mavenSettingsLocationText;

    private RealmComposite mavenSettingsLocationRealmComposite;

    private Button mavenSettingsRequiredCheckbox;

    private Text eclipsePreferencesLocationText;

    private RealmComposite eclipsePreferencesLocationRealmComposite;

    public EclipseWorkspaceComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                                      FormToolkit toolkit )
    {
        super( parent, widthGroup, validationGroup, toolkit );

        setLayout( new GridLayout( 3, false ) );

        createWorkspacePathControls();
        createMavenSettingsControls();
        createEclipsePreferencesControls();
    }

    private void createWorkspacePathControls()
    {
        createLabel( Messages.eclipseWorkspaceComposite_path_label );

        workspacePathText = createText( SWT.NONE, 2, 1, "workspacePathText", null ); //$NON-NLS-1$
        workspacePathText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                saveEclipseWorkspacePath();
            }
        } );

        workspacePathCheckbox =
            createCheckbox( Messages.eclipseWorkspaceComposite_path_checkbox, 3, 1, "workspacePathCheckbox" ); //$NON-NLS-1$ //$NON-NLS-2$
        workspacePathCheckbox.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                saveEclipseWorkspacePath();
            }
        } );
        workspacePathCheckbox.setEnabled( false );
        workspacePathCheckbox.setSelection( true );
    }

    private void saveEclipseWorkspacePath()
    {
        String path = workspacePathText.getText().trim();
        if ( path.length() == 0 )
        {
            workspacePathCheckbox.setEnabled( false );
            getProject().setEclipseWorkspaceLocation( null );
        }
        else
        {
            workspacePathCheckbox.setEnabled( true );
            EclipseWorkspaceLocation location = new EclipseWorkspaceLocation();
            location.setDirectory( path );
            location.setCustomizable( workspacePathCheckbox.getSelection() );
            getProject().setEclipseWorkspaceLocation( location );
        }
        notifyCodebaseChangeListeners();
    }

    private void createMavenSettingsControls()
    {
        createLabel( Messages.eclipseWorkspaceComposite_mavenSettings_label );

        mavenSettingsLocationText =
            createText( "mavenSettingsLocationText", Messages.eclipseWorkspaceComposite_mavenSettings_name ); //$NON-NLS-1$
        mavenSettingsLocationText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                saveMavenSettingsLocation();
            }
        } );

        Button button = createButton( Messages.eclipseWorkspaceComposite_mavenSettings_select );
        button.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                String currentValue = mavenSettingsLocationText.getText();
                String currentTemplate;
                if ( currentValue != null && currentValue.contains( SERVICE_LOCAL_TEMPLATES_SETTINGS ) )
                {
                	int index = currentValue.indexOf( SERVICE_LOCAL_TEMPLATES_SETTINGS );
                    currentTemplate = currentValue.substring(index + SERVICE_LOCAL_TEMPLATES_SETTINGS.length() + 1);
                    if ( currentTemplate.contains("/content") )
                    {
                    	currentTemplate = currentTemplate.substring(0, currentTemplate.indexOf("/content"));
                    }
                    currentValue = currentValue.substring( 0,  index);
                }
                else
                {
                    currentValue = null;
                    currentTemplate = null;
                }

                SelectMavenSettingsWizard wizard = new SelectMavenSettingsWizard( currentValue, currentTemplate );
                WizardDialog dialog = new WizardDialog( getShell(), wizard ) {
					@Override
					protected void createButtonsForButtonBar(Composite parent) {
						super.createButtonsForButtonBar(parent);
						Button btn = getButton(IDialogConstants.FINISH_ID);
						btn.setText(IDialogConstants.OK_LABEL);
					}
                };
                	
                if ( dialog.open() == Dialog.OK )
                {
                    mavenSettingsLocationText.setText( wizard.getMavenSettingsUrl() );
                    saveMavenSettingsLocation();
                }
            }
        } );

        Label realmLabel = createLabel( Messages.eclipseWorkspaceComposite_securityRealm );
        GridData realmLabelData = (GridData) realmLabel.getLayoutData();
        realmLabelData.horizontalIndent = INPUT_INDENT;

        mavenSettingsLocationRealmComposite =
            new RealmComposite( this, mavenSettingsLocationText, getValidationGroup(), getToolkit() );
        mavenSettingsLocationRealmComposite.setLayoutData( createInputData( 2, 1 ) );
        mavenSettingsLocationRealmComposite.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                notifyCodebaseChangeListeners();
            }
        } );

        mavenSettingsRequiredCheckbox =
            createCheckbox( Messages.eclipseWorkspaceComposite_mavenSettingsRequired, 3, 1,
                            "mavenSettingsRequiredCheckbox" ); //$NON-NLS-2$
        GridData checkboxData = (GridData) mavenSettingsRequiredCheckbox.getLayoutData();
        checkboxData.horizontalIndent = INPUT_INDENT;

        mavenSettingsRequiredCheckbox.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                getProject().setRequiresMavenSettings( mavenSettingsRequiredCheckbox.getSelection() );
                getValidationGroup().performValidation();
                notifyCodebaseChangeListeners();
            }
        } );

        addToValidationGroup( mavenSettingsLocationText, new Validator<String>()
        {
            public void validate( Problems problems, String componentName, String value )
            {
                if ( mavenSettingsRequiredCheckbox.getSelection() && value.length() == 0 )
                {
                    problems.add( Messages.eclipseWorkspaceComposite_mavenSettingsRequired, Severity.WARNING );
                }
                SonatypeValidators.EMPTY_OR_URL.validate( problems, componentName, value );
            }

            public Class<String> modelType()
            {
                return String.class;
            }
        } );
    }

    private void saveMavenSettingsLocation()
    {
        String url = mavenSettingsLocationText.getText().trim();
        if ( url.length() == 0 )
        {
            getProject().setMavenSettingsLocation( null );
        }
        else
        {
            MavenSettingsLocation location = new MavenSettingsLocation();
            location.setUrl( url );
            getProject().setMavenSettingsLocation( location );
        }

        notifyCodebaseChangeListeners();
    }

    private void createEclipsePreferencesControls()
    {
        createLabel( Messages.eclipseWorkspaceComposite_eclipsePreferences_label );

        eclipsePreferencesLocationText =
            createText( "eclipsePreferencesLocationText", Messages.eclipseWorkspaceComposite_eclipsePreferences_name ); //$NON-NLS-1$
        eclipsePreferencesLocationText.setEditable( false );
        eclipsePreferencesLocationText.setEnabled( false );

        Button button = createButton( Messages.eclipseWorkspaceComposite_eclipsePreferences_select );
        button.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                String url = ""; //$NON-NLS-1$
                if ( getProject().getEclipsePreferencesLocation() != null )
                {
                    url = getProject().getEclipsePreferencesLocation().getUrl();
                }

                EclipsePreferencesDialog d =
                    new EclipsePreferencesDialog( getShell(),
                                                  IS2Project.PROJECT_PREFERENCES_FILENAME.equals( url ) ? null : url,
                                                  getEclipsePreferenceGroups() );
                if ( d.open() == Dialog.OK )
                {
                    if ( d.isUrlSelected() )
                    {
                        eclipsePreferencesLocationText.setText( d.getUrl() );
                        saveEclipsePreferenceGroups( null );
                    }
                    else
                    {
                        eclipsePreferencesLocationText.setText( IS2Project.PROJECT_PREFERENCES_FILENAME );
                        saveEclipsePreferenceGroups( d.getGroups() );
                        eclipsePreferencesLocationRealmComposite.setEnabled( false );
                        getValidationGroup().performValidation();
                    }
                    saveEclipsePreferencesLocation();
                }
            }
        } );

        Label realmLabel = createLabel( Messages.eclipseWorkspaceComposite_securityRealm );
        GridData realmLabelData = (GridData) realmLabel.getLayoutData();
        realmLabelData.horizontalIndent = INPUT_INDENT;

        eclipsePreferencesLocationRealmComposite =
            new RealmComposite( this, eclipsePreferencesLocationText, getValidationGroup(), getToolkit() );
        eclipsePreferencesLocationRealmComposite.setLayoutData( createInputData( 2, 1 ) );
        eclipsePreferencesLocationRealmComposite.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                notifyCodebaseChangeListeners();
            }
        } );
    }

    private void saveEclipsePreferencesLocation()
    {
        String url = eclipsePreferencesLocationText.getText().trim();
        if ( url.length() == 0 )
        {
            getProject().setEclipsePreferencesLocation( null );
        }
        else
        {
            EclipsePreferencesLocation location = new EclipsePreferencesLocation();
            location.setUrl( url );
            getProject().setEclipsePreferencesLocation( location );
        }

        notifyCodebaseChangeListeners();
    }

    @Override
    protected void update( IS2Project project )
    {
        IEclipseWorkspaceLocation workspaceLocation = project.getEclipseWorkspaceLocation();
        if ( workspaceLocation == null )
        {
            workspacePathText.setText( "" ); //$NON-NLS-1$
            workspacePathCheckbox.setSelection( true );
            workspacePathCheckbox.setEnabled( false );
        }
        else
        {
            workspacePathText.setText( workspaceLocation.getDirectory() );
            workspacePathCheckbox.setSelection( workspaceLocation.isCustomizable() );
            workspacePathCheckbox.setEnabled( true );
        }

        mavenSettingsRequiredCheckbox.setSelection( project.isRequiresMavenSettings() );

        IMavenSettingsLocation mavenSettingsLocation = project.getMavenSettingsLocation();
        mavenSettingsLocationText.setText( mavenSettingsLocation == null ? "" : nvl( mavenSettingsLocation.getUrl() ) ); //$NON-NLS-1$

        IEclipsePreferencesLocation eclipsePreferencesLocation = project.getEclipsePreferencesLocation();
        if ( eclipsePreferencesLocation == null )
        {
            eclipsePreferencesLocationText.setText( "" ); //$NON-NLS-1$
        }
        else
        {
            String url = nvl( eclipsePreferencesLocation.getUrl() );
            eclipsePreferencesLocationText.setText( url );
            if ( IS2Project.PROJECT_PREFERENCES_FILENAME.equals( url ) )
            {
                eclipsePreferencesLocationRealmComposite.setEnabled( false );
                getValidationGroup().performValidation();
            }
        }
    }

    public void addRealmChangeListener( IRealmChangeListener listener )
    {
        mavenSettingsLocationRealmComposite.addRealmChangeListener( listener );
        eclipsePreferencesLocationRealmComposite.addRealmChangeListener( listener );
    }

    public void saveRealms( RealmUrlCollector realmUrlCollector )
    {
        realmUrlCollector.collect( mavenSettingsLocationRealmComposite );
        realmUrlCollector.collect( eclipsePreferencesLocationRealmComposite );
    }

    abstract protected Collection<PreferenceGroup> getEclipsePreferenceGroups();

    abstract protected void saveEclipsePreferenceGroups( Collection<PreferenceGroup> groups );
}
