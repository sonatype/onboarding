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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.sonatype.s2.project.core.IS2CodebaseChangeEventListener;
import com.sonatype.s2.project.core.S2CodebaseChangeEvent;
import com.sonatype.s2.project.ui.materialization.update.CodebaseUpdateView;

public class CodebaseViewAction
    implements IViewActionDelegate, IS2CodebaseChangeEventListener
{
    private CodebaseUpdateView view;

    public void run( IAction action )
    {
        if ( view == null || view.getWorkspaceCodebase() == null )
        {
            return;
        }
        ProjectDescriptorViewer.openEditor( view.getWorkspaceCodebase().getDescriptorUrl() );
    }

    public void selectionChanged( IAction action, ISelection selection )
    {
    }

    public void init( IViewPart view )
    {
        if ( view instanceof CodebaseUpdateView )
        {
            this.view = (CodebaseUpdateView) view;
            this.view.addWorkspaceCodebaseChangeListener( this );
        }
    }

    public void codebaseChanged( S2CodebaseChangeEvent event )
    {
        IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        IEditorReference[] editors =
            workbenchPage.findEditors( null, ProjectDescriptorViewer.class.getName(), IWorkbenchPage.MATCH_ID );

        if ( editors != null && editors.length > 0 )
        {
            IEditorPart editorPart = editors[0].getEditor( true );
            if ( editorPart instanceof ProjectDescriptorViewer )
            {
                ( (ProjectDescriptorViewer) editorPart ).codebaseChanged( event );
            }
        }
    }

}
