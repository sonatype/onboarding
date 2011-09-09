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
package com.sonatype.s2.project.ui.codebase.dialogs;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.ui.common.InputHistory;

import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.prefs.PreferenceGroup;
import com.sonatype.s2.project.ui.codebase.Activator;
import com.sonatype.s2.project.ui.codebase.Messages;

public class EclipsePreferencesDialog
    extends TitleAreaDialog
{
    private static final String COMBO_ID = "externalUrlCombo";

    private boolean urlSelected;

    private String externalUrl;

    private Collection<PreferenceGroup> preferenceGroups;

    private Collection<PreferenceGroup> allGroups;

    private Button urlRadio;

    private Button exportRadio;

    private Combo urlCombo;

    private CheckboxTableViewer groupViewer;

    private Button selectButton;

    private Button deselectButton;

    private InputHistory inputHistory;

    public EclipsePreferencesDialog( Shell parentShell, String externalUrl, Collection<PreferenceGroup> preferenceGroups )
    {
        super( parentShell );
        this.externalUrl = externalUrl;
        this.preferenceGroups = preferenceGroups;
        inputHistory = new InputHistory( EclipsePreferencesDialog.class.getName() );
    }

    @Override
    protected Control createDialogArea( Composite parent )
    {
        Composite dialogArea = (Composite) super.createDialogArea( parent );
        setMessage( Messages.eclipsePreferencesDialog_message );
        setTitle( Messages.eclipsePreferencesDialog_title );

        Composite panel = new Composite( dialogArea, SWT.NONE );
        GridLayout gl = new GridLayout( 3, false );
        gl.marginLeft = 10;
        gl.marginRight = 10;
        panel.setLayout( gl );
        GridData gd = new GridData( SWT.FILL, SWT.TOP, true, false );
        gd.heightHint = 200;
        panel.setLayoutData( gd );

        SelectionAdapter selectionAdapter = new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                boolean b = urlRadio.getSelection();
                urlCombo.setEnabled( b );
                groupViewer.getTable().setEnabled( !b );
                selectButton.setEnabled( !b );
                deselectButton.setEnabled( !b );
                validate();
            }
        };

        urlRadio = new Button( panel, SWT.RADIO );
        urlRadio.setText( Messages.eclipsePreferencesDialog_externalUrl );
        urlRadio.addSelectionListener( selectionAdapter );

        urlCombo = new Combo( panel, SWT.BORDER );
        urlCombo.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 2, 1 ) );
        urlCombo.setData( "name", COMBO_ID );
        inputHistory.add( COMBO_ID, urlCombo );
        inputHistory.load();

        exportRadio = new Button( panel, SWT.RADIO );
        exportRadio.setText( Messages.eclipsePreferencesDialog_exportWorkspacePreferences );
        exportRadio.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 3, 1 ) );
        exportRadio.addSelectionListener( selectionAdapter );

        groupViewer =
            CheckboxTableViewer.newCheckList( panel, SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.SINGLE );
        groupViewer.getTable().setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true, 2, 3 ) );
        groupViewer.setContentProvider( new IStructuredContentProvider()
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
                if ( inputElement instanceof Collection )
                {
                    return ( (Collection) inputElement ).toArray();
                }
                return null;
            }
        } );
        groupViewer.setLabelProvider( new LabelProvider() );
        groupViewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                validate();
            }
        } );

        selectButton = new Button( panel, SWT.NONE );
        selectButton.setLayoutData( new GridData( SWT.FILL, SWT.TOP, false, false ) );
        selectButton.setText( Messages.eclipsePreferencesDialog_selectAll );
        selectButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                selectGroups( true );
            }
        } );

        deselectButton = new Button( panel, SWT.NONE );
        deselectButton.setLayoutData( new GridData( SWT.FILL, SWT.TOP, false, false ) );
        deselectButton.setText( Messages.eclipsePreferencesDialog_deselectAll );
        deselectButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                selectGroups( false );
            }
        } );

        try
        {
            allGroups = S2ProjectCore.getInstance().getPrefManager().getPreferenceGroups();
            groupViewer.setInput( allGroups );
        }
        catch ( CoreException e )
        {
            StatusManager.getManager().handle( e, Activator.PLUGIN_ID );
        }

        if ( preferenceGroups != null )
        {
            groupViewer.setCheckedElements( preferenceGroups.toArray() );
        }
        else
        {
            selectGroups( true );
        }

        if ( externalUrl != null )
        {
            urlCombo.setText( externalUrl );
            urlRadio.setSelection( true );
        }
        else
        {
            exportRadio.setSelection( true );
        }
        selectionAdapter.widgetSelected( null );

        applyDialogFont( dialogArea );
        return dialogArea;
    }

    @Override
    protected void configureShell( Shell newShell )
    {
        super.configureShell( newShell );
        newShell.setText( Messages.eclipsePreferencesDialog_title );
    }

    @Override
    protected void okPressed()
    {
        urlSelected = urlRadio.getSelection();
        if ( urlSelected )
        {
            externalUrl = urlCombo.getText();
            preferenceGroups = null;
            inputHistory.save();
        }
        else
        {
            externalUrl = null;
            preferenceGroups = new ArrayList<PreferenceGroup>();
            for ( Object o : groupViewer.getCheckedElements() )
            {
                preferenceGroups.add( (PreferenceGroup) o );
            }
        }

        super.okPressed();
    }

    private void validate()
    {
        String message = null;

        if ( exportRadio.getSelection() && groupViewer.getCheckedElements().length == 0 )
        {
            message = Messages.eclipsePreferencesDialog_nothingSelected;
        }

        setErrorMessage( message );
        Button ok = getButton( IDialogConstants.OK_ID );
        if ( ok != null )
        {
            ok.setEnabled( message == null );
        }
    }

    private void selectGroups( boolean select )
    {
        for ( PreferenceGroup group : allGroups )
        {
            groupViewer.setChecked( group, select );
        }
        validate();
    }

    public boolean isUrlSelected()
    {
        return urlSelected;
    }

    public String getUrl()
    {
        return externalUrl;
    }

    public Collection<PreferenceGroup> getGroups()
    {
        return preferenceGroups;
    }
}
