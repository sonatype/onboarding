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
package com.sonatype.s2.project.ui.internal.composites;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import com.sonatype.s2.project.ui.internal.Messages;
import com.sonatype.s2.project.ui.internal.views.ValidationStatusContentProvider;
import com.sonatype.s2.project.ui.internal.views.ValidationStatusLabelProvider;

public class ValidationStatusComposite
    extends Composite
{
    private final static String[] COLUMNS =
        new String[] { Messages.validationStatusViewer_columns_messages,
            Messages.validationStatusViewer_columns_validator };

    private TreeViewer treeViewer;

    private ValidationStatusLabelProvider labelProvider;

    private ValidationStatusContentProvider contentProvider;

    private Text detailsText;

    private Button showErrorsOnlyButton;

    private IAction copyAction;

    public ValidationStatusComposite( Composite parent )
    {
        super( parent, SWT.NONE );
        setLayout( new GridLayout( 2, false ) );

        createViewer();
        createDetails();

        Composite buttonsComposite = new Composite( this, SWT.NONE );
        buttonsComposite.setLayout( new GridLayout() );
        buttonsComposite.setLayoutData( new GridData( SWT.FILL, SWT.TOP, false, false ) );
        createButtons( buttonsComposite );
    }

    protected void createButtons( Composite parent )
    {
        showErrorsOnlyButton = new Button( parent, SWT.CHECK );
        showErrorsOnlyButton.setText( Messages.validationStatusViewer_showErrorsOnly );
        showErrorsOnlyButton.setEnabled( true );
        showErrorsOnlyButton.setLayoutData( new GridData( SWT.FILL, SWT.BOTTOM, false, false ) );
        showErrorsOnlyButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                contentProvider.setErrorsOnly( showErrorsOnlyButton.getSelection() );
                treeViewer.refresh();
            }
        } );
    }

    private void createDetails()
    {
        detailsText = new Text( this, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.WRAP );
        GridData gd = new GridData( SWT.FILL, SWT.FILL, true, true );
        gd.minimumHeight = 70;
        detailsText.setLayoutData( gd );
        detailsText.setBackground( detailsText.getDisplay().getSystemColor( SWT.COLOR_WIDGET_BACKGROUND ) );
    }

    private void createViewer()
    {
        treeViewer = new TreeViewer( this, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE );
        GridData gd_tree = new GridData( SWT.FILL, SWT.FILL, true, true, 2, 1 );
        gd_tree.heightHint = 200;
        gd_tree.widthHint = 700;

        final Tree tree = treeViewer.getTree();
        tree.setHeaderVisible( true );
        tree.setLinesVisible( true );

        for ( int i = 0; i < COLUMNS.length; i++ )
        {
            TreeViewerColumn tvc = new TreeViewerColumn( treeViewer, SWT.NONE, i );
            TreeColumn tc = tvc.getColumn();
            tc.setResizable( true );
            tc.setText( COLUMNS[i] );
            tc.setWidth( i == ValidationStatusLabelProvider.MESSAGE_COLUMN ? 500 : 200 );
        }
        tree.setLayoutData( gd_tree );

        contentProvider = new ValidationStatusContentProvider();
        treeViewer.setContentProvider( contentProvider );

        labelProvider = new ValidationStatusLabelProvider();
        treeViewer.setLabelProvider( labelProvider );

        treeViewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                Object selection = ( (IStructuredSelection) event.getSelection() ).getFirstElement();
                if ( selection instanceof IStatus )
                {
                    detailsText.setText( labelProvider.extractMessage( (IStatus) selection ) );
                }
            }
        } );

        copyAction = new Action( Messages.actions_copy_title )
        {
            @Override
            public void run()
            {
                Object selection = ( (IStructuredSelection) treeViewer.getSelection() ).getFirstElement();
                if ( selection instanceof IStatus )
                {
                    Clipboard clipboard = new Clipboard( getShell().getDisplay() );
                    clipboard.setContents( new Object[] { labelProvider.extractMessage( (IStatus) selection ) },
                                           new TextTransfer[] { TextTransfer.getInstance() } );
                }

            }
        };

        MenuManager menuMgr = new MenuManager(); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown( true );
        menuMgr.addMenuListener( new IMenuListener()
        {
            public void menuAboutToShow( IMenuManager manager )
            {
                if ( !treeViewer.getSelection().isEmpty() )
                {
                    manager.add( copyAction );
                }
            }
        } );
        Menu menu = menuMgr.createContextMenu( tree );
        tree.setMenu( menu );
    }

    public void setInput( Object input )
    {
        showErrorsOnlyButton.setSelection( input == null || ( input instanceof IStatus && !( (IStatus) input ).isOK() ) );
        detailsText.setText( "" );
        treeViewer.setInput( input );
    }

    public void addSelectionChangedListener( ISelectionChangedListener listener )
    {
        treeViewer.addSelectionChangedListener( listener );
    }

    public void removeSelectionChangedListener( ISelectionChangedListener listener )
    {
        treeViewer.removeSelectionChangedListener( listener );
    }

    public void refresh()
    {
        treeViewer.refresh();
    }

    public void expandAll()
    {
        treeViewer.expandAll();
    }

    public IStructuredSelection getSelection()
    {
        ISelection selection = treeViewer.getSelection();
        assert selection instanceof IStructuredSelection;
        return (IStructuredSelection) selection;
    }
}
