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
package com.sonatype.s2.project.ui.lineup.composites;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.nexus.p2.rest.model.P2LineupError;
import com.sonatype.nexus.p2.rest.model.P2LineupRepositoryError;
import com.sonatype.nexus.p2.rest.model.P2LineupUnresolvedInstallableUnit;
import com.sonatype.s2.project.ui.internal.composites.ValidationStatusDialog;
import com.sonatype.s2.project.ui.lineup.Activator;
import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;
import com.sonatype.s2.publisher.nexus.P2LineupValidationStatus;

public class RemoteValidator
{
    private Logger log = LoggerFactory.getLogger( RemoteValidator.class );

    private RepositoryComposite repositoryComposite;

    private RootIUComposite rootIUComposite;

    public RemoteValidator( RepositoryComposite repositoryComposite, RootIUComposite rootIUComposite )
    {
        this.repositoryComposite = repositoryComposite;
        this.rootIUComposite = rootIUComposite;
    }

    public IStatus validate( NexusLineupPublishingInfo info, IProgressMonitor monitor )
    {
        IStatus status = info.getPublisher().validateLineup( info.getServerUrl(), info.getLineup(), monitor );

        List<P2LineupUnresolvedInstallableUnit> units = new ArrayList<P2LineupUnresolvedInstallableUnit>();
        List<P2LineupRepositoryError> repos = new ArrayList<P2LineupRepositoryError>();
        List<P2LineupValidationStatus> extraStates = null;
        String globalErrorMessage = null;
        int globalErrorLevel = IStatus.OK;

        if ( !status.isOK() )
        {
            boolean hasWarnings = hasOnlyWarnings( status );
            globalErrorLevel = hasWarnings ? IStatus.WARNING : IStatus.ERROR;
            if ( status.isMultiStatus() )
            {
                globalErrorMessage = hasWarnings ? Messages.remoteValidator_warnings : status.getMessage();

                extraStates = extractErrors( status.getChildren(), units, repos );

                if ( !repos.isEmpty() )
                {
                    globalErrorMessage =
                        !units.isEmpty() ? Messages.remoteValidator_proxyError : Messages.remoteValidator_proxyWarning;
                }
                else if ( !units.isEmpty() )
                {
                    globalErrorMessage = Messages.remoteValidator_iuError;
                }

                if ( globalErrorMessage == null || globalErrorMessage.trim().length() == 0 )
                {
                    globalErrorMessage = Messages.remoteValidator_unknownError;
                }
            }
            else
            {
                globalErrorMessage = status.getMessage();
            }
        }

        repositoryComposite.setErrors( repos, globalErrorMessage, globalErrorLevel );
        rootIUComposite.setErrors( units, globalErrorMessage, globalErrorLevel );

        if ( extraStates != null && extraStates.size() > 0 )
        {
            if ( extraStates.size() == 1 )
            {
                globalErrorMessage = extraStates.get( 0 ).getMessage();
            }
            else
            {
                final IStatus multi =
                    new MultiStatus( Activator.PLUGIN_ID, 0, extraStates.toArray( new IStatus[0] ), globalErrorMessage,
                                     null );
                Display.getDefault().asyncExec( new Runnable()
                {
                    public void run()
                    {
                        new ValidationStatusDialog( rootIUComposite.getShell(), multi ).open();
                    }
                } );
                return multi;
            }
        }

        if ( !status.isOK() )
        {
            return new Status( globalErrorLevel, Activator.PLUGIN_ID, globalErrorMessage );
        }

        return Status.OK_STATUS;
    }

    private boolean hasOnlyWarnings( IStatus status )
    {
        if ( status.isMultiStatus() )
        {
            for ( IStatus s : status.getChildren() )
            {
                if ( s instanceof P2LineupValidationStatus )
                {
                    P2LineupValidationStatus validationStatus = (P2LineupValidationStatus) s;
                    P2LineupError e = validationStatus.getError();
                    if ( !e.isWarning() )
                    {
                        return false;
                    }
                }
            }
            return true;
        }
        else
        {
            return status.getSeverity() == IStatus.WARNING;
        }
    }

    private List<P2LineupValidationStatus> extractErrors( IStatus[] children,
                                                          List<P2LineupUnresolvedInstallableUnit> units,
                                                          List<P2LineupRepositoryError> repos )
    {
        List<P2LineupValidationStatus> extras = new ArrayList<P2LineupValidationStatus>();

        if ( children != null )
        {
            for ( IStatus s : children )
            {
                if ( s instanceof P2LineupValidationStatus )
                {
                    P2LineupValidationStatus status = (P2LineupValidationStatus) s;
                    P2LineupError e = status.getError();
                    if ( e instanceof P2LineupUnresolvedInstallableUnit )
                    {
                        P2LineupUnresolvedInstallableUnit uie = (P2LineupUnresolvedInstallableUnit) e;
                        units.add( uie );
                    }
                    else if ( e instanceof P2LineupRepositoryError )
                    {
                        P2LineupRepositoryError ur = (P2LineupRepositoryError) e;
                        repos.add( ur );
                    }
                    else
                    {
                        extras.add( status );
                    }
                    log.error( "error validating lineup: " + status.getMessage(), status.getException() ); //$NON-NLS-1$
                }
                else
                {
                    log.error( "Error validation lineup, unknown status:" + s.getMessage() + " " + s.getClass() ); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }
        return extras;
    }
}
