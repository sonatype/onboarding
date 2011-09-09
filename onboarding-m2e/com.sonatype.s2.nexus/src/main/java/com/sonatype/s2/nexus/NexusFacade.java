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
package com.sonatype.s2.nexus;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.UnresolvedAddressException;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.authentication.SecurityRealmPersistenceException;
import org.maven.ide.eclipse.authentication.internal.AuthData;
import org.maven.ide.eclipse.authentication.internal.SimpleAuthService;
import org.maven.ide.eclipse.io.HttpBaseSupport.HttpInputStream;
import org.maven.ide.eclipse.io.HttpFetcher;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.maven.ide.eclipse.io.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.nexus.internal.Activator;
import com.sonatype.s2.nexus.internal.Messages;
import com.sonatype.s2.nexus.securityrealm.persistence.NexusSecurityRealmPersistence;

public class NexusFacade
{
    private final static Logger log = LoggerFactory.getLogger( NexusFacade.class );

    public static final String NEXUS_URL_PROPNAME = "nexus.baseUrl";

    public static final String ARG_NEXUS_BASE_URL = "-" + NexusFacade.NEXUS_URL_PROPNAME; //$NON-NLS-1$

    private static final String NEXUS_URL_PREF_NAME = "nexus.server.url"; //$NON-NLS-1$

    public static final String STATUS_RESOURCE_URI = "/service/local/status"; //$NON-NLS-1$

    public static void removeMainNexusServerURL()
    {
        log.debug( "Removing main Nexus server URL" ); //$NON-NLS-1$
        Activator.getDefault().getPluginPreferences().setValue( NEXUS_URL_PREF_NAME, "" );
    }

    public static void setMainNexusServerData( String url, String username, String password, IProgressMonitor monitor )
        throws CoreException
    {
        String oldUrl = getMainNexusServerURL();
        String oldUsername = null;
        String oldPassword = null;
        if ( oldUrl != null )
        {
            IAuthData oldAuthData = new SimpleAuthService( SecurePreferencesFactory.getDefault() ).select( oldUrl );
            if ( oldAuthData != null )
            {
                oldUsername = oldAuthData.getUsername();
                oldPassword = oldAuthData.getPassword();
            }
        }
        
        url = setMainNexusServerURL( url );

        if ( username != null )
        {
            new SimpleAuthService( SecurePreferencesFactory.getDefault() ).save( url, username, password );
        }

        if ( NexusSecurityRealmPersistence.isInUse() )
        {
            if ( !stringEquals( oldUrl, url ) || !stringEquals( oldUsername, username )
                || !stringEquals( oldPassword, password ) )
            {
                try
                {
                    AuthFacade.getAuthRegistry().reload( monitor );
                }
                catch ( SecurityRealmPersistenceException e )
                {
                    // Everything is supposed to work without security realms.
                    log.debug( e.getMessage(), e );
                    log.warn( e.getMessage() );
                    return;
                }
                IAuthRealm realm = AuthFacade.getAuthRegistry().getRealmForURI( url );
                if ( realm != null )
                {
                    // The nexus server url is associated with a realm defined on the same nexus server...
                    // Set the auth data for the realm to be the same as the auth data for the url.
                    IAuthData authData = new SimpleAuthService( SecurePreferencesFactory.getDefault() ).select( url );
                    realm.setAuthData( authData );
                }
            }
        }
    }

    private static boolean stringEquals( String s1, String s2 )
    {
        if ( s1 != null && s1.trim().length() == 0 )
        {
            s1 = null;
        }
        if ( s2 != null && s2.trim().length() == 0 )
        {
            s2 = null;
        }
        if ( s1 == null )
        {
            return s2 == null;
        }
        return s1.equals( s2 );
    }

    private static String setMainNexusServerURL( String url )
        throws CoreException
    {
        log.debug( "Setting main Nexus server URL to '{}'", url ); //$NON-NLS-1$

        if ( url == null || url.trim().length() == 0 )
        {
            IStatus status = new Status( Status.ERROR, Activator.PLUGIN_ID, Messages.nexusFacade_urlCannotBeNull );
            throw new CoreException( status );
        }

        url = url.trim();
        if ( url.endsWith( "/" ) ) //$NON-NLS-1$
        {
            url = url.substring( 0, url.length() - 1 );
        }
        Activator.getDefault().getPluginPreferences().setValue( NEXUS_URL_PREF_NAME, url );
        Activator.getDefault().savePluginPreferences();

        return url;
    }

    public static String getMainNexusServerURL()
    {
        String url = Activator.getDefault().getPluginPreferences().getString( NEXUS_URL_PREF_NAME );
        if ( url == null || url.trim().length() == 0 )
        {
            return null;
        }
        return url;
    }

