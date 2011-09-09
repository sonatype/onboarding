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

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.sonatype.s2.project.ui.internal.Messages;

public class ProjectUrlPage
    extends WizardPageWithHistory
{
    private final static String HISTORY_ID = "project-descriptor-url";

    private Combo urlCombo;

    private String url;

    public ProjectUrlPage()
    {
        this( null );
    }

    public ProjectUrlPage( String url )
    {
        super( Messages.materializationWizard_title );
        setTitle( Messages.materializationWizard_title );
        setDescription( Messages.materializationWizard_urlPage_description );
        setPageComplete( false );
        this.url = url;
    }

    public void createControl( Composite parent )
    {
        Composite container = new Composite( parent, SWT.NONE );
        container.setLayout( new GridLayout( 2, false ) );

        Label label = new Label( container, SWT.NONE );
        label.setText( Messages.materializationWizard_urlPage_urlLabel );

        urlCombo = new Combo( container, SWT.BORDER );
        urlCombo.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false ) );
        if ( url != null )
        {
            urlCombo.setText( url );
        }
        addToInputHistory( HISTORY_ID, urlCombo );
        urlCombo.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                validate();
            }
        } );
        urlCombo.addSelectionListener( new SelectionListener()
        {
            public void widgetSelected( SelectionEvent e )
            {
                validate();
            }
            public void widgetDefaultSelected( SelectionEvent e )
            {
                
            }
        });

        setControl( container );
        validate();
    }

    @Override
    public IWizardPage getNextPage()
    {
        final String url = urlCombo.getText();

        try
        {
            ( (AbstractMaterializationWizard) getWizard() ).setProject( url );
            return super.getNextPage();
        }
        catch ( CoreException e )
        {
            setErrorMessage( e.getMessage() );
        }

        return null;
    }

    @Override
    public boolean canFlipToNextPage()
    {
        // changed the default to not invoke getNextPage() here
        return isPageComplete();
    }

    private void validate()
    {
        String url = urlCombo.getText().trim();
        String message = null;

        if ( url.length() == 0 )
        {
            message = Messages.materializationWizard_urlPage_enterUrl;
        }
        else
        {
            try
            {
                new URL( url );
            }
            catch ( MalformedURLException e )
            {
                message = NLS.bind( Messages.materializationWizard_urlPage_invalidUrl, e.getMessage() );
            }
        }
        setErrorMessage( message );
        setPageComplete( message == null );
    }
}
