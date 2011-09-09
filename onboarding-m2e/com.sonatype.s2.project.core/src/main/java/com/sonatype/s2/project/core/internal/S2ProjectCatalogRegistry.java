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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IS2ProjectCatalogRegistry;
import com.sonatype.s2.project.core.IS2ProjectCatalogRegistryEntry;
import com.sonatype.s2.project.core.IS2ProjectCatalogRegistryListener;
import com.sonatype.s2.project.model.IS2ProjectCatalog;
import com.sonatype.s2.project.model.IS2ProjectCatalogEntry;
import com.sonatype.s2.project.model.catalog.ProjectCatalogEntry;
import com.sonatype.s2.utils.BackgroundProcessingQueue;

public class S2ProjectCatalogRegistry
    implements IS2ProjectCatalogRegistry
{
    private final Logger log = LoggerFactory.getLogger( S2ProjectCatalogRegistry.class );

    public static final String DEFAULT_CATALOGS_PROP = "s2.catalogs";

    public static final String CATALOG_REGISTRY_FILENAME = "catalogs.lst";

    // public static final String TEST_PROJECT_CATALOG =
    // "https://svn.sonatype.com/repos/code/products/sonatype-studio/trunk/onboarding/sample-project-catalog/src/main/catalog";

    private static final String EOL = "\n";

    private final File registryFile;

    private final ArrayList<IS2ProjectCatalogRegistryListener> listeners =
        new ArrayList<IS2ProjectCatalogRegistryListener>();

    /**
     * Catalog registry. Use {@link #getRegistry(IProgressMonitor)} to access the registry.
     */
    private Map<String, IS2ProjectCatalogRegistryEntry> registry;

    private BackgroundProcessingQueue job = new BackgroundProcessingQueue( "Loading project catalog" )
    {
        public void schedule( Request request )
        {
            log.debug( "Loading project catalog..." );
            schedule( request, 0L );
        };
    };

    public S2ProjectCatalogRegistry( File basedir )
    {
        registryFile = new File( basedir, CATALOG_REGISTRY_FILENAME );
    }

    public List<IS2ProjectCatalog> getCatalogs( IProgressMonitor monitor )
        throws CoreException
    {
        List<IS2ProjectCatalog> list = new ArrayList<IS2ProjectCatalog>();
        for ( IS2ProjectCatalogRegistryEntry entry : getRegistry( monitor ).values() )
        {
            if ( entry.isLoaded() )
            {
                list.add( entry.getCatalog() );
            }
        }
        return list;
    }

    public List<IS2ProjectCatalogRegistryEntry> getCatalogEntries( IProgressMonitor monitor )
        throws CoreException
    {
        return new ArrayList<IS2ProjectCatalogRegistryEntry>( getRegistry( monitor ).values() );
    }

    private synchronized Map<String, IS2ProjectCatalogRegistryEntry> getRegistry( IProgressMonitor monitor )
        throws CoreException
    {
        if ( registry == null )
        {
            log.debug( "Initializing project catalog registry" );

            try
            {
                registry = new LinkedHashMap<String, IS2ProjectCatalogRegistryEntry>();
                if ( registryFile.canRead() )
                {
                    BufferedReader br =
                        new BufferedReader( new InputStreamReader( new FileInputStream( registryFile ) ) );
                    try
                    {
                        String urlStr;
                        while ( ( urlStr = br.readLine() ) != null )
                        {
                            IS2ProjectCatalogRegistryEntry entry = createEntry( urlStr, monitor );
                            registry.put( entry.getUrl(), entry );
                        }
                    }
                    finally
                    {
                        IOUtil.close( br );
                    }
                }
                else
                {
                    List<String> defaultCatalogURLs = getDefaultCatalogURLs();
                    for ( String catalogURL : defaultCatalogURLs )
                    {
                        IS2ProjectCatalogRegistryEntry entry = createEntry( catalogURL, monitor );
                        registry.put( entry.getUrl(), entry );
                    }
                }
            }
            catch ( IOException e )
            {
                throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                     "Could not initialize project catalog registry", e ) );
            }
        }
        return registry;
    }

    /** Creates an entry and attempts to load the catalog. */
    private IS2ProjectCatalogRegistryEntry createEntry( String url, IProgressMonitor monitor )
        throws CoreException
    {
        S2ProjectCatalogRegistryEntry entry = new S2ProjectCatalogRegistryEntry( url );
        entry.load( monitor );

        return entry;
    }

    public void addCatalog( final String urlStr )
    {
        job.schedule( new BackgroundProcessingQueue.Request()
        {
            @Override
            public void run( IProgressMonitor monitor )
                throws CoreException
            {
                addCatalog( urlStr, monitor );
            }
        } );
    }

    /**
     * Synchronously reads project catalog from the specified url, adds the new catalog to the registry, notifies
     * registry listeners and returns the new catalog if the load was successful.
     */
    public IS2ProjectCatalogRegistryEntry addCatalog( String url, IProgressMonitor monitor )
        throws CoreException
    {
        log.debug( "Adding catalog {}", url );

        IS2ProjectCatalogRegistryEntry entry = createEntry( url, monitor );
        registerCatalog( entry, monitor );

        log.debug( "Notifying listeners of addition of catalog {}", entry.getUrl() );

        ArrayList<IS2ProjectCatalogRegistryListener> _listeners;
        synchronized ( listeners )
        {
            _listeners = new ArrayList<IS2ProjectCatalogRegistryListener>( listeners );
        }

        for ( IS2ProjectCatalogRegistryListener listener : _listeners )
        {
            listener.catalogAdded( entry );
        }

        return entry;
    }

    private List<String> getDefaultCatalogURLs()
    {
        List<String> result = new ArrayList<String>();

        String defaultCatalogs = System.getProperty( DEFAULT_CATALOGS_PROP );
        log.debug( "{}={}", DEFAULT_CATALOGS_PROP, defaultCatalogs );
        if ( defaultCatalogs != null )
        {
            defaultCatalogs = defaultCatalogs.trim();
        }
        if ( defaultCatalogs == null || defaultCatalogs.length() == 0 )
        {
            log.info(
                      "There are no default catalogs. To set default catalogs, set the {} property in the eclipse.ini file.",
                      DEFAULT_CATALOGS_PROP );
            return result;
        }

        while ( defaultCatalogs != null && defaultCatalogs.length() != 0 )
        {
            String catalogURL;
            int commaAt = defaultCatalogs.indexOf( "|" );
            if ( commaAt > 0 )
            {
                catalogURL = defaultCatalogs.substring( 0, commaAt );
                defaultCatalogs = defaultCatalogs.substring( commaAt + 1 ).trim();
            }
            else
            {
                catalogURL = defaultCatalogs;
                defaultCatalogs = null;
            }

            log.debug( "Found default catalog URL: {}", catalogURL );
            result.add( catalogURL );
        }

        return result;
    }

    public boolean hasDefaultCatalogs()
    {
        return !getDefaultCatalogURLs().isEmpty();
    }

    public void addDefaultCatalogs()
    {
        List<String> defaultCatalogURLs = getDefaultCatalogURLs();
        for ( String catalogURL : defaultCatalogURLs )
        {
            log.info( "Adding default catalog from: {}", catalogURL );
            addCatalog( catalogURL );
        }
    }

    private synchronized void registerCatalog( IS2ProjectCatalogRegistryEntry entry, IProgressMonitor monitor )
        throws CoreException
    {
        Map<String, IS2ProjectCatalogRegistryEntry> registry = getRegistry( monitor );

        registry.put( entry.getUrl(), entry );

        writeRegistryFile( registry );
    }

    private void writeRegistryFile( Map<String, IS2ProjectCatalogRegistryEntry> registry )
        throws CoreException
    {
        log.debug( "Writing catalog registry file {}", registryFile );

        try
        {
            BufferedWriter bw = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( registryFile ) ) );

            try
            {
                for ( String urlStr : registry.keySet() )
                {
                    bw.write( urlStr + EOL );
                }
            }
            finally
            {
                IOUtil.close( bw );
            }
        }
        catch ( IOException e )
        {
            String message = "Could not write project catalog registry file";
            log.error( message, e );
            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID, message, e ) );
        }
    }

    public void addListener( IS2ProjectCatalogRegistryListener listener )
    {
        log.debug( "Adding catalog registry listener {}", listener );

        synchronized ( listeners )
        {
            listeners.add( listener );
        }
    }

    public void removeCatalog( final String urlStr )
    {
        job.schedule( new BackgroundProcessingQueue.Request()
        {
            @Override
            public void run( IProgressMonitor monitor )
                throws CoreException
            {
                removeCatalog( urlStr, monitor );
            }
        } );
    }

    public IS2ProjectCatalog removeCatalog( String urlStr, IProgressMonitor monitor )
        throws CoreException
    {
        log.debug( "Removing catalog {}", urlStr );

        Map<String, IS2ProjectCatalogRegistryEntry> registry = getRegistry( monitor );

        IS2ProjectCatalogRegistryEntry entry = registry.remove( urlStr );

        writeRegistryFile( registry );

        if ( entry != null )
        {
            log.debug( "Notifying listeners of removal of catalog {}", urlStr );

            ArrayList<IS2ProjectCatalogRegistryListener> _listeners;
            synchronized ( listeners )
            {
                _listeners = new ArrayList<IS2ProjectCatalogRegistryListener>( listeners );
            }

            for ( IS2ProjectCatalogRegistryListener listener : _listeners )
            {
                listener.catalogRemoved( entry );
            }

            if ( entry.isLoaded() )
            {
                return entry.getCatalog();
            }
        }

        return null;
    }

    public void removeListener( IS2ProjectCatalogRegistryListener listener )
    {
        log.debug( "Removing catalog registry listener {}", listener );

        synchronized ( listeners )
        {
            listeners.remove( listener );
        }
    }

    public String getEffectiveDescriptorUrl( IS2ProjectCatalogEntry catalogEntry )
        throws CoreException
    {
        if ( !( catalogEntry instanceof ProjectCatalogEntry ) )
        {
            throw new IllegalArgumentException();
        }

        return catalogEntry.getEffectiveDescriptorUrl();
    }

    public void purge()
    {
        log.debug( "Purging project catalog registry" );

        registry = null;
    }

}
