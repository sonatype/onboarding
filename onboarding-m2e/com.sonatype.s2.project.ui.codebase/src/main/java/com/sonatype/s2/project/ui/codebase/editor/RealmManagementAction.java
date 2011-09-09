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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.maven.ide.eclipse.ui.common.authentication.RealmManagementDialog;

public class RealmManagementAction
    implements IEditorActionDelegate
{
    private IEditorPart editorPart;

    public void setActiveEditor( IAction action, IEditorPart targetEditor )
    {
        editorPart = targetEditor;
    }

    public void run( IAction action )
    {
        if ( editorPart != null )
        {
            new RealmManagementDialog( editorPart.getEditorSite().getShell() ).open();
            CodebaseDescriptorEditor.updateAllCodebaseEditors();
        }
    }

    public void selectionChanged( IAction action, ISelection selection )
    {
    }
}
