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

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.validation.SonatypeValidators;
import org.netbeans.validation.api.ui.ValidationGroup;

import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;
import com.sonatype.s2.publisher.nexus.NexusRepositories;

public class AddRepositoryDialog
    extends TitleAreaDialog
{
    private ValidationGroup vg;

    private Label lblName;

    private Label lblLocation;

    private Text txtName;

    private Combo txtLocation;

    private String name;

    private String location;

    private final NexusLineupPublishingInfo info;

    /**
     * Create the dialog.
     * 
     * @param parentShell
     */
    public AddRepositoryDialog( Shell parentShell, NexusLineupPublishingInfo info )
    {
        super( parentShell );
        this.info = info;
    }

    protected void addRepositories( final Collection<String> urls )
    {
        Display.getDefault().syncExec( new Runnable()
        {
            public void run()
            {
                for ( String url : urls )
                {
                    txtLocation.add( url );
                }
            }
        } );
    }

    /**
     * Create contents of the dialog.
     * 
     * @param parent
     */
    @SuppressWarnings( "unchecked" )
    @Override
    protected Control createDialogArea( Composite parent )
    {
        Composite container = (Composite) super.createDialogArea( parent );
        setTitle( Messages.addRepositoryDialog_title );
        setMessage( Messages.addRepositoryDialog_description );

        Composite composite = new Composite( container, SWT.NONE );
        composite.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );
        composite.setLayout( new GridLayout( 2, false ) );

        lblName = new Label( composite, SWT.NONE );
        lblName.setText( Messages.addRepositoryDialog_name_label );
        lblName.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );

        txtName = new Text( composite, SWT.BORDER );
        txtName.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false ) );

        lblLocation = new Label( composite, SWT.NONE );
        lblLocation.setText( Messages.addRepositoryDialog_location_label );
        lblLocation.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );

        txtLocation = new Combo( composite, SWT.BORDER );
        txtLocation.setFocus();
        txtLocation.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false ) );

        Job job = new Job( Messages.addRepositoryDialog_job )
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                Collection<String> repos = new ArrayList<String>();
                NexusRepositories.getP2Repositories( info, repos, monitor );
                addRepositories( repos );
                return Status.OK_STATUS;
            }
        };
        job.schedule();

        vg = SwtValidationGroup.create( SwtValidationUI.createUI( this ) );
        SwtValidationGroup.setComponentName( txtLocation, Messages.addRepositoryDialog_location_name );

        vg.add( txtLocation, SonatypeValidators.createRemoteHttpUrlValidators() );

        return container;
    }

    @Override
    protected Control createButtonBar( Composite parent )
    {
        Control c = super.createButtonBar( parent );
        getButton( OK ).setEnabled( false );
        return c;
    }

    public final String getLocation()
    {
        return location;
    }

    public final String getName()
    {
        return name;
    }

    protected void okPressed()
    {
        location = txtLocation.getText();
        name = txtName.getText();
        super.okPressed();
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize()
    {
        return getShell().computeSize( SWT.DEFAULT, SWT.DEFAULT, true );
    }

    @Override
    protected void configureShell( Shell shell )
    {
        super.configureShell( shell );
        shell.setText( Messages.addRepositoryDialog_title );
    }
}
