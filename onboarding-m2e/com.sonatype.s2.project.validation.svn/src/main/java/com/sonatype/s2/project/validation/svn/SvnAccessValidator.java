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
package com.sonatype.s2.project.validation.svn;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSLAuthentication;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import com.sonatype.s2.project.validation.api.IScmAccessData;
import com.sonatype.s2.project.validation.api.IScmAccessValidator;
import com.sonatype.s2.project.validation.api.UnauthorizedStatus;

public class SvnAccessValidator
    implements IScmAccessValidator, IExecutableExtension
{

    private final Logger log = LoggerFactory.getLogger( SvnAccessValidator.class );

    private static final String SVN_SCM_ID = "scm:svn:";

    private int priority;

    static
    {
        SVNRepositoryFactoryImpl.setup();
        DAVRepositoryFactory.setup();
        FSRepositoryFactory.setup();
    }

    public boolean accept( IScmAccessData data )
    {
        return data.getRepositoryUrl() != null && data.getRepositoryUrl().startsWith( SVN_SCM_ID );
    }

    public boolean accept( String type )
    {
        return "svn".equalsIgnoreCase( type );
    }

    public int getPriority()
    {
        return priority;
    }

    public IStatus validate( IScmAccessData data, IProgressMonitor monitor )
    {
        log.debug( "Validating access to {} for user {}", data, data.getUsername() );

        final SVNErrorMessage[] authError = new SVNErrorMessage[1];

        try
        {
            String url = data.getRepositoryUrl().substring( SVN_SCM_ID.length() );
            final SVNURL svnUrl = SVNURL.parseURIDecoded( url );
            SVNRepository svnRepo = SVNRepositoryFactory.create( svnUrl );
            try
            {
                String username = null;
                String password = null;

                if ( svnUrl.getUserInfo() != null )
                {
                    username = svnUrl.getUserInfo();
                    int colon = username.indexOf( ':' );
                    if ( colon < 0 )
                    {
                        password = "";
                    }
                    else
                    {
                        password = username.substring( colon + 1 );
                        username = username.substring( 0, colon );
                    }
                }

                if ( data.getUsername() != null )
                {
                    if ( username == null || data.getUsername().equals( username ) )
                    {
                        username = data.getUsername();
                        password = data.getPassword();
                    }
                }

                ISVNAuthenticationManager authManager =
                    SVNWCUtil.createDefaultAuthenticationManager( SVNWCUtil.getDefaultConfigurationDirectory(),
                                                                  username, password, false );
                if ( authManager == null )
                {
                    throw new IllegalStateException( "Failed to create SVN DefaultAuthenticationManager" );
                }
                if ( username != null && username.trim().length() > 0 )
                {
                    // We have a non-empty username - force svn to use it
                    // (If the svn repository allows anonymous access, subversive falls back to anonymous if the
                    // provided credentials are not correct)
                    // Note: We should not use data.getAuthData().allowsAnonymousAccess() here!
                    if ( authManager instanceof DefaultSVNAuthenticationManager )
                    {
                        DefaultSVNAuthenticationManager defaultAuthManager =
                            (DefaultSVNAuthenticationManager) authManager;
                        defaultAuthManager.setAuthenticationForced( true );
                    }
                    else
                    {
                        log.warn( "The SVN Authentication Manager is NOT and instance of DefaultSVNAuthenticationManager: {}",
                                  authManager.getClass().getCanonicalName() );
                    }
                }
                if ( data.getAuthData().allowsCertificate() && data.getAuthData().getCertificatePath() != null )
                {
                    log.debug( "Validating access to {} using SSL certificate {}", data,
                               data.getAuthData().getCertificatePath() );
                    final SVNSSLAuthentication sslAuth =
                        new SVNSSLAuthentication( data.getAuthData().getCertificatePath(),
                                                  data.getAuthData().getCertificatePassphrase(),
                                                  false /* storageAllowed */);

                    authManager.setAuthenticationProvider( new ISVNAuthenticationProvider()
                    {
                        public SVNAuthentication requestClientAuthentication( String kind, SVNURL url, String realm,
                                                                              SVNErrorMessage errorMessage,
                                                                              SVNAuthentication previousAuth,
                                                                              boolean authMayBeStored )
                        {
                            if ( previousAuth != null )
                            {
                                // we already provided passphrase and it did not work
                                // since we do not have new passphrase, we save errorMessage so we can report proper
                                // status to the caller and return null to cancel authentication

                                authError[0] = errorMessage;
                                return null;
                            }

                            if ( ISVNAuthenticationManager.SSL.equals( kind ) && svnUrl.equals( url ) )
                            {
                                return sslAuth;
                            }

                            return null;
                        }

                        public int acceptServerAuthentication( SVNURL url, String realm, Object certificate,
                                                               boolean resultMayBeStored )
                        {
                            return ISVNAuthenticationProvider.ACCEPTED_TEMPORARY;
                        }
                    } );
                }
                svnRepo.setAuthenticationManager( authManager );
                svnRepo.getRepositoryRoot( true );
                svnRepo.getRepositoryUUID( true );
                SVNNodeKind svnNodeKind = svnRepo.checkPath( "", -1 );
                if ( svnNodeKind == null || SVNNodeKind.NONE.equals( svnNodeKind ) )
                {
                    SVNErrorMessage err =
                        SVNErrorMessage.create( SVNErrorCode.BAD_URL, "Bad URL: ''{0}'': Path does not exist",
                                                new Object[] { url } );
                    throw new SVNException( err );
                }
            }
            finally
            {
                svnRepo.closeSession();
            }

            return Status.OK_STATUS;
        }
        catch ( SVNException e )
        {
            if ( e instanceof SVNAuthenticationException )
            {
                return new UnauthorizedStatus( IStatus.ERROR, getClass().getName(), -1, e.getMessage(), e );
            }
            if ( e instanceof SVNCancelException && authError[0] != null )
            {
                return new UnauthorizedStatus( IStatus.ERROR, getClass().getName(), -1, authError[0].getMessage(), e );
            }
            return new Status( IStatus.ERROR, getClass().getName(), -1, e.getMessage(), e );
        }
    }

    public void setInitializationData( IConfigurationElement config, String propertyName, Object data )
        throws CoreException
    {
        String priority = config.getAttribute( "priority" );

        if ( priority != null )
        {
            try
            {
                this.priority = Integer.parseInt( priority );
            }
            catch ( Exception ex )
            {
                log.error( "Unable to parse priority for " + getClass().getName(), ex );
            }
        }
    }

}
