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
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.ConfigurationIdGenerator;
import org.sonatype.nexus.util.configurationreader.ConfigurationHelper;
import org.sonatype.nexus.util.configurationreader.ConfigurationReader;
import org.sonatype.nexus.util.configurationreader.ConfigurationWritter;

import com.sonatype.nexus.onboarding.persist.model.CSecurityRealm;
import com.sonatype.nexus.onboarding.persist.model.CSecurityRealmURLAssoc;
import com.sonatype.nexus.onboarding.persist.model.CSecurityRealmsConfiguration;
import com.sonatype.nexus.onboarding.persist.model.io.xpp3.NexusOnboardingSecurityRealmsXpp3Reader;
import com.sonatype.nexus.onboarding.persist.model.io.xpp3.NexusOnboardingSecurityRealmsXpp3Writer;

@Component( role = SecurityRealmConfiguration.class )
public class DefaultSecurityRealmConfiguration
    extends AbstractLogEnabled
    implements SecurityRealmConfiguration
{
    @org.codehaus.plexus.component.annotations.Configuration( value = "${nexus-work}/conf/onboarding-security-realms.xml" )
    private File configurationFile;

    @Requirement
    private ConfigurationHelper cfgLoader;

    @Requirement( role = ConfigurationIdGenerator.class )
    private ConfigurationIdGenerator idGenerator;

    @Requirement( role = SecurityRealmConfigurationValidator.class )
    private SecurityRealmConfigurationValidator validator;

    @Requirement
    private Logger logger;

    private CSecurityRealmsConfiguration configuration;

    private ReentrantLock lock = new ReentrantLock();

    private CSecurityRealmsConfiguration getConfiguration()
    {
        if ( configuration != null )
        {
            return configuration;
        }

        configuration = new CSecurityRealmsConfiguration();
        configuration.setVersion( CSecurityRealmsConfiguration.MODEL_VERSION );

        configuration =
            cfgLoader.load( configuration, CSecurityRealmsConfiguration.MODEL_VERSION, configurationFile, lock,
                            new ConfigurationReader<CSecurityRealmsConfiguration>()
                            {
                                public CSecurityRealmsConfiguration read( Reader fr )
                                    throws IOException, XmlPullParserException
                                {
                                    return new NexusOnboardingSecurityRealmsXpp3Reader().read( fr );
                                }
                            }, validator, null );

        save();

        return configuration;
    }

    public void createOrUpdateRealm( CSecurityRealm realm )
        throws InvalidConfigurationException
    {
        logger.debug( "Creating or updating onboarding security realm with id=" + realm.getId() );
        ValidationResponse vr = validator.validateRealm( realm );
        if ( vr.getValidationErrors().size() > 0 )
        {
            throw new InvalidConfigurationException( vr );
        }

        lock.lock();

        try
        {
            CSecurityRealm current = getRealm( realm.getId() );

            if ( current != null )
            {
                getConfiguration().getRealms().remove( current );
            }

            getConfiguration().getRealms().add( realm );

            save();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void deleteRealm( String realmId )
        throws InvalidConfigurationException
    {
        logger.debug( "Deleting onboarding security realm with id=" + realmId );

        lock.lock();

        try
        {
            Iterator<CSecurityRealm> realmIter = getConfiguration().getRealms().iterator();
            while ( realmIter.hasNext() )
            {
                CSecurityRealm realm = realmIter.next();

                if ( realmId.equals( realm.getId() ) )
                {
                    // Found the realm - remove it
                    realmIter.remove();

                    // Remove all URLs associated with the realm we just removed
                    Iterator<CSecurityRealmURLAssoc> urlIter = getConfiguration().getUrls().iterator();
                    while ( urlIter.hasNext() )
                    {
                        CSecurityRealmURLAssoc urlAssoc = urlIter.next();
                        if ( realmId.equals( urlAssoc.getRealmId() ) )
                        {
                            urlIter.remove();
                        }
                    }

                    save();

                    return;
                }
            }

            throw new InvalidConfigurationException( "Unable to delete realm '" + realmId + "'. Realm not found." );
        }
        finally
        {
            lock.unlock();
        }
    }

    public List<CSecurityRealm> listRealms()
    {
        return Collections.unmodifiableList( getConfiguration().getRealms() );
    }

    public CSecurityRealm readRealm( String realmId )
        throws InvalidConfigurationException
    {
        CSecurityRealm realm = getRealm( realmId );
        if ( realm != null )
        {
            return realm;
        }

        throw new InvalidConfigurationException( "No realm found with id: '" + realmId + "'." );
    }

    private CSecurityRealm getRealm( String realmId )
    {
        for ( CSecurityRealm realm : getConfiguration().getRealms() )
        {
            if ( realmId.equals( realm.getId() ) )
            {
                return realm;
            }
        }

        return null;
    }

    private CSecurityRealmURLAssoc getURLAssocById( String urlId )
    {
        for ( CSecurityRealmURLAssoc url : getConfiguration().getUrls() )
        {
            if ( urlId.equals( url.getId() ) )
            {
                return url;
            }
        }

        return null;
    }

    private CSecurityRealmURLAssoc getURLAssocByURL( String url )
    {
        for ( CSecurityRealmURLAssoc urlAssoc : getConfiguration().getUrls() )
        {
            if ( urlsEquals( url, urlAssoc.getUrl() ) )
            {
                return urlAssoc;
            }
        }

        return null;
    }

    public void save()
    {
        logger.debug( "Saving configuration to file " + configurationFile.getAbsolutePath() );
        cfgLoader.save( getConfiguration(), configurationFile, new ConfigurationWritter<CSecurityRealmsConfiguration>()
        {
            public void write( Writer fr, CSecurityRealmsConfiguration configuration )
                throws IOException
            {
                new NexusOnboardingSecurityRealmsXpp3Writer().write( fr, configuration );
            }
        }, lock );
    }

    public CSecurityRealmURLAssoc readURL( String urlId )
        throws InvalidConfigurationException
    {
        CSecurityRealmURLAssoc url = getURLAssocById( urlId );
        if ( url != null )
        {
            return url;
        }

        throw new InvalidConfigurationException( "No URL found for id: '" + urlId + "'." );
    }

    public CSecurityRealmURLAssoc createURL( CSecurityRealmURLAssoc urlAssoc )
        throws InvalidConfigurationException
    {
        logger.debug( "Creating onboarding security realm to URL association: " + urlAssoc );
        ValidationResponse vr = validator.validateRealmURLAssoc( urlAssoc, true /* allowEmptyId */);
        if ( vr.getValidationErrors().size() > 0 )
        {
            throw new InvalidConfigurationException( vr );
        }

        lock.lock();
        // Validate the referenced realm exists
        readRealm( urlAssoc.getRealmId() );

        try
        {
            CSecurityRealmURLAssoc current = getURLAssocByURL( urlAssoc.getUrl() );

            if ( current != null )
            {
                throw new InvalidConfigurationException( "URL already exists: '" + urlAssoc.getUrl() + "'." );
            }

            urlAssoc.setId( idGenerator.generateId() );
            getConfiguration().getUrls().add( urlAssoc );

            save();
            logger.debug( "Created onboarding security realm to URL association: " + urlAssoc );

            return urlAssoc;
        }
        finally
        {
            lock.unlock();
        }
    }

    public void deleteURL( String urlId )
        throws InvalidConfigurationException
    {
        logger.debug( "Creating onboarding security realm to URL association id=" + urlId );
        lock.lock();

        try
        {
            Iterator<CSecurityRealmURLAssoc> urlIter = getConfiguration().getUrls().iterator();
            while ( urlIter.hasNext() )
            {
                CSecurityRealmURLAssoc urlAssoc = urlIter.next();

                if ( urlId.equals( urlAssoc.getId() ) )
                {
                    // Found the url - remove it
                    urlIter.remove();

                    save();

                    return;
                }
            }

            throw new InvalidConfigurationException( "Unable to delete URL with id '" + urlId + "'. URL id not found." );
        }
        finally
        {
            lock.unlock();
        }
    }

    public List<CSecurityRealmURLAssoc> listURLs()
    {
        return Collections.unmodifiableList( getConfiguration().getUrls() );
    }

    public void updateURL( CSecurityRealmURLAssoc urlAssoc )
        throws InvalidConfigurationException
    {
        logger.debug( "Updating onboarding security realm to URL association: " + urlAssoc );
        ValidationResponse vr = validator.validateRealmURLAssoc( urlAssoc );
        if ( vr.getValidationErrors().size() > 0 )
        {
            throw new InvalidConfigurationException( vr );
        }

        lock.lock();
        // Validate the referenced realm exists
        readRealm( urlAssoc.getRealmId() );

        try
        {
            CSecurityRealmURLAssoc current = getURLAssocById( urlAssoc.getId() );

            if ( current == null )
            {
                throw new InvalidConfigurationException( "Unable to update URL with id '" + urlAssoc.getId()
                    + "'. URL Id not found." );
            }

            getConfiguration().getUrls().remove( current );
            getConfiguration().getUrls().add( urlAssoc );

            save();
        }
        finally
        {
            lock.unlock();
        }
    }

    public static boolean urlsEquals( String url1, String url2 )
    {
        if ( url1.endsWith( "/" ) )
        {
            url1 = url1.substring( 0, url1.length() - 1 );
        }
        if ( url2.endsWith( "/" ) )
        {
            url2 = url2.substring( 0, url2.length() - 1 );
        }
        return url1.equals( url2 );
    }
}