    public static IStatus validateCredentials( String serverUrl, final String username, final String password,
                                               AnonymousAccessType annonymousAccessType,
                                               final IProgressMonitor monitor )
    {
        log.debug( "Validating credentials for Nexus server URL '{}': username='{}'", serverUrl, username );

        final URI url;
        try
        {
            url = new URI( cleanupUrl( serverUrl ) + STATUS_RESOURCE_URI );
        }
        catch ( URISyntaxException e )
        {
            // TODO this shall not be happening, we already have field level validators
            // for proper url
            return new Status( IStatus.ERROR, Activator.PLUGIN_ID, Messages.nexusFacade_cannotConstructUrl, e );
        }

        AuthFacade.getAuthService().save( serverUrl, new AuthData( username, password, annonymousAccessType ) );

        IProxyService proxyService = S2IOFacade.getProxyService();

        try
        {
            HttpInputStream is = new HttpFetcher().openStream( url, monitor, AuthFacade.getAuthService(), proxyService );
            try
            {
                // Xpp3Dom dom =
                // Throws 401 if the user/password is invalid
                // Throws 401 if user=anonymous and anonymous is not allowed on nexus
                Xpp3DomBuilder.build( is, is.getEncoding() );

                //                Xpp3Dom child = dom.getChild( "data" ); //$NON-NLS-1$
                // if ( child != null )
                // {
                //                    child = child.getChild( "clientPermissions" ); //$NON-NLS-1$
                // }
                // if ( child != null )
                // {
                //                    child = child.getChild( "loggedIn" ); //$NON-NLS-1$
                // }
                //
                // if ( child == null )
                // {
                //                    log.debug( "Illegal Nexus status response: " + dom.toString() ); //$NON-NLS-1$
                // return new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                // Messages.nexusFacade_unexpectedServerResponse );
                // }
                //
                // boolean loggedIn = Boolean.parseBoolean( child.getValue() );
                //
                // if ( !loggedIn )
                // {
                // return newLoginFailedStatus();
                // }
            }
            finally
            {
                IOUtil.close( is );
            }

            return Status.OK_STATUS;
        }
        catch ( XmlPullParserException e )
        {
            IStatus refusedConn = findConnectionException( e );
            if ( refusedConn != null )
            {
                return refusedConn;
            }

            if ( isAuthorizationException( e.getDetail() ) )
            {
                return newLoginFailedStatus();
            }
            if ( find404( e ) )
            {
                return error404Status();
            }

            return new Status( IStatus.ERROR, Activator.PLUGIN_ID, Messages.nexusFacade_couldNotValidateCredentials, e );
        }
        catch ( IOException e )
        {
        	if ( e instanceof ConnectException )
        	{
        		return newConnectExceptionStatus( (ConnectException) e );
        	}
            if ( isAuthorizationException( e ) )
            {
                return newLoginFailedStatus();
            }

            if ( find404( e ) )
            {
                return error404Status();
            }
            return new Status( IStatus.ERROR, Activator.PLUGIN_ID, Messages.nexusFacade_couldNotValidateCredentials, e );
        }
    }

    /**
     * server url checks.. cannot end with slash
     * 
     * @param serverUrl
     */
    private static String cleanupUrl( String serverUrl )
    {
        return serverUrl.endsWith( "/" ) ? serverUrl.substring( 0, serverUrl.length() - 1 ) : serverUrl; //$NON-NLS-1$
    }

    private static Status findConnectionException( XmlPullParserException ex )
    {
        // TODO once we upgrade to plexus-utils 2.0.6+, use getCause() call and reduce parameter type to Exception..
        if ( ex.getDetail() instanceof IOException && ex.getDetail().getCause() instanceof UnresolvedAddressException )
        {
            String error = ex.getDetail().getCause().getMessage();
            return new Status( IStatus.ERROR, Activator.PLUGIN_ID, error != null ? error
                            : Messages.nexusFacade_unresolvedAddress );
        }
        if ( ex.getDetail() instanceof ConnectException )
        {
        	return newConnectExceptionStatus((ConnectException) ex.getDetail());
        }
        if ( ex.getDetail() instanceof NoRouteToHostException )
        {
            // TODO we should not rely on exception message in UI.
            String error = ex.getDetail().getMessage();
            return new Status( IStatus.ERROR, Activator.PLUGIN_ID, error != null ? error
                            : Messages.nexusFacade_noRouteToHost );
        }
        return null;
    }

    private static boolean find404( Throwable exc )
    {
        Throwable e = exc;
        while ( e != null )
        {
            if ( e.getMessage() != null && e.getMessage().contains( "HTTP status code 404: Not Found:" ) ) //$NON-NLS-1$
            { // NOI18N
                return true;
            }
            e = e.getCause();
        }
        return false;

    }

    private static boolean isAuthorizationException( Throwable e )
    {
        Throwable cause = e;
        while ( cause != null && !( cause instanceof UnauthorizedException ) )
        {
            cause = cause.getCause();
        }
        return cause instanceof UnauthorizedException;
    }
    
    private static Status newConnectExceptionStatus(ConnectException e) 
    {
    	Throwable cause = getCause(e);
    	if (cause instanceof UnresolvedAddressException) 
    	{
    		return new Status( IStatus.ERROR, Activator.PLUGIN_ID, Messages.nexusFacade_unresolvedAddress );
    	}
        // TODO we should not rely on exception message in UI.
    	return new Status( IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage() != null ? e.getMessage() : Messages.nexusFacade_connectionFailed );
    }

    private static Status newLoginFailedStatus()
    {
        return new Status( IStatus.ERROR, Activator.PLUGIN_ID, Messages.nexusFacade_loginFailed );
    }

    private static Status error404Status()
    {
        return new Status( IStatus.ERROR, Activator.PLUGIN_ID, Messages.nexusFacade_noNexusThere );
    }
    
    private static Throwable getCause(Throwable e) 
    {
    	Throwable cause = e;
    	while (cause.getCause() != null && cause.getCause() != cause) 
    	{
    		cause = cause.getCause();
    	}
    	return cause;
    }
}
