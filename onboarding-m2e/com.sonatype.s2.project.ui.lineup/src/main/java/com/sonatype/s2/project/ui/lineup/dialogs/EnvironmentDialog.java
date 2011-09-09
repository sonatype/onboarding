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
package com.sonatype.s2.project.ui.lineup.dialogs;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.sonatype.s2.p2lineup.model.IP2LineupTargetEnvironment;
import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.project.ui.lineup.composites.EnvironmentLabelProvider;

public class EnvironmentDialog
    extends TitleAreaDialog
{
    private CheckboxTableViewer viewer;

    private String iuTitle;

    private Button allRadio;

    private Button someRadio;

    private Object[] targetSelection;

    public EnvironmentDialog( Shell parentShell, String iuTitle, Set<IP2LineupTargetEnvironment> selection )
    {
        super( parentShell );
        setShellStyle( SWT.DIALOG_TRIM | SWT.MAX | SWT.RESIZE );
        this.iuTitle = iuTitle;
        this.targetSelection = selection == null || selection.isEmpty() ? null : selection.toArray();
    }

    @Override
    protected void configureShell( Shell newShell )
    {
        super.configureShell( newShell );
        newShell.setText( Messages.environmentDialog_title );
    }

    @Override
    protected Control createDialogArea( Composite parent )
    {
        Composite dialogArea = (Composite) super.createDialogArea( parent );
        setMessage( NLS.bind( Messages.environmentDialog_description, iuTitle ) );
        setTitle( Messages.environmentDialog_title );

        Composite panel = new Composite( dialogArea, SWT.NONE );
        GridLayout gl = new GridLayout();
        gl.horizontalSpacing = 0;
        gl.marginLeft = 10;
        gl.marginRight = 10;
        panel.setLayout( gl );
        panel.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );

        SelectionListener selectionListener = new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                updateSelection();
                viewer.getControl().setEnabled( someRadio.getSelection() );
            }
        };

        allRadio = new Button( panel, SWT.RADIO );
        allRadio.setText( Messages.environmentDialog_allowAll );
        allRadio.setData( "name", "allowAllRadio" ); //$NON-NLS-1$ //$NON-NLS-2$
        allRadio.setSelection( targetSelection == null );
        allRadio.addSelectionListener( selectionListener );

        someRadio = new Button( panel, SWT.RADIO );
        someRadio.setText( Messages.environmentDialog_allowSome );
        someRadio.setData( "name", "allowSomeRadio" ); //$NON-NLS-1$ //$NON-NLS-2$
        someRadio.setSelection( targetSelection != null );
        someRadio.addSelectionListener( selectionListener );

        viewer = CheckboxTableViewer.newCheckList( panel, SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.BORDER );
        GridData gd = new GridData( SWT.FILL, SWT.TOP, true, false );
        gd.heightHint = 130;
        viewer.getControl().setLayoutData( gd );
        viewer.getControl().setData( "name", "environmentTable" ); //$NON-NLS-1$ //$NON-NLS-2$

        viewer.setLabelProvider( new EnvironmentLabelProvider() );
        viewer.setContentProvider( new IStructuredContentProvider()
        {
            public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
            {
            }

            public void dispose()
            {
            }

            public Object[] getElements( Object inputElement )
            {
                if ( inputElement instanceof List )
                {
                    return ( (List) inputElement ).toArray();
                }
                return null;
            }
        } );

        viewer.setInput( EnvironmentLabelProvider.getSupportedEnvironments() );

        if ( targetSelection == null )
        {
            viewer.getControl().setEnabled( false );
        }
        else
        {
            viewer.setCheckedElements( targetSelection );
        }

        viewer.addCheckStateListener( new ICheckStateListener()
        {
            public void checkStateChanged( CheckStateChangedEvent event )
            {
                updateSelection();
            }
        } );
        viewer.addDoubleClickListener( new IDoubleClickListener()
        {
            public void doubleClick( DoubleClickEvent event )
            {
                ISelection selection = event.getSelection();
                assert selection instanceof IStructuredSelection;
                for ( Object o : ( (IStructuredSelection) selection ).toArray() )
                {
                    viewer.setChecked( o, !viewer.getChecked( o ) );
                }
            }
        } );

        applyDialogFont( dialogArea );
        return dialogArea;
    }

    public Set<IP2LineupTargetEnvironment> getSelection()
    {
        if ( targetSelection == null )
        {
            return null;
        }

        Set<IP2LineupTargetEnvironment> list = new LinkedHashSet<IP2LineupTargetEnvironment>();
        for ( Object o : targetSelection )
        {
            assert o instanceof IP2LineupTargetEnvironment;
            list.add( (IP2LineupTargetEnvironment) o );
        }
        return list;
    }

    private void updateSelection()
    {
        targetSelection = allRadio.getSelection() ? null : viewer.getCheckedElements();
    }
}
