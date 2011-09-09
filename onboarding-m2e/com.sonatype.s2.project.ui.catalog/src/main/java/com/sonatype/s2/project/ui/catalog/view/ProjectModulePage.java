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

import java.util.List;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import com.sonatype.s2.project.model.ICIServerLocation;
import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.ui.catalog.Messages;
import com.sonatype.s2.project.ui.catalog.feeds.FeedsComposite;

public class ProjectModulePage
    extends ProjectViewerPage
{
    private IS2Module module;

    public ProjectModulePage( ProjectDescriptorViewer projectViewer, IS2Module module )
    {
        super( projectViewer, module.getName() );
        this.module = module;
    }

    @Override
    protected String createBodyContent( Composite body, FormToolkit toolkit )
    {
        super.createBodyContent( body, toolkit );

        createDetailsSection( body, toolkit );
        createFeedsSection( body, toolkit );

        return NLS.bind( Messages.projectEditor_modules_title, new Object[] { projectViewer.getProject().getName(),
            module.getName() } );
    }

    private void createDetailsSection( Composite parent, FormToolkit toolkit )
    {
        createHyperlinkEntry( detailsComposite, toolkit, Messages.projectEditor_details_projectHome,
                              module.getHomeUrl() );
        createHyperlinkEntry( detailsComposite, toolkit, Messages.projectEditor_details_scmLocation,
                              module.getScmLocation().getUrl() );
        createHyperlinkEntry( detailsComposite, toolkit, Messages.projectEditor_details_documentation,
                              module.getDocsUrl() );
        createHyperlinkEntry( detailsComposite, toolkit, Messages.projectEditor_details_issueTracking,
                              module.getIssuesUrl() );
        //MECLIPSE-1661 look into the ciServers list if no buildUrl is defined.
        String url = module.getBuildUrl();
        if (url == null) {
            List<ICIServerLocation> servers = module.getCiServers();
            if (!servers.isEmpty()) {
                ICIServerLocation loc = servers.get( 0 );
                url = loc.getUrl();
                url = url + (loc.getJobs().isEmpty() ? "" : ("/" + loc.getJobs().get( 0 )));  
            }
        }
        createHyperlinkEntry( detailsComposite, toolkit, Messages.projectEditor_details_builds, url );

        toolkit.createLabel( detailsComposite, Messages.projectEditor_details_workingSet );
        toolkit.createLabel( detailsComposite, module.getName() );
    }

    private void createFeedsSection( Composite parent, FormToolkit toolkit )
    {
        Section section = toolkit.createSection( parent, ExpandableComposite.TITLE_BAR );
        section.setText( Messages.projectEditor_feeds_title );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );

        FeedsComposite feedsComposite = new FeedsComposite( section );
        feedsComposite.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );
        feedsComposite.setFeeds( module.getFeeds() );
        section.setClient( feedsComposite );
        toolkit.adapt( feedsComposite );
    }
}
