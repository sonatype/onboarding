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
package com.sonatype.nexus.onboarding.rest;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.component.annotations.Requirement;

import com.sonatype.nexus.onboarding.configuration.CatalogConfiguration;
import com.sonatype.nexus.onboarding.persist.model.CCatalog;
import com.sonatype.nexus.onboarding.persist.model.CCatalogEntry;
import com.sonatype.nexus.onboarding.rest.dto.CatalogDTO;
import com.sonatype.nexus.onboarding.rest.dto.CatalogEntryDTO;
import com.sonatype.nexus.onboarding.rest.dto.CatalogEntryRequest;
import com.sonatype.nexus.onboarding.rest.dto.CatalogRequest;
import com.sonatype.nexus.onboarding.rest.dto.CatalogResponse;

public abstract class AbstractOnboardingCatalogPlexusResource
    extends AbstractOnboardingPlexusResource
{
    protected static final String RESOURCE_URI_PART = "/mse/catalogs";

    @Requirement
    protected CatalogConfiguration catalogCfg;

    protected CatalogResponse toRest( List<CCatalog> listCatalogs )
    {
        CatalogResponse res = new CatalogResponse();
        for ( CCatalog cCatalog : listCatalogs )
        {
            res.addData( toRest( cCatalog ) );
        }
        return res;
    }

    protected CatalogDTO toRest( CCatalog cCatalog )
    {
        CatalogDTO req = new CatalogDTO();

        req.setId( cCatalog.getId() );
        req.setName( cCatalog.getName() );
        req.setRealm( cCatalog.getRealm() );
        req.setEntries( entryToRest( cCatalog.getEntries() ) );

        return req;
    }

    protected List<CatalogEntryDTO> entryToRest( List<CCatalogEntry> entries )
    {
        List<CatalogEntryDTO> rEntries = new ArrayList<CatalogEntryDTO>();
        for ( CCatalogEntry cEntry : entries )
        {
            rEntries.add( toRest( cEntry ) );
        }
        return rEntries;
    }

    protected CatalogEntryDTO toRest( CCatalogEntry cEntry )
    {
        CatalogEntryDTO entry = new CatalogEntryDTO();
        entry.setId( cEntry.getId() );
        entry.setName( cEntry.getName() );
        entry.setRealm( cEntry.getRealm() );
        entry.setType( cEntry.getType() );
        entry.setUrl( cEntry.getUrl() );
        return entry;
    }

    protected CCatalog toModel( CatalogRequest req )
    {
        return toModel( req.getData() );
    }

    protected CCatalogEntry toModel( CatalogEntryRequest req )
    {
        return toModel( req.getData() );
    }

    protected CCatalog toModel( CatalogDTO dto )
    {
        CCatalog catalog = new CCatalog();
        catalog.setId( dto.getId() );
        catalog.setName( dto.getName() );
        catalog.setRealm( dto.getRealm() );
        catalog.setEntries( toModel( dto.getEntries() ) );
        return catalog;
    }

    protected List<CCatalogEntry> toModel( List<CatalogEntryDTO> entries )
    {
        List<CCatalogEntry> cEntries = new ArrayList<CCatalogEntry>();
        for ( CatalogEntryDTO dto : entries )
        {
            cEntries.add( toModel( dto ) );
        }
        return cEntries;
    }

    protected CCatalogEntry toModel( CatalogEntryDTO dto )
    {
        CCatalogEntry model = new CCatalogEntry();
        model.setId( dto.getId() );
        model.setName( dto.getName() );
        model.setRealm( dto.getRealm() );
        model.setType( dto.getType() );
        model.setUrl( dto.getUrl() );
        return model;
    }
}
