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

import static com.sonatype.s2.project.ui.catalog.LinkUtil.createHyperlinkEntry;

import java.util.Collection;

import org.eclipse.jface.action.Action;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction.MavenPathStorageEditorInput;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

import com.sonatype.s2.project.prefs.PreferenceGroup;
import com.sonatype.s2.project.ui.catalog.Messages;
import com.sonatype.s2.project.ui.codebase.editor.ICodebaseEditorInput;
import com.sonatype.s2.project.ui.codebase.editor.RemoteCodebaseEditorInput;

public class ProjectViewerEclipsePreferencesExtension
    extends AbstractProjectViewerExtension
{
    protected int pageIndex = -1;

    @Override
    public void createPages()
        throws PartInitException
    {
        ICodebaseEditorInput input = projectViewer.getCodebaseEditorInput();
        if ( input instanceof RemoteCodebaseEditorInput && input.getEclipsePreferenceGroups() != null )
        {
        	MavenPathStorageEditorInput storageInput = ( (RemoteCodebaseEditorInput) input ).getEclipsePreferences();
            if ( storageInput != null )
            {
                StructuredTextEditor editor = new StructuredTextEditor();
                editor.setEditorPart( projectViewer );
                pageIndex = projectViewer.addPage( editor, storageInput );
                projectViewer.setTabText( pageIndex, Messages.projectEditor_eclipsePreferences );
            }
        }
    }

    @Override
    public void createPageContent( ProjectViewerPage page, Composite body, FormToolkit toolkit )
    {
        Collection<PreferenceGroup> eclipsePreferenceGroups =
            projectViewer.getCodebaseEditorInput().getEclipsePreferenceGroups();
        if ( eclipsePreferenceGroups != null && !eclipsePreferenceGroups.isEmpty() )
        {
            Composite parent = page.getDetailsComposite();

            StringBuilder sb = new StringBuilder();
            for ( PreferenceGroup s : eclipsePreferenceGroups )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( ", " );
                }
                sb.append( s );
            }
            createHyperlinkEntry( parent, toolkit, Messages.projectEditor_details_eclipsePreferences,
                                  projectViewer.getProject().getEclipsePreferencesLocation().getUrl(), sb.toString(),
                                  new Action[] { new Action( Messages.projectEditor_eclipsePreferences_view )
                                  {
                                      public void run()
                                      {
                                          projectViewer.setActivePageByIndex( pageIndex );
                                      }
                                  } } );
        }
    }
}
