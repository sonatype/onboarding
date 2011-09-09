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
package com.sonatype.s2.project.ui.materialization.wizards;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.ui.internal.Activator;
import com.sonatype.s2.project.ui.internal.Messages;

public class PreferenceExportWizard
    extends Wizard
    implements IExportWizard
{
    private PreferenceExportPage preferenceExportPage;

    @Override
    public boolean performFinish()
    {
        final PreferenceExportPage.ExportData exportData = preferenceExportPage.getExportData();

        new WorkspaceJob( Messages.exportWizard_jobs_exportingWorkspacePreferences )
        {
            @Override
            public IStatus runInWorkspace( IProgressMonitor monitor )
                throws CoreException
            {
                try
                {
                    SubMonitor progress =
                        SubMonitor.convert( monitor, Messages.exportWizard_jobs_exportingPreferences,
                                            1 + ( exportData.deploy ? 1 : 0 ) );

                    File file =
                        exportData.deploy ? File.createTempFile( "eclipse-preferences", ".jar" )
                                        : new File( exportData.fileName );

                    SubMonitor child = progress.newChild( 1 );
                    S2ProjectCore.getInstance().getPrefManager().exportPreferences( file, exportData.preferences, child );

                    if ( exportData.deploy )
                    {
                        progress.subTask( Messages.exportWizard_jobs_deployingPreferences );
                        child = progress.newChild( 1 );
                        S2ProjectCore.getInstance().getPrefManager().deployPreferences( file, exportData.uploadUrl,
                                                                                        child );
                        file.delete();
                    }

                    return Status.OK_STATUS;
                }
                catch ( IOException e )
                {
                    return new Status( IStatus.ERROR, Activator.PLUGIN_ID, Messages.exportWizard_errors_temporaryFile,
                                       e );
                }
            }
        }.schedule();

        return true;
    }

    public void init( IWorkbench workbench, IStructuredSelection selection )
    {
        setWindowTitle( Messages.exportWizard_title );
    }

    @Override
    public void addPages()
    {
        preferenceExportPage = new PreferenceExportPage();
        addPage( preferenceExportPage );
    }
}
