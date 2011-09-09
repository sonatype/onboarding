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
package com.sonatype.s2.project.ui.materialization.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputComposite;
import org.netbeans.validation.api.Problem;

import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.prefs.PreferenceGroup;
import com.sonatype.s2.project.ui.internal.Activator;
import com.sonatype.s2.project.ui.internal.Messages;
import com.sonatype.s2.project.ui.internal.wizards.WizardPageWithHistory;

public class PreferenceExportPage
    extends WizardPageWithHistory
{
    private Button selectButton;

    private Button deselectButton;

    private Button fileRadio;

    private Button uploadRadio;

    private Button browseButton;

    private Combo fileCombo;

    private Combo uploadCombo;

    private CheckboxTreeViewer viewer;

    private UrlComposite urlComposite;

    private Collection<PreferenceGroup> groups = new ArrayList<PreferenceGroup>();
    
    private SwtValidationGroup validationGroup;

    public PreferenceExportPage()
    {
        super( Messages.exportWizard_groupsPage_title );
        setTitle( Messages.exportWizard_groupsPage_title );
        setDescription( Messages.exportWizard_groupsPage_description );
        setPageComplete( false );

        validationGroup = SwtValidationGroup.create( SwtValidationUI.createUI( this ) );
    }

    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NULL );
        composite.setLayout( new GridLayout() );

        createSelectorGroup( composite );
        createDestinationGroup( composite );

        setControl( composite );

        selectGroups( true );
        selectDestination();
    }

    private void createSelectorGroup( Composite composite )
    {
        Group group = new Group( composite, SWT.NONE );
        group.setLayout( new GridLayout( 2, false ) );
        group.setText( Messages.exportWizard_groupsPage_preferenceGroups );

        GridData gridData = new GridData( SWT.FILL, SWT.FILL, true, true );
        gridData.heightHint = 100;
        gridData.widthHint = 500;
        group.setLayoutData( gridData );

        viewer = new CheckboxTreeViewer( group, SWT.BORDER );
        viewer.getControl().setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true, 1, 3 ) );
        viewer.setContentProvider( new PreferenceGroupContentProvider() );
        viewer.setLabelProvider( new PreferenceGroupLabelProvider() );
        viewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                validate();
            }
        } );
        try
        {
            groups = S2ProjectCore.getInstance().getPrefManager().getPreferenceGroups();
        }
        catch ( CoreException e )
        {
            StatusManager.getManager().handle( e, Activator.PLUGIN_ID );
        }
        viewer.setInput( groups );

        selectButton = new Button( group, SWT.NONE );
        selectButton.setLayoutData( new GridData( SWT.FILL, SWT.TOP, false, false ) );
        selectButton.setText( Messages.actions_selectAll_title );
        selectButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                selectGroups( true );
            }
        } );

        deselectButton = new Button( group, SWT.NONE );
        deselectButton.setLayoutData( new GridData( SWT.FILL, SWT.TOP, false, false ) );
        deselectButton.setText( Messages.actions_deselectAll_title );
        deselectButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                selectGroups( false );
            }
        } );
    }

    private void createDestinationGroup( Composite composite )
    {
        Group group = new Group( composite, SWT.NONE );
        group.setLayout( new GridLayout() );
        group.setText( Messages.exportWizard_groupsPage_destination );
        group.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );

        urlComposite = new UrlComposite( group, Messages.exportWizard_groupsPage_deployToLabel );
        urlComposite.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );
    }

    private void selectDestination()
    {
        boolean save = fileRadio.getSelection();

        fileCombo.setEnabled( save );
        browseButton.setEnabled( save );
        urlComposite.enableControls( !save );

        validate();
    }

    private void selectGroups( boolean select )
    {
        for ( PreferenceGroup group : groups )
        {
            viewer.setSubtreeChecked( group, select );
        }
        validate();
    }

    private void validate()
    {
        String message = null;
        boolean warning = false;

        if ( viewer.getCheckedElements().length == 0 )
        {
            message = Messages.exportWizard_groupsPage_errors_nothingSelected;
        }
        else
        {
            if ( fileRadio.getSelection() )
            {
                validationGroup.performValidation();

                String text = fileCombo.getText().trim();
                if ( text.length() <= 0 )
                {
                    fileCombo.setFocus();
                    message = Messages.exportWizard_groupsPage_errors_enterFileName;
                } else {
                    File fil = new File(text);
                    if ( fil.isDirectory() ) 
                    {
                        fileCombo.setFocus();
                        message = Messages.exportWizard_groupsPage_errors_directory;
                    } else if ( !fil.isAbsolute() ) {
                        fileCombo.setFocus();
                        message = Messages.exportWizard_groupsPage_errors_absolute;
                    } else if ( fil.exists() ) {
                        fileCombo.setFocus();
                        message = Messages.exportWizard_groupsPage_errors_exists;
                        warning = true;
                    }
                }
            }
            else
            {
//                String uploadUrl = uploadCombo.getText();
                Problem problem = validationGroup.performValidation();
                if ( problem != null && problem.isFatal() ) {
                    message = problem.getMessage();
                }
//                if ( uploadUrl.length() <= 0 )
//                {
//                    uploadCombo.setFocus();
//                    message = Messages.exportWizard_groupsPage_errors_enterUrl;
//                }
//                else
//                {
//                    message = urlComposite.validateControls();
//                }
            }
        }
        setMessage( null, warning ? DialogPage.ERROR : DialogPage.WARNING );
        setMessage( message, warning ? DialogPage.WARNING : DialogPage.ERROR );
        setPageComplete( message == null || warning);
    }

    ExportData getExportData()
    {
        return new ExportData();
    }

    private class UrlComposite
        extends UrlInputComposite
    {
        private UrlComposite( Composite parent, String urlLabelText )
        {
            super( parent, null, validationGroup, ALLOW_ANONYMOUS );
            setUrlLabelText( urlLabelText );
        }

        @Override
        protected void createControls()
        {
            setInputHistory( PreferenceExportPage.this.getInputHistory() );

            SelectionListener selectionListener = new SelectionAdapter()
            {
                @Override
                public void widgetSelected( SelectionEvent e )
                {
                    selectDestination();
                }
            };
            fileRadio = new Button( this, SWT.RADIO );
            fileRadio.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, false, false ) );
            fileRadio.setText( Messages.exportWizard_groupsPage_saveToFileLabel );
            fileRadio.setSelection( true );
            fileRadio.addSelectionListener( selectionListener );

            fileCombo = new Combo( this, SWT.BORDER );
            GridData gd = new GridData( SWT.FILL, SWT.CENTER, true, false, 2, 1 );
            gd.horizontalIndent = 10;
            fileCombo.setLayoutData( gd );
            fileCombo.addModifyListener( new ModifyListener()
            {
                public void modifyText( ModifyEvent e )
                {
                    PreferenceExportPage.this.validate();
                }
            } );
            addToInputHistory( "preference-export-file", fileCombo );

            browseButton = new Button( this, SWT.NONE );
            browseButton.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
            browseButton.setText( Messages.actions_browse_title );
            browseButton.addSelectionListener( new SelectionAdapter()
            {
                @Override
                public void widgetSelected( SelectionEvent e )
                {
                    FileDialog fileDialog = new FileDialog( getShell(), SWT.SAVE );
                    fileDialog.setFileName( "preferences.jar" );
                    fileDialog.setOverwrite( true );
                    String file = fileDialog.open();
                    if ( file != null )
                    {
                        fileCombo.setText( file );
                    }
                }
            } );

            uploadRadio = new Button( this, SWT.RADIO );
            uploadRadio.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, false, false ) );
            uploadRadio.setText( Messages.exportWizard_groupsPage_uploadToLabel );
            uploadRadio.addSelectionListener( selectionListener );

            createUrlControl();

            PreferenceExportPage.this.uploadCombo = this.getComboBoxComponent();
            assert PreferenceExportPage.this.uploadCombo != null;
        }
    }

    class ExportData
    {
        boolean deploy = false;

        String fileName = null;

        String uploadUrl = null;

        Collection<PreferenceGroup> preferences = new ArrayList<PreferenceGroup>();

        private ExportData()
        {
            if ( fileRadio.getSelection() )
            {
                fileName = fileCombo.getText();
            }
            else
            {
                deploy = true;
                uploadUrl = urlComposite.getUrl();
            }

            for ( Object o : viewer.getCheckedElements() )
            {
                preferences.add( (PreferenceGroup) o );
            }
        }
    }

    private class PreferenceGroupContentProvider
        implements ITreeContentProvider
    {
        public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
        {
        }

        public void dispose()
        {
        }

        @SuppressWarnings( "unchecked" )
        public Object[] getElements( Object inputElement )
        {
            if ( inputElement instanceof Collection<?> )
            {
                return ( (Collection<PreferenceGroup>) inputElement ).toArray( new PreferenceGroup[0] );
            }
            return null;
        }

        public boolean hasChildren( Object element )
        {
            return false;
        }

        public Object getParent( Object element )
        {
            return null;
        }

        public Object[] getChildren( Object parentElement )
        {
            return null;
        }
    }

    private class PreferenceGroupLabelProvider
        extends LabelProvider
    {
        @Override
        public String getText( Object element )
        {
            if ( element instanceof PreferenceGroup )
            {
                return ( (PreferenceGroup) element ).name();
            }
            return super.getText( element );
        }
    }
}
