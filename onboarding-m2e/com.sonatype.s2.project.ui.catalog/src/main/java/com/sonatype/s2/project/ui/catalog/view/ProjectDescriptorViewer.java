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
package com.sonatype.s2.project.ui.catalog.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IS2CodebaseChangeEventListener;
import com.sonatype.s2.project.core.S2CodebaseChangeEvent;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.ui.catalog.Messages;
import com.sonatype.s2.project.ui.codebase.editor.AbstractCodebaseEditor;
import com.sonatype.s2.project.ui.codebase.editor.RemoteCodebaseEditorInput;
import com.sonatype.s2.project.ui.internal.Dialog;
import com.sonatype.s2.project.ui.internal.ProjectData;

public class ProjectDescriptorViewer
    extends AbstractCodebaseEditor
    implements IS2CodebaseChangeEventListener
{
    private static final Logger log = LoggerFactory.getLogger( ProjectDescriptorViewer.class );

    public static final String EXTENSION_ID = "com.sonatype.s2.project.ui.ProjectViewerExtension";

    public static final String ELEMENT_EXTENSION = "projectViewerExtension";

    public static final String ELEMENT_CLASS = "class";

    private List<IProjectViewerExtension> extensions;

    public ProjectDescriptorViewer()
    {
        loadExtensions();
        S2ProjectCore.getInstance().addWorkspaceCodebaseChangeListener( this );
    }

    @Override
    public void dispose()
    {
        S2ProjectCore.getInstance().removeWorkspaceCodebaseChangeListener( this );
        super.dispose();
    }

    @Override
    public void doSave( IProgressMonitor monitor )
    {
        monitor.done();
    }

    @Override
    public void doSaveAs()
    {
    }

    @Override
    public boolean isDirty()
    {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return false;
    }

    @Override
    protected boolean isReadOnly()
    {
        return true;
    }

    @Override
    protected void addPages()
    {
        try
        {
            ProjectOverviewPage overviewPage = new ProjectOverviewPage( this );
            addPage( overviewPage );

            for ( IProjectViewerExtension extension : extensions )
            {
                extension.createPages();
            }
        }
        catch ( PartInitException e )
        {
            Dialog.openCoreError( getSite().getShell(), Messages.projectEditor_errors_errorOpeningProject, e );
        }
    }

    private void loadExtensions()
    {
        extensions = new ArrayList<IProjectViewerExtension>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint( EXTENSION_ID );
        if ( configuratorsExtensionPoint != null )
        {
            IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
            for ( IExtension extension : configuratorExtensions )
            {
                IConfigurationElement[] elements = extension.getConfigurationElements();
                for ( IConfigurationElement element : elements )
                {
                    if ( element.getName().equals( ELEMENT_EXTENSION ) )
                    {
                        try
                        {
                            IProjectViewerExtension projectViewerExtension =
                                (IProjectViewerExtension) element.createExecutableExtension( ELEMENT_CLASS );
                            projectViewerExtension.setProjectViewer( this );
                            extensions.add( projectViewerExtension );
                        }
                        catch ( CoreException e )
                        {
                            log.error( Messages.projectEditor_errors_errorLoadingOverview, e );
                        }
                    }
                }
            }
        }
    }

    List<IProjectViewerExtension> getExtensions()
    {
        return extensions;
    }

    public static IStatus openEditor( IEditorInput input )
    {
        return openEditor( input, ProjectDescriptorViewer.class.getName() );
    }

    public static void openEditor( final String url )
    {
        Job job = new Job( NLS.bind( Messages.catalogView_jobs_loadingProjectDetails, url ) )
        {
            @Override
            public IStatus run( IProgressMonitor monitor )
            {
                try
                {
                    ProjectData projectData = new ProjectData( url );
                    projectData.load( monitor, true );

                    final RemoteCodebaseEditorInput input = new RemoteCodebaseEditorInput( projectData );
                    input.load( monitor );

                    final IStatus[] status = new IStatus[] { Status.OK_STATUS };
                    PlatformUI.getWorkbench().getDisplay().syncExec( new Runnable()
                    {
                        public void run()
                        {
                            status[0] = openEditor( input );
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

    public void codebaseChanged( S2CodebaseChangeEvent event )
    {
        String url = event.getCodebase().getDescriptorUrl();
        IEditorInput editorInput = getEditorInput();
        if ( editorInput instanceof RemoteCodebaseEditorInput
            && url.equals( ( (RemoteCodebaseEditorInput) editorInput ).getUrl() ) )
        {
            openEditor( url );
        }
    }
}
