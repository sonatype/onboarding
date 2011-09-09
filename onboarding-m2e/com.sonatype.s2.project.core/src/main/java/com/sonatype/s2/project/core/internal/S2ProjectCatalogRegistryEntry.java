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
package com.sonatype.s2.project.core.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IS2ProjectCatalogRegistryEntry;
import com.sonatype.s2.project.model.IS2ProjectCatalog;
import com.sonatype.s2.project.model.S2ProjectFacade;

public class S2ProjectCatalogRegistryEntry
    implements IS2ProjectCatalogRegistryEntry
{

    private final Logger log = LoggerFactory.getLogger( S2ProjectCatalogRegistryEntry.class );

    private IS2ProjectCatalog catalog;

    private IStatus status;

    private String url;

    private boolean loaded = false;

    public S2ProjectCatalogRegistryEntry( String url )
    {
        this.url = url;
        if ( url.endsWith( "/" + IS2ProjectCatalog.CATALOG_FILENAME ) )
        {
            this.url = url.substring( 0, url.length() - IS2ProjectCatalog.CATALOG_FILENAME.length() );
        }
    }

    public IS2ProjectCatalog getCatalog()
    {
        return catalog;
    }

    public IStatus getStatus()
    {
        return status;
    }

    public String getUrl()
    {
        return url;
    }

    public boolean isLoaded()
    {
        return loaded;
    }

    /** Loads the catalog and updates the "loaded" flag, or sets an error status. */
    void load( IProgressMonitor monitor )
        throws CoreException
    {
        String urlStr = S2ProjectFacade.getCatalogFileUrl( url );
        log.debug( "Loading catalog registry entry {}", urlStr );

        String message = null;
        Exception cause = null;
        try
        {
            InputStream is = S2IOFacade.openStream( urlStr, monitor );
            try
            {
                IS2ProjectCatalog catalog = S2ProjectFacade.loadProjectCatalog( is );
                S2ProjectFacade.applyCatalogUrl( catalog, url );

                this.catalog = catalog;
                status = Status.OK_STATUS;
                loaded = true;
                return;
            }
            finally
            {
                IOUtil.close( is );
            }
        }
        catch ( IOException e )
        {
            message = "Error loading project catalog from " + urlStr;
            cause = e;
        }
        catch ( URISyntaxException e )
        {
            message = "Invalid URL " + urlStr;
            cause = e;
        }
        catch ( RuntimeException e )
        {
            message = e.getMessage();
            cause = e;
        }
        log.error( message, cause );
        status = new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID, message, cause );
        loaded = false;
    }
}
