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
package com.sonatype.s2.nexus.securityrealm.persistence;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.authentication.AbstractSecurityRealmPersistence;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.authentication.ISecurityRealmPersistence;
import org.maven.ide.eclipse.authentication.ISecurityRealmURLAssoc;
import org.maven.ide.eclipse.authentication.SecurityRealmPersistenceException;
import org.maven.ide.eclipse.authentication.SecurityRealmURLAssoc;
import org.maven.ide.eclipse.authentication.internal.AuthRealm;
import org.maven.ide.eclipse.io.ByteArrayRequestEntity;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.maven.ide.eclipse.io.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.nexus.NexusFacade;
import com.sonatype.s2.securityrealm.model.IS2SecurityRealm;
import com.sonatype.s2.securityrealm.model.IS2SecurityRealmURLAssoc;
import com.sonatype.s2.securityrealm.model.S2AnonymousAccessType;
import com.sonatype.s2.securityrealm.model.S2SecurityRealm;
import com.sonatype.s2.securityrealm.model.S2SecurityRealmAuthenticationType;
import com.sonatype.s2.securityrealm.model.S2SecurityRealmURLAssoc;
import com.sonatype.s2.securityrealm.model.io.xstream.S2SecurityRealmXstreamIO;

public class NexusSecurityRealmPersistence
    extends AbstractSecurityRealmPersistence
    implements ISecurityRealmPersistence, IExecutableExtension
{
    private final static Logger log = LoggerFactory.getLogger( NexusSecurityRealmPersistence.class );

    private static final String NEXUS_REALMS_REST_URL_PART = "/service/local/mse/realms";

    private static final String NEXUS_URLS_REST_URL_PART = "/service/local/mse/urls";

    private static volatile boolean inUse = false;

    public static boolean isInUse()
    {
        return inUse;
    }

    public void addRealm( IAuthRealm realm, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        log.debug( "Adding nexus security realm id={}", realm.getId() );
        addOrUpdateRealm( realm, monitor );
    }

    private void addOrUpdateRealm( IAuthRealm realm, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        String url = getNexusURL();
        if ( url == null )
        {
            return;
        }

        url += NEXUS_REALMS_REST_URL_PART + "/" + realm.getId();
        log.debug( "nexus url: {}", url );

        IS2SecurityRealm s2Realm = toS2Realm( realm );
        ByteArrayRequestEntity entity =
            new ByteArrayRequestEntity( S2SecurityRealmXstreamIO.writeRealm( s2Realm ), "application/xml" );
        try
        {
            S2IOFacade.put( entity, url, null /* timeoutInMilliseconds */, monitor, "Adding or updating security realm" );
        }
        catch ( IOException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
        catch ( URISyntaxException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
    }

    private IS2SecurityRealm toS2Realm( IAuthRealm realm )
    {
        IS2SecurityRealm s2Realm = new S2SecurityRealm();
        s2Realm.setId( realm.getId() );
        s2Realm.setName( realm.getName() );
        s2Realm.setDescription( realm.getDescription() );
        s2Realm.setAuthenticationType( convert( realm.getAuthenticationType() ) );
        return s2Realm;
    }

    private IS2SecurityRealmURLAssoc toS2RealmURLAssoc( ISecurityRealmURLAssoc urlAssoc )
    {
        IS2SecurityRealmURLAssoc s2RealmURLAssoc = new S2SecurityRealmURLAssoc();
        s2RealmURLAssoc.setId( urlAssoc.getId() );
        s2RealmURLAssoc.setUrl( urlAssoc.getUrl() );
        s2RealmURLAssoc.setRealmId( urlAssoc.getRealmId() );
        s2RealmURLAssoc.setAnonymousAccess( convert( urlAssoc.getAnonymousAccess() ) );
        return s2RealmURLAssoc;
    }

    private S2AnonymousAccessType convert( AnonymousAccessType type )
    {
        return S2AnonymousAccessType.valueOf( type.toString() );
    }

    public void deleteRealm( String realmId, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        log.debug( "Deleting nexus security realm id={}", realmId );
        String url = getNexusURL();
        if ( url == null )
        {
            return;
        }

        url += NEXUS_REALMS_REST_URL_PART + "/" + realmId;
        log.debug( "nexus url: {}", url );

        try
        {
            S2IOFacade.delete( url, null /* timeoutInMilliseconds */, monitor, "Adding or updating security realm" );
        }
        catch ( IOException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
        catch ( URISyntaxException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
    }

    private String getNexusURL()
    {
        String url = NexusFacade.getMainNexusServerURL();
        log.info( "Nexus URL for security realm persistence: {}", url );
        if ( url == null )
        {
            // TODO Throw exception?
        }
        return url;
    }

    private static AuthenticationType convert( S2SecurityRealmAuthenticationType type )
    {
        return AuthenticationType.valueOf( type.toString() );
    }

    private static S2SecurityRealmAuthenticationType convert( AuthenticationType type )
    {
        return S2SecurityRealmAuthenticationType.valueOf( type.toString() );
    }

    public Set<IAuthRealm> getRealms( IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        String url = getNexusURL();

        Set<IAuthRealm> result = new LinkedHashSet<IAuthRealm>();
        if ( url == null )
        {
            return result;
        }

        url += NEXUS_REALMS_REST_URL_PART;
        try
        {
            InputStream is = S2IOFacade.openStream( url, monitor );
            try
            {
                List<IS2SecurityRealm> s2Realms = S2SecurityRealmXstreamIO.readRealmList( is );
                for ( IS2SecurityRealm s2Realm : s2Realms )
                {
                    result.add( new AuthRealm( s2Realm.getId(), s2Realm.getName(), s2Realm.getDescription(),
                                                         convert( s2Realm.getAuthenticationType() ) ) );
                }
                return result;
            }
            finally
            {
                is.close();
            }
        }
        catch ( RuntimeException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
        catch ( IOException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
        catch ( URISyntaxException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
    }

    public void updateRealm( IAuthRealm realm, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        log.debug( "Updating nexus security realm id={}", realm.getId() );
        addOrUpdateRealm( realm, monitor );
    }

    @Override
    public void setActive( boolean active )
    {
        super.setActive( active );
        inUse = active;
    }

    public ISecurityRealmURLAssoc addURLToRealmAssoc( ISecurityRealmURLAssoc securityRealmURLAssoc,
                                                      IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        log.debug( "Adding URL '{}' to nexus security realm id={}", securityRealmURLAssoc.getUrl(),
                   securityRealmURLAssoc.getRealmId() );
        String nexusURL = getNexusURL();
        if ( nexusURL == null )
        {
            return securityRealmURLAssoc;
        }

        nexusURL += NEXUS_URLS_REST_URL_PART;
        log.debug( "nexus url: {}", nexusURL );

        IS2SecurityRealmURLAssoc s2RealmURLAssoc = toS2RealmURLAssoc( securityRealmURLAssoc );
        ByteArrayRequestEntity entity =
            new ByteArrayRequestEntity( S2SecurityRealmXstreamIO.writeRealmURLAssoc( s2RealmURLAssoc ),
                                        "application/xml" );
        try
        {
            ServerResponse response =
                S2IOFacade.post( entity, nexusURL, null /* timeoutInMilliseconds */, monitor,
                             "Adding security realm to URL association" );
            s2RealmURLAssoc =
                S2SecurityRealmXstreamIO.readRealmURLAssoc( new ByteArrayInputStream( response.getResponseData() ) );
            log.debug( "New URL to realm assoc id={}", s2RealmURLAssoc.getId() );
            return new SecurityRealmURLAssoc( s2RealmURLAssoc.getId(), s2RealmURLAssoc.getUrl(),
                                                        s2RealmURLAssoc.getRealmId(),
                                                        convert( s2RealmURLAssoc.getAnonymousAccess() ) );
        }
        catch ( IOException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
        catch ( URISyntaxException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
    }

    private AnonymousAccessType convert( S2AnonymousAccessType type )
    {
        return AnonymousAccessType.valueOf( type.toString() );
    }

    public Set<ISecurityRealmURLAssoc> getURLToRealmAssocs( IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        String url = getNexusURL();

        Set<ISecurityRealmURLAssoc> result = new LinkedHashSet<ISecurityRealmURLAssoc>();
        if ( url == null )
        {
            return result;
        }

        url += NEXUS_URLS_REST_URL_PART;
        try
        {
            InputStream is = S2IOFacade.openStream( url, monitor );
            try
            {
                List<IS2SecurityRealmURLAssoc> s2urlAssocs = S2SecurityRealmXstreamIO.readRealmURLAssocList( is );
                for ( IS2SecurityRealmURLAssoc s2urlAssoc : s2urlAssocs )
                {
                    result.add( new SecurityRealmURLAssoc( s2urlAssoc.getId(), s2urlAssoc.getUrl(),
                                                                     s2urlAssoc.getRealmId(),
                                                                     convert( s2urlAssoc.getAnonymousAccess() ) ) );
                }
                return result;
            }
            finally
            {
                is.close();
            }
        }
        catch ( RuntimeException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
        catch ( IOException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
        catch ( URISyntaxException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
    }

    public void deleteURLToRealmAssoc( String urlAssocId, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        log.debug( "Deleting URL to nexus security realm id={}", urlAssocId );
        String nexusURL = getNexusURL();
        if ( nexusURL == null )
        {
            return;
        }

        nexusURL += NEXUS_URLS_REST_URL_PART + "/" + urlAssocId;
        log.debug( "nexus url: {}", nexusURL );

        try
        {
            S2IOFacade.delete( nexusURL, null /* timeoutInMilliseconds */, monitor,
                               "Deleting security realm to URL association" );
        }
        catch ( IOException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
        catch ( URISyntaxException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
    }

    public void updateURLToRealmAssoc( ISecurityRealmURLAssoc securityRealmURLAssoc, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        log.debug( "Updating URL '{}' to nexus security realm id={}", securityRealmURLAssoc.getUrl(),
                   securityRealmURLAssoc.getRealmId() );
        String nexusURL = getNexusURL();
        if ( nexusURL == null )
        {
            return;
        }

        nexusURL += NEXUS_URLS_REST_URL_PART + "/" + securityRealmURLAssoc.getId();
        log.debug( "nexus url: {}", nexusURL );

        IS2SecurityRealmURLAssoc s2RealmURLAssoc = toS2RealmURLAssoc( securityRealmURLAssoc );
        ByteArrayRequestEntity entity =
            new ByteArrayRequestEntity( S2SecurityRealmXstreamIO.writeRealmURLAssoc( s2RealmURLAssoc ),
                                        "application/xml" );
        try
        {
            S2IOFacade.put( entity, nexusURL, null /* timeoutInMilliseconds */, monitor,
                                "Updating security realm to URL association" );
        }
        catch ( IOException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
        catch ( URISyntaxException e )
        {
            log.debug( e.getMessage(), e );
            throw new SecurityRealmPersistenceException( e );
        }
    }
}
