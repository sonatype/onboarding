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
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.ui.common.authentication.RealmUrlCollector;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.S2ProjectFacade;
import com.sonatype.s2.project.prefs.PreferenceGroup;
import com.sonatype.s2.project.ui.codebase.Activator;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.codebase.NewCodebaseProjectOperation;
import com.sonatype.s2.project.ui.codebase.editor.CodebaseDescriptorEditor;
import com.sonatype.s2.project.ui.codebase.editor.WorkspaceCodebaseEditorInput;

public class NewCodebaseWizard
    extends Wizard
    implements INewWizard
{
    private IS2Project project;

    private CoordinatesPage coordinatesPage;

    private CodebaseInfoPage codebaseInfoPage;

    private EclipsePage eclipsePage;

    private WorkspacePage workspacePage;

    private SourceTreesPage sourceTreesPage;

    @Override
    public boolean performFinish()
    {
        final Image image = codebaseInfoPage.getCodebaseImage();
        final Collection<PreferenceGroup> preferences = workspacePage.getPreferenceGroups();
        final RealmUrlCollector realmUrlCollector = new RealmUrlCollector();
        eclipsePage.saveRealms( realmUrlCollector );
        workspacePage.saveRealms( realmUrlCollector );

        try
        {
            getContainer().run( true, true, new IRunnableWithProgress()
            {
                public void run( IProgressMonitor monitor )
                    throws InvocationTargetException, InterruptedException
                {
                    try
                    {
                        IProject workspaceProject =
                            new NewCodebaseProjectOperation( project.getArtifactId(), project, null ).createProject( monitor );

                        IFile file =
                            workspaceProject.getFolder( IS2Project.PROJECT_DESCRIPTOR_PATH ).getFile( IS2Project.PROJECT_DESCRIPTOR_FILENAME );

                        final WorkspaceCodebaseEditorInput input = new WorkspaceCodebaseEditorInput( file, project );
                        input.setCodebaseImage( image );
                        input.setEclipsePreferenceGroups( preferences );
                        input.doSave( monitor );

                        realmUrlCollector.save( monitor );

                        Display.getDefault().asyncExec( new Runnable()
                        {
                            public void run()
                            {
                                CodebaseDescriptorEditor.openEditor( input );
                            }
                        } );
                    }
                    catch ( CoreException e )
                    {
                        throw new InvocationTargetException( e );
                    }
                }
            } );
        }
        catch ( InterruptedException e )
        {
            return false;
        }
        catch ( InvocationTargetException e )
        {
            IStatus status;
            Throwable cause = e.getCause();
            if ( cause instanceof CoreException )
            {
                status = ( (CoreException) cause ).getStatus();
            }
            else
            {
                status = new Status( IStatus.ERROR, Activator.PLUGIN_ID, cause.getMessage(), cause );
            }
            StatusManager.getManager().handle( status, StatusManager.BLOCK | StatusManager.LOG );
            return false;
        }

        return true;
    }

    public void init( IWorkbench workbench, IStructuredSelection selection )
    {
        setWindowTitle( Messages.newCodebaseWizard_title );
        setNeedsProgressMonitor( true );

        project = S2ProjectFacade.createProject( "com.mycompany", "codebase", "1.0-HEAD" );
    }

    @Override
    public void addPages()
    {
        addCoordinatesPage();
        addCodebaseInfoPage();
        addEclipsePage();
        addWorkspacePage();
        addSourceTreesPage();
    }

    protected void addCoordinatesPage()
    {
        coordinatesPage = new CoordinatesPage( project );
        addPage( coordinatesPage );
    }

    protected void addCodebaseInfoPage()
    {
        codebaseInfoPage = new CodebaseInfoPage( project );
        addPage( codebaseInfoPage );
    }

    protected void addEclipsePage()
    {
        eclipsePage = new EclipsePage( project );
        addPage( eclipsePage );
    }

    protected EclipsePage getEclipsePage()
    {
        return eclipsePage;
    }

    protected void addWorkspacePage()
    {
        workspacePage = new WorkspacePage( project );
        addPage( workspacePage );
    }

    protected void addSourceTreesPage()
    {
        sourceTreesPage = new SourceTreesPage( project );
        addPage( sourceTreesPage );
    }

    protected IS2Project getProject()
    {
        return project;
    }
}
