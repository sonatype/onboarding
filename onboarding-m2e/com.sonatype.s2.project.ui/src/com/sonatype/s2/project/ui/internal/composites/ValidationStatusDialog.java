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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.sonatype.s2.project.ui.internal.Messages;

public class ValidationStatusDialog
    extends TitleAreaDialog
{
    private IStatus status;

    private ValidationStatusComposite statusViewer;

    public ValidationStatusDialog( Shell parentShell, IStatus status )
    {
        super( parentShell );
        this.status = status;
    }

    @Override
    protected void createButtonsForButtonBar( Composite parent )
    {
        Button button = createButton( parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true );
        button.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                close();
            }
        } );
    }

    @Override
    protected Control createDialogArea( Composite parent )
    {
        Composite dialogArea = (Composite) super.createDialogArea( parent );
        setMessage( status.isOK() ? Messages.validationStatusViewer_successful : Messages.validationStatusViewer_failed );
        setTitle( Messages.validationStatusViewer_title );

        Composite panel = new Composite( dialogArea, SWT.NONE );
        GridLayout gl = new GridLayout();
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        gl.marginLeft = 10;
        gl.marginRight = 10;
        panel.setLayout( gl );
        GridData gd = new GridData( SWT.FILL, SWT.TOP, true, false );
        gd.widthHint = 700;
        gd.heightHint = 300;
        panel.setLayoutData( gd );

        statusViewer = new ValidationStatusComposite( panel );
        statusViewer.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );

        statusViewer.setInput( status );

        applyDialogFont( dialogArea );
        return dialogArea;
    }

    @Override
    protected void configureShell( Shell newShell )
    {
        super.configureShell( newShell );
        newShell.setText( Messages.validationStatusViewer_title );
    }
}
