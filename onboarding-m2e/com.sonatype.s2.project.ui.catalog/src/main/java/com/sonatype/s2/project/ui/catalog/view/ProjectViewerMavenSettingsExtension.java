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

import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

import com.sonatype.s2.project.model.IResourceLocation;
import com.sonatype.s2.project.ui.catalog.Messages;
import com.sonatype.s2.project.ui.codebase.editor.ICodebaseEditorInput;
import com.sonatype.s2.project.ui.codebase.editor.RemoteCodebaseEditorInput;

public class ProjectViewerMavenSettingsExtension
    extends AbstractProjectViewerExtension
{
    protected int pageIndex = -1;

    @Override
    public void createPages()
        throws PartInitException
    {
        ICodebaseEditorInput input = projectViewer.getCodebaseEditorInput();
        if ( input.getMavenSettings() != null )
        {
            StructuredTextEditor editor = new StructuredTextEditor();
            editor.setEditorPart( projectViewer );
            pageIndex = projectViewer.addPage( editor, input.getMavenSettings() );
            projectViewer.setTabText( pageIndex, RemoteCodebaseEditorInput.SETTINGS_XML );
        }
    }

    @Override
    public void createPageContent( ProjectViewerPage page, Composite body, FormToolkit toolkit )
    {
        final IResourceLocation mavenSettings = projectViewer.getProject().getMavenSettingsLocation();
        if ( mavenSettings != null && page instanceof ProjectOverviewPage )
        {
            Composite parent = page.getDetailsComposite();
            String url = mavenSettings.getUrl();

            createHyperlinkEntry( parent, toolkit, Messages.projectEditor_mavenSettings_label, url,
                                  NLS.bind( Messages.projectEditor_mavenSettings_hyperlink,
                                            projectViewer.getProject().getName() ),
                                  new Action[] { new Action( Messages.projectEditor_mavenSettings_view )
                                  {
                                      public void run()
                                      {
                                          projectViewer.setActivePageByIndex( pageIndex );
                                      }
                                  } } );
        }
    }
}
