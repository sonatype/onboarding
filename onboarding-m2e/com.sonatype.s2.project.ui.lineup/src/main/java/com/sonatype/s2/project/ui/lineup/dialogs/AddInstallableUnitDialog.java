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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.sonatype.s2.extractor.AddRepositoryWrapper;
import com.sonatype.s2.extractor.AvailableGroupWrapper;
import com.sonatype.s2.extractor.RemoveRepositoryWrapper;
import com.sonatype.s2.p2lineup.model.IP2LineupInstallableUnit;
import com.sonatype.s2.p2lineup.model.IP2LineupSourceRepository;
import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;
import com.sonatype.s2.publisher.nexus.NexusRepositories;

public class AddInstallableUnitDialog
    extends TitleAreaDialog
{

    private AvailableGroupWrapper group;

    private IP2LineupInstallableUnit[] selectedIUs;

    private IP2LineupSourceRepository[] selectedRepos;

    private Collection<URI> toRemove;

    /**
     * Create the dialog.
     * 
     * @param parentShell
     */
    public AddInstallableUnitDialog( Shell parentShell, final NexusLineupPublishingInfo info )
    {
        super( parentShell );
        setShellStyle( SWT.BORDER | SWT.RESIZE | SWT.TITLE );

        Job job = new Job( Messages.addInstallableUnitDialog_job )
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                final SubMonitor subMon = SubMonitor.convert( monitor, 10 );
                final Collection<String> repos = new ArrayList<String>();
                NexusRepositories.getP2Repositories( info, repos, subMon.newChild( 5 ) );
                setRepositoriesToRemove( new AddRepositoryWrapper().addRepositories( repos ) );
                return Status.OK_STATUS;
            }

        };
        job.schedule();
        try
        {
            job.join();
        }
        catch ( InterruptedException e )
        {
            // do nothing
        }
    }

    private void setRepositoriesToRemove( Collection<URI> toRemove )
    {
        this.toRemove = toRemove;
    }

    private void removeNewRepositories()
    {
        if ( toRemove == null )
            return;
        RemoveRepositoryWrapper.removeRepositories( toRemove );
    }

    /**
     * Create contents of the dialog.
     * 
     * @param parent
     */
    @Override
    protected Control createDialogArea( Composite parent )
    {
        setTitle( Messages.addInstallableUnitDialog_title );
        setMessage( Messages.addInstallableUnitDialog_description );
        Composite area = (Composite) super.createDialogArea( parent );
        area.setLayout( new GridLayout( 1, false ) );

        group = new AvailableGroupWrapper();
        group.createControl( area );

        return area;
    }

    public IP2LineupInstallableUnit[] getInstallableUnits()
    {
        return selectedIUs;
    }

    public IP2LineupSourceRepository[] getRepositories()
    {
        return selectedRepos;
    }

    /**
     * Create contents of the button bar.
     * 
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar( Composite parent )
    {
        createButton( parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true );
        createButton( parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false );
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
    public boolean close()
    {
        selectedIUs = group.getSelectedIUs();
        selectedRepos = group.getRepositories();
        removeNewRepositories();
        return super.close();
    }

    @Override
    protected void configureShell( Shell newShell )
    {
        // TODO Auto-generated method stub
        super.configureShell( newShell );
        newShell.setText( Messages.addInstallableUnitDialog_title );
    }
}
