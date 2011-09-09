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
package com.sonatype.s2.project.validator.p2.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.dialogs.InstallWizardPage;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings( "restriction" )
class P2InstallWizardPage
    extends InstallWizardPage
{
    private static Logger log = LoggerFactory.getLogger( P2InstallWizardPage.class );

    private PlannerResolutionOperation resolutionOperation;

    private IStatus status;

    private ProvisionException exception;

    public ProvisionException getException()
    {
        return exception;
    }

    public IStatus getStatus()
    {
        return status;
    }

    public P2InstallWizardPage( Policy policy, String profileId, IUElementListRoot root,
                                PlannerResolutionOperation initialResolution )
    {
        super( policy, profileId, root, initialResolution );
        resolutionOperation = initialResolution;
    }

    @Override
    public boolean performFinish()
    {
        status = Status.CANCEL_STATUS;
        try
        {
            getContainer().run( true, true, new IRunnableWithProgress()
            {
                public void run( IProgressMonitor monitor )
                    throws InvocationTargetException, InterruptedException
                {
                    if ( resolutionOperation.getResolutionResult().getSummaryStatus().getSeverity() != IStatus.ERROR )
                    {
                        P2ProfileModificationOperation op =
                            new P2ProfileModificationOperation( getOperationLabel(), profileId,
                                                                resolutionOperation.getProvisioningPlan(),
                                                                resolutionOperation.getProvisioningContext() );
                        try
                        {
                            status = op.execute( monitor );
                        }
                        catch ( ProvisionException e )
                        {
                            exception = e;
                            status = e.getStatus();
                        }
                        // ProvisioningOperationRunner.schedule( op, StatusManager.SHOW | StatusManager.LOG );
                    }

                }
            } );
            if ( status.isOK() )
            {
                return true;
            }
        }
        catch ( InvocationTargetException e1 )
        {
            log.error( "Resolution error", e1 );
        }
        catch ( InterruptedException e1 )
        {
            log.error( "Resolution interrupted", e1 );
        }
        return false;
    }
}
