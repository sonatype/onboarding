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
package com.sonatype.m2e.subversive;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.extension.options.IOptionProvider;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.SSLSettings;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.m2e.subversive.internal.NonInteractiveOptionProvider;

public class SubversiveHelper
{
    private static final Logger log = LoggerFactory.getLogger( SubversiveHelper.class );

    public static final String SVN_SCM_ID = "scm:svn:";

    private static final Integer optionProviderLock = new Integer( 0 );

    private static volatile int installNonInteractiveOptionProviderCount = 0;

    private static IOptionProvider originalOptionProvider;

    /**
     * Disables svn UI. Each call to this method should be paired with a call to restoreOptionProvider().
     */
    public static void installNonInteractiveOptionProvider()
    {
        synchronized ( optionProviderLock )
        {
            log.debug( "SubversiveHelper.installNonInteractiveOptionProvider called from thread {}, count={}",
                       Thread.currentThread().getName(), installNonInteractiveOptionProviderCount );
            installNonInteractiveOptionProviderCount++;
            SVNTeamPlugin svnTeamPlugin = SVNTeamPlugin.instance();
            IOptionProvider optionProvider = svnTeamPlugin.getOptionProvider();
            if ( optionProvider instanceof NonInteractiveOptionProvider )
            {
                return;
            }
            originalOptionProvider = optionProvider;
            optionProvider = new NonInteractiveOptionProvider( optionProvider );
            svnTeamPlugin.setOptionProvider( optionProvider );
        }
    }

    /**
     * Re-enables the svn UI. This method should be called only after a call to installNonInteractiveOptionProvider().
     */
    public static void restoreOptionProvider()
    {
        synchronized ( optionProviderLock )
        {
            log.debug( "SubversiveHelper.restoreOptionProvider called from thread {}, count={}",
                       Thread.currentThread().getName(), installNonInteractiveOptionProviderCount );
            if ( installNonInteractiveOptionProviderCount > 0 )
            {
                installNonInteractiveOptionProviderCount--;
            }
            else
            {
                IllegalStateException e = new IllegalStateException( "SVN option provider restored too many times" );
                log.warn( e.getMessage(), e );
                return;
            }
            if ( installNonInteractiveOptionProviderCount > 0 )
            {
                return;
            }

            SVNTeamPlugin.instance().setOptionProvider( originalOptionProvider );
            originalOptionProvider = null;
        }
    }

    /**
     * Returns true if the credentials where changed
     */
    public static boolean setCredentials( String scmUrl, IRepositoryLocation repositoryLocation )
        throws MalformedURLException, CoreException
    {
        if ( scmUrl.startsWith( SVN_SCM_ID ) )
        {
            scmUrl = scmUrl.substring( SVN_SCM_ID.length() );
        }
        log.debug( "Setting credentials for SVN URL: '{}', repository location URL: '{}'", scmUrl,
                   repositoryLocation.getUrl() );

        URL uri = SVNUtility.getSVNUrl( scmUrl );

        boolean credentialsChanged = false;
        String username = uri.getUserInfo();
        if ( username != null )
        {
            // The svn url contains a username
            String password = "";
            int colon = username.indexOf( ':' );
            if ( colon >= 0 )
            {
                password = username.substring( colon + 1 );
                username = username.substring( 0, colon );
            }

            if ( !eq( username, repositoryLocation.getUsername() ) || !eq( password, repositoryLocation.getPassword() ) )
            {
                repositoryLocation.setUsername( username );
                repositoryLocation.setPassword( password );
                repositoryLocation.setPasswordSaved( true );
                credentialsChanged = true;
            }
        }

        IAuthData authData = AuthFacade.getAuthService().select( scmUrl );
        if ( authData != null )
        {
            if ( authData.allowsUsernameAndPassword() )
            {
                if ( username == null )
                {
                    // The svn url does not contain a username
                    if ( !eq( authData.getUsername(), repositoryLocation.getUsername() )
                        || !eq( authData.getPassword(), repositoryLocation.getPassword() ) )
                    {
                        repositoryLocation.setUsername( authData.getUsername() );
                        repositoryLocation.setPassword( authData.getPassword() );
                        repositoryLocation.setPasswordSaved( true );
                        credentialsChanged = true;
                    }
                }
                else if ( eq( authData.getUsername(), username ) )
                {
                    if ( authData.getPassword() != null
                        && !eq( authData.getPassword(), repositoryLocation.getPassword() ) )
                    {
                        repositoryLocation.setPassword( authData.getPassword() );
                        repositoryLocation.setPasswordSaved( true );
                        credentialsChanged = true;
                    }
                }
            }

            if ( authData.allowsCertificate() )
            {
                // log.debug( "Using SSL certificate {}", info.getSSLCertificate().getAbsolutePath() );
                String certificatePath = null;
                if ( authData.getCertificatePath() != null )
                {
                    certificatePath = authData.getCertificatePath().getAbsolutePath();
                }
                SSLSettings sslSettings = repositoryLocation.getSSLSettings();
                if ( !eq( certificatePath, sslSettings.getCertificatePath() )
                    || !eq( authData.getCertificatePassphrase(), sslSettings.getPassPhrase() ) )
                {
                    sslSettings.setAuthenticationEnabled( true );
                    sslSettings.setCertificatePath( certificatePath );
                    sslSettings.setPassPhrase( authData.getCertificatePassphrase() );
                    credentialsChanged = true;
                }
            }
        }

        if ( credentialsChanged )
        {
            log.debug( "Credentials changed for SVN URL: '{}', repository location URL: '{}'", scmUrl,
                       repositoryLocation.getUrl() );
            // Force svn to "forget" any cached credentials for this location
            repositoryLocation.dispose();

            SVNTeamPlugin.instance().setLocationsDirty( true );
        }
        else
        {
            log.debug( "Credentials not changed for SVN URL: '{}', repository location URL: '{}'", scmUrl,
                       repositoryLocation.getUrl() );
        }
        return credentialsChanged;
    }

    private static <T> boolean eq( T a, T b )
    {
        return a != null ? a.equals( b ) : b == null;
    }
}
