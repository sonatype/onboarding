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
package com.sonatype.s2.project.ui.codebase.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.statushandlers.StatusManager;

import com.sonatype.nexus.p2.rest.model.P2LineupSummaryDto;
import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.project.model.descriptor.P2LineupLocation;
import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.project.ui.lineup.NewLineupProjectOperation;
import com.sonatype.s2.project.ui.lineup.composites.EnvironmentLabelProvider;
import com.sonatype.s2.project.ui.lineup.wizard.IURepoPage;
import com.sonatype.s2.project.ui.lineup.wizard.LineupCoordinatesPage;
import com.sonatype.s2.project.ui.lineup.wizard.RuntimeEnvironmentPage;
import com.sonatype.s2.publisher.Activator;
import com.sonatype.s2.publisher.nexus.NexusLineupPublisher;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;

@SuppressWarnings( "restriction" )
public class NewLineupWizard
    extends NewCodebaseWizard
    implements INewWizard
{
    private NexusLineupPublishingInfo info;

    private LineupCoordinatesPage lineupCoordinatesPage;

    private RuntimeEnvironmentPage runtimeEnvironmentPage;

    private IURepoPage iuRepoPage;

    private LineupToCodebasePage lineupToCodebasePage;

    @Override
    public void init( IWorkbench workbench, IStructuredSelection selection )
    {
        super.init( workbench, selection );

        setWindowTitle( Messages.newLineupWizard_title );
        setNeedsProgressMonitor( true );

        info = new NexusLineupPublishingInfo();
        info.getLineup().setGroupId( "com.mycompany" ); //$NON-NLS-1$
        info.getLineup().setId( "baselineup" ); //$NON-NLS-1$
        info.getLineup().setVersion( "1.0" ); //$NON-NLS-1$
        info.getLineup().getTargetEnvironments().addAll( EnvironmentLabelProvider.getSupportedEnvironments() );

        getProject().setGroupId( null );
        getProject().setVersion( null );
    }

    @Override
    public boolean performFinish()
    {
        Throwable t = null;

        try
        {
            if ( lineupToCodebasePage.isLocal() )
            {
                getContainer().run( true, true, new IRunnableWithProgress()
                {
                    public void run( IProgressMonitor monitor )
                        throws InvocationTargetException, InterruptedException
                    {
                        IP2Lineup lineup = info.getLineup();
                        try
                        {
                            new NewLineupProjectOperation( lineup.getId(), lineup ).createProject( monitor, true );
                        }
                        catch ( CoreException coreException )
                        {
                            throw new InvocationTargetException( coreException );
                        }
                    }
                } );
            }

            if ( lineupToCodebasePage.isPublishingNeeded() )
            {
                final P2LineupSummaryDto[] summary = new P2LineupSummaryDto[1];

                getContainer().run( true, true, new IRunnableWithProgress()
                {
                    public void run( IProgressMonitor monitor )
                        throws InvocationTargetException, InterruptedException
                    {
                        String serverUrl = info.getServerUrl();
                        P2Lineup lineup = info.getLineup();
                        NexusLineupPublisher publisher = info.getPublisher();

                        try
                        {
                            summary[0] = publisher.publishLineup( serverUrl, lineup, monitor );
                        }
                        catch ( CoreException coreException )
                        {
                            throw new InvocationTargetException( coreException );
                        }
                    }
                } );

                if ( lineupToCodebasePage.isCodebaseNeeded() )
                {
                    P2LineupLocation location = new P2LineupLocation();
                    location.setUrl( summary[0].getRepositoryUrl() );
                    getProject().setP2LineupLocation( location );
                    return super.performFinish();
                }
            }
        }
        catch ( InvocationTargetException invocationTargetException )
        {
            t = invocationTargetException.getTargetException();
        }
        catch ( InterruptedException interruptedException )
        {
            t = interruptedException;
        }

        if ( t != null )
        {
            IStatus status;

            if ( t instanceof CoreException )
            {
                status = ( (CoreException) t ).getStatus();
            }
            else
            {
                status = new Status( IStatus.ERROR, Activator.PLUGIN_ID, Messages.newLineupWizard_error, t );
            }

            StatusManager.getManager().handle( status, StatusManager.LOG | StatusManager.BLOCK );
            return false;
        }
        return true;
    }

    @Override
    public void addPages()
    {
        lineupCoordinatesPage = new LineupCoordinatesPage( info );
        addPage( lineupCoordinatesPage );

        runtimeEnvironmentPage = new RuntimeEnvironmentPage( info );
        addPage( runtimeEnvironmentPage );

        iuRepoPage = new IURepoPage( info );
        addPage( iuRepoPage );

        super.addPages();
    }

    @Override
    protected void addCoordinatesPage()
    {
        lineupToCodebasePage = new LineupToCodebasePage( info, getProject() );
        addPage( lineupToCodebasePage );
    }

    @Override
    protected void addEclipsePage()
    {
        super.addEclipsePage();
        getEclipsePage().setLineupControlsEnabled( false );
    }

    @Override
    public boolean canFinish()
    {
        if ( getContainer().getCurrentPage() == lineupToCodebasePage && !lineupToCodebasePage.isCodebaseNeeded() )
        {
            return true;
        }

        return super.canFinish();
    }
}
