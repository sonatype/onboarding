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

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.catalog.Messages;
import com.sonatype.s2.project.ui.internal.Images;

public class ProjectOverviewPage
    extends ProjectViewerPage
{
    public ProjectOverviewPage( ProjectDescriptorViewer projectViewer )
    {
        super( projectViewer, projectViewer.getProject().getName() );
    }

    @Override
    protected String createBodyContent( Composite body, FormToolkit toolkit )
    {
        IS2Project project = projectViewer.getProject();

        Composite descriptionPanel = toolkit.createComposite( body );
        descriptionPanel.setLayout( new GridLayout( 2, false ) );
        descriptionPanel.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );

        Image image = projectViewer.getCodebaseEditorInput().getCodebaseImage();
        Label descriptionLabel = toolkit.createLabel( descriptionPanel, "" );
        descriptionLabel.setLayoutData( new GridData( SWT.LEFT, SWT.TOP, false, false ) );
        descriptionLabel.setImage( image == null ? Images.DEFAULT_PROJECT_IMAGE : image );

        String description = project.getDescription();
        Label descriptionText =
            toolkit.createLabel( descriptionPanel, description == null ? project.getName() : description, SWT.WRAP
                 );
        GridData gd = new GridData( SWT.FILL, SWT.TOP, true, false );
        gd.widthHint = 10;
        descriptionText.setLayoutData( gd );

        super.createBodyContent( body, toolkit );

        createHyperlinkEntry( detailsComposite, toolkit, Messages.projectEditor_details_projectHome,
                              project.getHomeUrl() );
        createHyperlinkEntry( detailsComposite, toolkit, Messages.projectEditor_details_documentation,
                              project.getDocsUrl() );
        if ( project.getP2LineupLocation() != null )
        {
            createHyperlinkEntry( detailsComposite, toolkit, Messages.projectEditor_details_p2Lineup,
                                  project.getP2LineupLocation().getUrl() );
        }

        return NLS.bind( Messages.projectEditor_overview_title, projectViewer.getProject().getName() );
    }

}
