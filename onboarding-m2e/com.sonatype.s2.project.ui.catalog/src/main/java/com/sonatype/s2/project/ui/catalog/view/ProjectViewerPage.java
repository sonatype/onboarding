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
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import com.sonatype.s2.project.ui.catalog.Messages;
import com.sonatype.s2.project.ui.internal.Activator;

abstract public class ProjectViewerPage
    extends FormPage
{
    protected ProjectDescriptorViewer projectViewer;

    protected Composite detailsComposite;

    public ProjectViewerPage( ProjectDescriptorViewer projectViewer, String title )
    {
        super( projectViewer, getPageId( title ), title );
        this.projectViewer = projectViewer;
    }

    public static String getPageId( String suffix )
    {
        return Activator.PLUGIN_ID + ".s2project." + suffix;
    }

    @Override
    protected void createFormContent( IManagedForm managedForm )
    {
        FormToolkit toolkit = managedForm.getToolkit();
        ScrolledForm form = managedForm.getForm();
        toolkit.decorateFormHeading( form.getForm() );

        Composite body = form.getBody();
        GridLayout bodyLayout = new GridLayout( 1, true );
        body.setLayout( bodyLayout );

        String title = createBodyContent( body, toolkit );

        for ( IProjectViewerExtension extension : projectViewer.getExtensions() )
        {
            extension.createPageContent( this, body, toolkit );
        }

        form.setText( title );
    }

    protected String createBodyContent( Composite body, FormToolkit toolkit )
    {
        Section section = toolkit.createSection( body, ExpandableComposite.TITLE_BAR );
        section.setText( Messages.projectEditor_details_title );
        section.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );

        detailsComposite = toolkit.createComposite( section );
        detailsComposite.setLayout( new GridLayout( 2, false ) );
        section.setClient( detailsComposite );

        return null;
    }

    public Composite getDetailsComposite()
    {
        return detailsComposite;
    }
}
