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
package com.sonatype.s2.project.ui.codebase.wizard;

import java.io.InputStream;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputComposite;
import org.maven.ide.eclipse.ui.common.wizards.RemoteArtifactLookupPage;

import com.sonatype.nexus.onboarding.rest.dto.CatalogDTO;
import com.sonatype.nexus.onboarding.rest.dto.CatalogEntryDTO;
import com.sonatype.nexus.onboarding.rest.dto.xstream.CatalogDTOXstreamIO;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.internal.composites.NexusUrlComposite;

public class SelectCodebasePage
    extends RemoteArtifactLookupPage<CatalogEntryDTO>
{
    public static final String CODEBASES_RESOURCE_URI =
        "service/local/mse/codebases/" + IS2Project.PROJECT_REPOSITORY_ID; //$NON-NLS-1$

    private NexusUrlComposite nexusUrlComposite;

    protected SelectCodebasePage()
    {
        super( null );
        setLoadButtonText( Messages.selectCodebasePage_loadButton );
        setReadyToLoadMessage( Messages.selectCodebasePage_clickToLoad );
        setResourceLabelText( Messages.selectCodebasePage_availableDescriptors );
        setSelectMessage( Messages.selectCodebasePage_description );
        setServerName( Messages.selectCodebasePage_nexusServer );
        setTitle( Messages.selectCodebasePage_title );
    }

    public CatalogEntryDTO getSelectedProject()
    {
        return getSelection().getEntry();
    }

    @Override
    protected UrlInputComposite createUrlInputComposite( Composite parent )
    {
        nexusUrlComposite =
            new NexusUrlComposite( parent, null, getLoadButtonValidationGroup(), null, UrlInputComposite.ALLOW_ANONYMOUS );
        return nexusUrlComposite;
    }

    @Override
    protected Object loadResources( String url, IProgressMonitor monitor )
        throws Exception
    {
        IStatus status = nexusUrlComposite.checkNexus( monitor );
        if ( !status.isOK() )
        {
            throw new CoreException( status );
        }
        nexusUrlComposite.saveNexusUrl( monitor );

        if ( !url.endsWith( "/" ) ) //$NON-NLS-1$
        {
            url += '/';
        }
        url += CODEBASES_RESOURCE_URI;

        InputStream is = S2IOFacade.openStream( url, monitor );
        try
        {
            CatalogDTO catalog = CatalogDTOXstreamIO.deserialize( is, CatalogDTO.class );
            return createGroups( catalog.getEntries() );
        }
        finally
        {
            IOUtil.close( is );
        }
    }

    @Override
    protected String getArtifactId( CatalogEntryDTO entry )
    {
        return entry.getId();
    }

    @Override
    protected String getGroupId( CatalogEntryDTO entry )
    {
        return entry.getGroupId();
    }

    @Override
    protected String getVersion( CatalogEntryDTO entry )
    {
        return entry.getVersion();
    }
}
