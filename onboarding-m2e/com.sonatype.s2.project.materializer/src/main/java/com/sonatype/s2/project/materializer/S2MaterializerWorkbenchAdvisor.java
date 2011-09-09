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
package com.sonatype.s2.project.materializer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.internal.ide.application.IDEWorkbenchAdvisor;
import org.eclipse.ui.internal.ide.application.IDEWorkbenchWindowAdvisor;

import com.sonatype.s2.project.ui.catalog.Messages;
import com.sonatype.s2.project.ui.catalog.view.ProjectDescriptorViewer;
import com.sonatype.s2.project.ui.codebase.editor.RemoteCodebaseEditorInput;
import com.sonatype.s2.project.ui.internal.ProjectData;
import com.sonatype.s2.project.ui.materialization.MaterializationJob;

@SuppressWarnings( "restriction" )
class S2MaterializerWorkbenchAdvisor
    extends IDEWorkbenchAdvisor
{
    private String s2ProjectURL;

    public S2MaterializerWorkbenchAdvisor( String s2ProjectURL )
    {
        this.s2ProjectURL = s2ProjectURL;
    }

    @Override
    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor( IWorkbenchWindowConfigurer configurer )
    {
        return new IDEWorkbenchWindowAdvisor( this, configurer )
        {
            @Override
            public void openIntro()
            {
                Job job = new Job( NLS.bind( Messages.catalogView_jobs_loadingProjectDetails, s2ProjectURL ) )
                {
                    @Override
                    public IStatus run( IProgressMonitor monitor )
                    {
                        try
                        {
                            ProjectData projectData = new ProjectData( s2ProjectURL );
                            projectData.load( monitor, true );

                            final RemoteCodebaseEditorInput input = new RemoteCodebaseEditorInput( projectData );
                            input.load( monitor );

                            final IStatus[] status = new IStatus[] { Status.OK_STATUS };
                            PlatformUI.getWorkbench().getDisplay().syncExec( new Runnable()
                            {
                                public void run()
                                {
                                    status[0] = ProjectDescriptorViewer.openEditor( input );
                                }
                            } );
                            return status[0];
                        }
                        catch ( CoreException e )
                        {
                            return e.getStatus();
                        }
                    }
                };
                job.schedule();
            }
        };
    }

    @Override
    public void postStartup()
    {
        super.postStartup();
        MaterializationJob job2 = new MaterializationJob( s2ProjectURL );
        job2.schedule();
    }
}
