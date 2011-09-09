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
package com.sonatype.s2.project.core;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.sonatype.s2.project.model.IS2ProjectCatalog;
import com.sonatype.s2.project.model.IS2ProjectCatalogEntry;

public interface IS2ProjectCatalogRegistry
{
    /**
     * @return List<IS2ProjectCatalog> all known project catalogs
     */
    public List<IS2ProjectCatalog> getCatalogs( IProgressMonitor monitor )
        throws CoreException;
    
    /** Returns all listed catalogs, including the broken ones. */
    public List<IS2ProjectCatalogRegistryEntry> getCatalogEntries( IProgressMonitor monitor )
      throws CoreException;

    /**
     * Adds the default project catalogs to the registry. The default catalogs are specified as a comma separated list
     * of urls in the s2.catalogs system property.
     */
    public void addDefaultCatalogs();

    /**
     * Checks whether any default catalogs have been configured.
     * 
     * @return {@code true} if at least one default catalog is configured, {@code false} otherwise.
     */
    public boolean hasDefaultCatalogs();

    /**
     * Adds project catalog from specified url to the registry. This method returns immediately and actual processing
     * happens in a background job. Registry listeners are notified are catalog is loaded and the registry is fully
     * updated.
     */
    public void addCatalog( String url );

    /**
     * Removes project catalog from specified url from the registry. This method returns immediately and actual
     * processing happens in a background job. Registry listeners are notified are catalog is loaded and the registry is
     * fully updated.
     */
    public void removeCatalog( String url );
    
    /** Purges the catalog cache, so next time it will be loaded again. */
    public void purge();

    public void addListener( IS2ProjectCatalogRegistryListener listener );

    public void removeListener( IS2ProjectCatalogRegistryListener listener );

    public String getEffectiveDescriptorUrl( IS2ProjectCatalogEntry catalogEntry )
        throws CoreException;
}
