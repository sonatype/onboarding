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
package com.sonatype.s2.project.ui.codebase.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.maven.ide.eclipse.ui.common.editor.AbstractFileEditor;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.codebase.Messages;

public abstract class AbstractCodebaseEditor
    extends AbstractFileEditor
{
    private IS2Project project;

    private ICodebaseEditorInput codebaseEditorInput;

    @Override
    public void init( IEditorSite site, IEditorInput input )
        throws PartInitException
    {
        super.init( site, input );

        if ( input instanceof IFileEditorInput )
        {
            try
            {
                input = new WorkspaceCodebaseEditorInput( ( (IFileEditorInput) input ).getFile() );
            }
            catch ( CoreException e )
            {
                throw new PartInitException( Messages.errors_errorOpeningCodebaseDescriptor, e );
            }
        }

        if ( input instanceof ICodebaseEditorInput )
        {
            ICodebaseEditorInput projectEditorInput = (ICodebaseEditorInput) input;
            this.codebaseEditorInput = projectEditorInput;
            setProject( projectEditorInput.getProject() );
        }
        else
        {
            throw new PartInitException( Messages.errors_badInput );
        }
    }

    public void setCodebaseEditorInput( ICodebaseEditorInput input )
    {
        if ( getCodebaseEditorInput() == input )
        {
            return;
        }

        codebaseEditorInput = input;
        setInputWithNotify( input );

        for ( int i = getPageCount() - 1; i >= 0; i-- )
        {
            removePage( i );
        }
        setProject( input.getProject() );
        addPages();
        setActivePage( 0 );
    }

    public ICodebaseEditorInput getCodebaseEditorInput()
    {
        return codebaseEditorInput;
    }

    public void setProject( IS2Project project )
    {
        this.project = project;
        setPartName( codebaseEditorInput.getTitle() );
    }

    public IS2Project getProject()
    {
        return project;
    }

    public void setTabText( int index, String text )
    {
        setPageText( index, text );
    }

    public void setActivePageByIndex( int index )
    {
        setActivePage( index );
    }

    protected boolean isReadOnly()
    {
        return false;
    }

    protected static IStatus openEditor( IEditorInput input, String editorId )
    {
        IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        IEditorReference[] editors = workbenchPage.findEditors( null, editorId, IWorkbenchPage.MATCH_ID );

        boolean found = false;
        if ( editors != null && editors.length > 0 )
        {
            IEditorPart editorPart = editors[0].getEditor( true );
            if ( editorPart instanceof AbstractCodebaseEditor && ( (AbstractCodebaseEditor) editorPart ).isReadOnly()
                && input instanceof ICodebaseEditorInput )
            {
                ( (AbstractCodebaseEditor) editorPart ).setCodebaseEditorInput( (ICodebaseEditorInput) input );
                workbenchPage.activate( editorPart );
                found = true;
            }
        }
        if ( !found )
        {
            try
            {
                workbenchPage.openEditor( input, editorId );
            }
            catch ( PartInitException e )
            {
                return e.getStatus();
            }
        }

        return Status.OK_STATUS;
    }
}
