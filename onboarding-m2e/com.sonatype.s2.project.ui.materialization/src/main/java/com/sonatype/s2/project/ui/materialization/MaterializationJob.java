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
package com.sonatype.s2.project.ui.materialization;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.internal.Messages;

public class MaterializationJob
    extends WorkspaceJob
{
    private final IS2Project project;

    private final String projectUrl;

    public MaterializationJob( IS2Project project )
    {
        this( project, null );
    }

    public MaterializationJob( String projectUrl )
    {
        this( null, projectUrl );
    }

    private MaterializationJob( IS2Project project, String projectUrl )
    {
        super( "" ); // proper name is set few lines below in this method  

        if ( ( project == null ) == ( projectUrl == null ) )
        {
            throw new IllegalArgumentException( "Either project or projectUrl but not both must be specified" );
        }

        this.project = project;
        this.projectUrl = projectUrl;

        String name = project != null? project.getName(): projectUrl;
        setName( NLS.bind( Messages.materializationWizard_jobs_projectMaterialization, name ) );

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        setRule( workspace.getRuleFactory().createRule( workspace.getRoot() ) );
        setUser( true );
    }

    @Override
    public IStatus runInWorkspace( IProgressMonitor monitor )
        throws CoreException
    {
        try
        {
            IS2Project project = this.project;
            if ( project == null )
            {
                project = S2ProjectCore.getInstance().loadProject( projectUrl, monitor );
            }

            S2ProjectCore.getInstance().materialize( project, true, monitor );
        }
        catch ( CoreException e )
        {
            return e.getStatus();
        }
        catch ( InterruptedException e )
        {
            throw new OperationCanceledException();
        }

        return Status.OK_STATUS;
    }
}
