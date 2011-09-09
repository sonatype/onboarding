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
package com.sonatype.s2.publisher.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.sonatype.s2.publisher.IS2Publisher;
import com.sonatype.s2.publisher.S2PublishRequest;
import com.sonatype.s2.publisher.S2PublisherConstants;
import com.sonatype.s2.publisher.internal.Messages;

public abstract class AbstractPublishAction
    implements IObjectActionDelegate, IEditorActionDelegate
{
    private List<IProject> projects;

    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    public void setActivePart( IAction action, IWorkbenchPart targetPart )
    {
    }

    /**
     * @see IActionDelegate#run(IAction)
     */
    public void run( IAction action )
    {
        if ( !checkOpenEditors() )
        {
            return;
        }

        S2PublishRequest request = new S2PublishRequest();
        for ( IProject project : projects )
        {
            request.addS2Project( project.getFolder( S2PublisherConstants.PMD_PATH ).getLocation() );
        }

        String title = getPublishWizardTitle();
        IS2Publisher publisher = getPublisher();

        PublishWizard wiz = new PublishWizard( title, publisher, request );

        WizardDialog pd = new WizardDialog( Display.getDefault().getActiveShell(), wiz )
        {
            protected Button createButton( Composite parent, int id, String label, boolean defaultButton )
            {
                Button supers = super.createButton( parent, id, label, defaultButton );
                if ( id == IDialogConstants.FINISH_ID )
                {
                    supers.setText( Messages.abstractPublishAction_publish );
                }
                return supers;
            }
        };

        pd.open();
    }

    /**
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    public void selectionChanged( IAction action, ISelection selection )
    {
        if ( selection instanceof IStructuredSelection )
        {
            projects = new ArrayList<IProject>();
            Iterator<?> iter = ( (IStructuredSelection) selection ).iterator();
            while ( iter.hasNext() )
            {
                Object obj = iter.next();
                IFile pmdFile = getSelectedFile( obj );
                if ( pmdFile != null )
                {
                    projects.add( pmdFile.getProject() );
                }
                IProject prj = getSelectedProject( obj );
                if ( prj != null )
                {
                    projects.add( prj );
                }
            }
            action.setEnabled( !projects.isEmpty() );
        }
    }

    private IFile getSelectedFile( Object obj )
    {
        if ( obj instanceof IAdaptable )
        {
            return (IFile) ( (IAdaptable) obj ).getAdapter( IFile.class );
        }
        return null;
    }

    private IProject getSelectedProject( Object obj )
    {
        if ( obj instanceof IAdaptable )
        {
            return (IProject) ( (IAdaptable) obj ).getAdapter( IProject.class );
        }
        return null;
    }

    public void setActiveEditor( IAction action, IEditorPart targetEditor )
    {
    }

    protected List<IProject> getProjects()
    {
        return projects;
    }

    protected abstract IS2Publisher getPublisher();

    protected abstract String getPublishWizardTitle();

    protected abstract boolean checkOpenEditors();
}
