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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.dialogs.ResolutionResultsWizardPage;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.QueryableMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.PreselectedIUInstallWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;

@SuppressWarnings( "restriction" )
public class P2PreselectedIUInstallWizard
    extends PreselectedIUInstallWizard
{
    private P2InstallWizardPage resolutionPage;

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

    public P2PreselectedIUInstallWizard( Policy policy, String profileId, IInstallableUnit[] initialSelections,
                                         PlannerResolutionOperation initialResolution,
                                         QueryableMetadataRepositoryManager manager )
    {
        super( policy, profileId, initialSelections, initialResolution, manager );
    }

    @Override
    protected ResolutionResultsWizardPage createResolutionPage()
    {
        resolutionPage = new P2InstallWizardPage( policy, profileId, root, resolutionOperation );
        return resolutionPage;
    }

    @Override
    public boolean performFinish()
    {
        boolean result = super.performFinish();
        
        exception = resolutionPage.getException();
        status = resolutionPage.getStatus();

        return result;
    }
}
