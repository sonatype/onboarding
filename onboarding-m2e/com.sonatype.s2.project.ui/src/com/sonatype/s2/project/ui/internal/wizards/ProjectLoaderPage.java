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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.ui.internal.Messages;

public class ProjectLoaderPage
    extends WizardPage
{
    private static Logger log = LoggerFactory.getLogger( ProjectLoaderPage.class );

    private String url;

    public ProjectLoaderPage( String url )
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
        label.setLayoutData( new GridData( SWT.LEFT, SWT.TOP, false, false ) );

        Label urlLabel = new Label( container, SWT.WRAP );
        urlLabel.setText( url );
        GridData gd = new GridData( SWT.FILL, SWT.TOP, true, false );
        gd.widthHint = 100;
        urlLabel.setLayoutData( gd );

        setControl( container );
    }

    @Override
    public void setVisible( boolean visible )
    {
        super.setVisible( visible );
        if ( visible && url != null )
        {
            getControl().getDisplay().asyncExec( new Runnable()
            {
                public void run()
                {
                    try
                    {
                        ( (AbstractMaterializationWizard) getWizard() ).setProject( url );
                        url = null;
                        setPageComplete( true );
                        getContainer().updateButtons();
                    }
                    catch ( CoreException e )
                    {
                        log.error( e.getMessage(), e );
                        setErrorMessage( e.getMessage() );
                    }
                }
            } );
        }
    }
}
