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
package com.sonatype.nexus.onboarding.configuration;

import java.util.List;

import org.sonatype.configuration.validation.InvalidConfigurationException;

import com.sonatype.nexus.onboarding.persist.model.CCatalog;
import com.sonatype.nexus.onboarding.persist.model.CCatalogEntry;

public interface CatalogConfiguration
{

    void createCatalog( CCatalog catalog )
        throws InvalidConfigurationException;

    CCatalog readCatalog( String catalogId )
        throws InvalidConfigurationException;

    void createOrUpdateCatalog( CCatalog catalog )
        throws InvalidConfigurationException;

    void deleteCatalog( String catalogId )
        throws InvalidConfigurationException;

    List<CCatalog> listCatalogs();

    void addCatalogEntry( String catalogId, CCatalogEntry catalogEntry )
        throws InvalidConfigurationException;

    void removeCatalogEntry( String catalogId, String catalogEntryId )
        throws InvalidConfigurationException;

    void save();

    void updateCatalogEntry( String catalogId, String entryId, CCatalogEntry model )
        throws InvalidConfigurationException;

    CCatalogEntry readCatalogEntry( String catalogId, String entryId )
        throws InvalidConfigurationException;

}
