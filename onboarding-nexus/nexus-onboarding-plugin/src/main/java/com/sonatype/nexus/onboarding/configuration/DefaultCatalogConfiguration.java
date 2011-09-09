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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.ConfigurationIdGenerator;
import org.sonatype.nexus.util.configurationreader.ConfigurationHelper;
import org.sonatype.nexus.util.configurationreader.ConfigurationReader;
import org.sonatype.nexus.util.configurationreader.ConfigurationWritter;

import com.sonatype.nexus.onboarding.persist.model.CCatalog;
import com.sonatype.nexus.onboarding.persist.model.CCatalogConfiguration;
import com.sonatype.nexus.onboarding.persist.model.CCatalogEntry;
import com.sonatype.nexus.onboarding.persist.model.io.xpp3.NexusOnboardingCatalogsXpp3Reader;
import com.sonatype.nexus.onboarding.persist.model.io.xpp3.NexusOnboardingCatalogsXpp3Writer;

@Component( role = CatalogConfiguration.class )
public class DefaultCatalogConfiguration
    extends AbstractLogEnabled
    implements CatalogConfiguration
{

    @org.codehaus.plexus.component.annotations.Configuration( value = "${nexus-work}/conf/onboarding-catalogs.xml" )
    private File configurationFile;

    @Requirement
    private ConfigurationHelper cfgLoader;

    @Requirement( role = ConfigurationIdGenerator.class )
    private ConfigurationIdGenerator idGenerator;

    @Requirement( role = CatalogConfigurationValidator.class )
    private CatalogConfigurationValidator validator;

    private CCatalogConfiguration configuration;

    private ReentrantLock lock = new ReentrantLock();

    private CCatalogConfiguration getConfiguration()
    {
        if ( configuration != null )
        {
            return configuration;
        }

        configuration = new CCatalogConfiguration();
        configuration.setVersion( CCatalogConfiguration.MODEL_VERSION );

        configuration =
            cfgLoader.load( configuration, CCatalogConfiguration.MODEL_VERSION, configurationFile, lock,
                            new ConfigurationReader<CCatalogConfiguration>()
                            {
                                public CCatalogConfiguration read( Reader fr )
                                    throws IOException, XmlPullParserException
                                {
                                    return new NexusOnboardingCatalogsXpp3Reader().read( fr );
                                }
                            }, validator, null );

        save();

        return configuration;
    }

    public void addCatalogEntry( String catalogId, CCatalogEntry catalogEntry )
        throws InvalidConfigurationException
    {
        CCatalog catalog = readCatalog( catalogId );

        catalogEntry.setId( idGenerator.generateId() );

        ValidationResponse vr = validator.validateCatalogEntry( catalogEntry );
        if ( vr.getValidationErrors().size() > 0 )
        {
            throw new InvalidConfigurationException( vr );
        }

        lock.lock();

        try
        {

            catalog.addEntry( catalogEntry );
            save();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void createCatalog( CCatalog catalog )
        throws InvalidConfigurationException
    {
        lock.lock();

        try
        {
            ValidationResponse vr = validator.validateCatalog( catalog );
            if ( vr.getValidationErrors().size() > 0 )
            {
                throw new InvalidConfigurationException( vr );
            }

            getConfiguration().getCatalogs().add( catalog );

            save();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void createOrUpdateCatalog( CCatalog catalog )
        throws InvalidConfigurationException
    {
        ValidationResponse vr = validator.validateCatalog( catalog );
        if ( vr.getValidationErrors().size() > 0 )
        {
            throw new InvalidConfigurationException( vr );
        }

        lock.lock();

        try
        {

            CCatalog current = getCatalog( catalog.getId() );

            if ( current != null )
            {
                getConfiguration().getCatalogs().remove( current );
            }

            getConfiguration().getCatalogs().add( catalog );

            save();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void deleteCatalog( String catalogId )
        throws InvalidConfigurationException
    {
        lock.lock();

        try
        {
            for ( Iterator<CCatalog> iterator = getConfiguration().getCatalogs().iterator(); iterator.hasNext(); )
            {
                CCatalog catalog = iterator.next();

                if ( catalogId.equals( catalog.getId() ) )
                {
                    iterator.remove();
                    save();

                    return;
                }
            }

            throw new InvalidConfigurationException( "Unable to delete catalog '" + catalogId + "'. Catalog not found." );

        }
        finally
        {
            lock.unlock();
        }
    }

    public List<CCatalog> listCatalogs()
    {
        return Collections.unmodifiableList( getConfiguration().getCatalogs() );
    }

    public CCatalog readCatalog( String catalogId )
        throws InvalidConfigurationException
    {
        CCatalog catalog = getCatalog( catalogId );
        if ( catalog != null )
        {
            return catalog;
        }

        throw new InvalidConfigurationException( "No Catalog found with id: '" + catalogId + "'." );
    }

    private CCatalog getCatalog( String catalogId )
    {
        for ( CCatalog catalog : getConfiguration().getCatalogs() )
        {
            if ( catalogId.equals( catalog.getId() ) )
            {
                return catalog;
            }
        }

        return null;
    }

    private CCatalogEntry getCatalogEntry( String catalogId, String entryId )
    {
        CCatalog catalog = getCatalog( catalogId );
        if ( catalog == null )
        {
            return null;
        }

        for ( CCatalogEntry entry : catalog.getEntries() )
        {
            if ( entryId.equals( entry.getId() ) )
            {
                return entry;
            }
        }

        return null;
    }

    public void removeCatalogEntry( String catalogId, String catalogEntryId )
        throws InvalidConfigurationException
    {
        CCatalog catalog = readCatalog( catalogId );

        lock.lock();

        try
        {
            for ( Iterator<CCatalogEntry> iterator = catalog.getEntries().iterator(); iterator.hasNext(); )
            {
                CCatalogEntry entry = iterator.next();

                if ( catalogEntryId.equals( entry.getId() ) )
                {
                    iterator.remove();
                    save();

                    return;
                }
            }

            throw new InvalidConfigurationException( "Unable to delete catalog entry '" + catalogId
                + "'. Catalog entry not found." );

        }
        finally
        {
            lock.unlock();
        }

    }

    public void save()
    {
        cfgLoader.save( getConfiguration(), configurationFile, new ConfigurationWritter<CCatalogConfiguration>()
        {
            public void write( Writer fr, CCatalogConfiguration configuration )
                throws IOException
            {
                new NexusOnboardingCatalogsXpp3Writer().write( fr, configuration );
            }
        }, lock );
    }

    public void updateCatalogEntry( String catalogId, String entryId, CCatalogEntry entry )
        throws InvalidConfigurationException
    {
        ValidationResponse vr = validator.validateCatalogEntry( entry );
        if ( vr.getValidationErrors().size() > 0 )
        {
            throw new InvalidConfigurationException( vr );
        }

        lock.lock();

        try
        {

            CCatalog catalog = getCatalog( catalogId );
            CCatalogEntry current = getCatalogEntry( catalogId, entryId );

            if ( current != null )
            {
                catalog.getEntries().remove( current );
                catalog.getEntries().add( entry );

                save();
            }

        }
        finally
        {
            lock.unlock();
        }
    }

    public CCatalogEntry readCatalogEntry( String catalogId, String entryId )
        throws InvalidConfigurationException
    {
        CCatalogEntry entry = getCatalogEntry( catalogId, entryId );
        if ( entry != null )
        {
            return entry;
        }

        throw new InvalidConfigurationException( "No Catalog Entry found for: '" + catalogId + "'/'" + entryId + "'." );
    }

}
