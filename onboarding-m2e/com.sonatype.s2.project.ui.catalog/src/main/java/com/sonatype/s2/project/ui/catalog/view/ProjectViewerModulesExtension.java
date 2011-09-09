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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.catalog.Messages;

public class ProjectViewerModulesExtension
    extends AbstractProjectViewerExtension
{
    @Override
    public void createPages()
        throws PartInitException
    {
        IS2Project project = projectViewer.getProject();
        for ( IS2Module module : project.getModules() )
        {
            ProjectModulePage page = new ProjectModulePage( projectViewer, module );
            projectViewer.addPage( page );
        }
    }

    public void createPageContent( ProjectViewerPage page, Composite body, FormToolkit toolkit )
    {
        if ( page instanceof ProjectOverviewPage )
        {
            // it probably would've been better to create this content directly in the OverviewPage,
            // but for the sake of the plug-in demo it's done here ;]
            Composite parent = page.getDetailsComposite();
            Label label = toolkit.createLabel( parent, Messages.projectEditor_modules_label );
            label.setLayoutData( new GridData( SWT.LEFT, SWT.TOP, false, false ) );

            Composite composite = toolkit.createComposite( parent );
            GridLayout gl = new GridLayout( 1, true );
            gl.verticalSpacing = 0;
            gl.marginHeight = 0;
            gl.marginWidth = 0;
            composite.setLayout( gl );
            composite.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );

            for ( final IS2Module module : projectViewer.getProject().getModules() )
            {
                Hyperlink link = toolkit.createHyperlink( composite, module.getName(), SWT.NONE );
                link.addHyperlinkListener( new HyperlinkAdapter()
                {
                    @Override
                    public void linkActivated( HyperlinkEvent e )
                    {
                        projectViewer.setActivePage( ProjectViewerPage.getPageId( module.getName() ) );
                    }
                } );
            }
        }
    }
}
