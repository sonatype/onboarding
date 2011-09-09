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
package com.sonatype.s2.project.ui.materialization.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.IS2ProjectCatalogEntry;
import com.sonatype.s2.project.ui.internal.Images;
import com.sonatype.s2.project.ui.internal.Messages;
import com.sonatype.s2.project.ui.materialization.MaterializationJob;
import com.sonatype.s2.project.ui.materialization.wizards.S2ProjectMaterializationWizard;
import com.sonatype.s2.project.validation.api.IS2ProjectValidator;
import com.sonatype.s2.project.validator.ValidationFacade;

public class MaterializeAction
    extends Action
{
    private static final Logger log = LoggerFactory.getLogger( MaterializeAction.class );

    private IS2Project actionProject;

    private IS2ProjectCatalogEntry entry;

    public MaterializeAction()
    {
        super( Messages.actions_materializeProject_title, Images.MATERIALIZE_PROJECT_DESCRIPTOR );
        setToolTipText( Messages.actions_materializeProject_tooltip );
    }

    public void setProject( IS2Project project )
    {
        actionProject = project;
    }

    protected void setCatalogEntry( IS2ProjectCatalogEntry entry )
    {
        this.entry = entry;
    }

    @Override
    public void run()
    {
        final boolean projectIsSet = actionProject != null;

        if ( !projectIsSet && entry == null )
        {
            throw new IllegalStateException( Messages.actions_materializeProject_errors_illegalState );
        }

        Job job =
            new Job( NLS.bind( Messages.materializationWizard_jobs_projectMaterialization,
                               projectIsSet ? actionProject.getName() : entry.getName() ) )
            {
                @Override
                public IStatus run( IProgressMonitor monitor )
                {
                    try
                    {
                        S2ProjectCore core = S2ProjectCore.getInstance();

                        IS2Project project = actionProject;
                        if ( !projectIsSet )
                        {
                            monitor.beginTask( Messages.actions_materializeProject_tasks_loadingProjectDescriptor, 1 );
                            project = core.loadProject( entry, monitor );
                            monitor.worked( 1 );
                        }

                        monitor.beginTask( Messages.actions_materializeProject_tasks_validatingProjectRequirements, 1 );
                        IStatus status =
                            ValidationFacade.getInstance().validate( project,
                                                                     IS2ProjectValidator.NULL_VALIDATION_CONTEXT,
                                                                     monitor, true );

                        if ( monitor != null && monitor.isCanceled() )
                        {
                            return Status.CANCEL_STATUS;
                        }

                        if ( status.isOK() )
                        {
                            new MaterializationJob( project ).schedule();
                        }
                        else
                        {
                            final IWizard wizard = new S2ProjectMaterializationWizard( project, status );
                            Display.getDefault().asyncExec( new Runnable()
                            {
                                public void run()
                                {
                                    WizardDialog dialog =
                                        new WizardDialog(
                                                          PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                                                          wizard );
                                    dialog.open();
                                }
                            } );
                        }
                        return Status.OK_STATUS;
                    }
                    catch ( CoreException e )
                    {
                        log.error( Messages.actions_materializeProject_errors_materializationFailed, e );
                        return e.getStatus();
                    }
                }
            };
        job.setUser( true );
        job.schedule();
    }
}
