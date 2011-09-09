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
package com.sonatype.s2.project.ui.materialization.update;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.maven.ide.eclipse.io.ForbiddenException;
import org.maven.ide.eclipse.io.UnauthorizedException;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputComposite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.model.CodebaseHelper;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.internal.composites.NexusUrlDialog;
import com.sonatype.s2.project.ui.materialization.Activator;
import com.sonatype.s2.project.validator.ValidationFacade;

public class CodebaseAccessChecker {
    private static final Logger log = LoggerFactory.getLogger( CodebaseAccessChecker.class );

	public static boolean checkAccess(String url, IProgressMonitor monitor)
			throws CoreException {
		// we load the descriptor twice but that's ok according to vlad
		IS2Project newProject = null;
		SubMonitor progress = SubMonitor.convert(monitor,
				"Checking access rights", 100);

		boolean doRepeat = true;
		while (doRepeat) {
			try {
				newProject = S2ProjectCore.getInstance().loadProject(url,
						progress.newChild(5, SubMonitor.SUPPRESS_NONE));
				doRepeat = false;
			} catch (CoreException exc) {
				// MECLIPSE-1839 - now catch AUTH and FORBIDDEN
				if (exc.getCause() instanceof UnauthorizedException
						|| exc.getCause() instanceof ForbiddenException) {
                    log.debug( "Failed loading codebase descriptor", exc );
					if (fixAccess(url, monitor)) {
						return true;
					}
				} else {
                    log.error( "Failed loading codebase descriptor", exc );
					throw exc;
				}
			}
		}

		// deal with access validation - MECLIPSE-1839
		monitor.setTaskName("Validating realm access");

		// does this mean the child progress will just consume the 7 ticks from
		// this progress??
		if (checkAccess(newProject, progress.newChild(7))) {
			// if access checker says "don't continue.. just exit..
			log.debug("AccessChecker says we shall cancel the update.");

			// we need to somehow mark the codebase update as cancelled -> to
			// really prevent
			// the update in CodebaseUpdateJob
			// MECLIPSE-1839
			return true;
		}
		return false;
	}

    private static boolean checkAccess( final IS2Project newProject, final IProgressMonitor monitor )
        throws CoreException
    {
        IStatus validation = ValidationFacade.getInstance().validateAccess( newProject, monitor );
        if ( !validation.isOK() )
        {
            // show the wizard now
            final IStatus fvalid = validation;
            final Boolean[] ret = new Boolean[1];
            ret[0] = Boolean.FALSE;
            Display.getDefault().syncExec( new Runnable()
            {
                public void run()
                {
                    UpdateCredsWizard wiz = new UpdateCredsWizard( newProject, fvalid );
                    WizardDialog wd =
                        new WizardDialog( Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
                                          wiz );
                    if ( wd.open() == IDialogConstants.CANCEL_ID )
                    {
                        // when cancelled, cancel the whole codebase
                        // update..
                        ret[0] = Boolean.TRUE;
                    }
                }
            } );
            return ret[0];
        }
        return false;
    }

    private static boolean fixAccess( final String descriptorUrl, final IProgressMonitor monitor )
        throws CoreException
    {
        final boolean[] ret = new boolean[1];
        ret[0] = false;
        final String nexusServerUrl = CodebaseHelper.getNexusServerUrlFromCodebaseUrl( descriptorUrl );
        Display.getDefault().syncExec( new Runnable()
        {
            public void run()
            {
                NexusUrlDialog dialog =
                    new NexusUrlDialog( Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
                                        "Update credentials to access codebase descriptor", nexusServerUrl,
                                        UrlInputComposite.READ_ONLY_URL );
                while ( true )
                {
                    if ( dialog.open() == IDialogConstants.CANCEL_ID )
                    {
                        // when canceled, cancel the whole codebase update..
                        ret[0] = true;
                        return;
                    }

                    IStatus nexusAccessValidationStatus = dialog.checkNexus( monitor );
                    if ( nexusAccessValidationStatus.isOK() )
                    {
                        return;
                    }
                    else
                    {
                        dialog.setErrorText( nexusAccessValidationStatus.getMessage() );
                    }
                }
            }
        } );
        return ret[0];
    }
}
