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
package com.sonatype.s2.project.ui.codebase;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISaveableFilter;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.part.FileEditorInput;

import com.sonatype.s2.project.ui.codebase.editor.CodebaseDescriptorEditor;
import com.sonatype.s2.publisher.IS2Publisher;
import com.sonatype.s2.publisher.S2PublisherConstants;
import com.sonatype.s2.publisher.actions.AbstractPublishAction;
import com.sonatype.s2.publisher.nexus.NexusCodebasePublisher;

public class CodebasePublishAction
    extends AbstractPublishAction
{
    private static NexusCodebasePublisher publisher = new NexusCodebasePublisher();

    /**
     * Constructor for Action1.
     */
    public CodebasePublishAction()
    {
        super();
    }

    protected String getPublishWizardTitle()
    {
        int count = getProjects().size();
        String title =
            count == 1 ? Messages.publish_action_dialog_title
                            : MessageFormat.format( Messages.publish_action_dialog_title2, count );
        return title;
    }

    @Override
    protected IS2Publisher getPublisher()
    {
        return publisher;
    }
    
    protected boolean checkOpenEditors()
    {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
        IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
        final List<IWorkbenchPart> unsaved = new ArrayList<IWorkbenchPart>();
        for ( IProject project : getProjects() )
        {
            IFile file = project.getFile( S2PublisherConstants.PMD_PATH + "/" + S2PublisherConstants.PMD_FILENAME );

            IEditorReference[] editors =
                workbenchPage.findEditors( new FileEditorInput( file ), null, IWorkbenchPage.MATCH_INPUT );
            for ( IEditorReference reference : editors )
            {
                IEditorPart editor = reference.getEditor( false );
                if ( editor instanceof CodebaseDescriptorEditor && ( (CodebaseDescriptorEditor) editor ).hasErrors() )
                {
                    if ( !MessageDialog.openQuestion( workbenchWindow.getShell(),
                                                      Messages.publish_action_unresolvedErrors_title,
                                                      NLS.bind( Messages.publish_action_unresolvedErrors_message,
                                                                editor.getTitle() ) ) )
                    {
                        return false;
                    }
                }
                if ( reference.isDirty() )
                {
                    unsaved.add( reference.getEditor( false ) );
                }
            }
        }
        if ( unsaved.size() > 0 )
        {
            return workbench.saveAll( workbenchWindow, workbenchWindow, new ISaveableFilter()
            {
                public boolean select( Saveable saveable, IWorkbenchPart[] containingParts )
                {
                    for ( IWorkbenchPart part : containingParts )
                    {
                        if ( unsaved.contains( part ) )
                        {
                            return true;
                        }
                    }
                    return false;
                }
            }, true );
        }
        return true;
    }
}
