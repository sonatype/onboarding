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
package com.sonatype.s2.project.validation.git;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.validation.api.IScmAccessData;
import com.sonatype.s2.project.validation.api.IScmAccessValidator;
import com.sonatype.s2.project.validation.api.UnauthorizedStatus;

public class GitAccessValidator
    implements IScmAccessValidator, IExecutableExtension
{

    private final Logger log = LoggerFactory.getLogger( GitAccessValidator.class );

    private int priority;

    public boolean accept( IScmAccessData data )
    {
        return data.getRepositoryUrl() != null && data.getRepositoryUrl().startsWith( GitUtil.SCM_GIT_PREFIX );
    }

    public boolean accept( String type )
    {
        return "git".equalsIgnoreCase( type );
    }

    public int getPriority()
    {
        return priority;
    }

    public IStatus validate( IScmAccessData data, IProgressMonitor monitor )
    {
        log.debug( "Validating access to {} for {}", data, data.getUsername() );

        try
        {
            URIish uri = GitUtil.getUri( data );

            File tmpDir = File.createTempFile( "s2jgit", ".git" );
            tmpDir.delete();

            Repository repo = new FileRepository( tmpDir );

            JSchSecurityContext secCtx = JSchSecurityContext.enter( data.getAuthData() );
            try
            {
                Transport transport = Transport.open( repo, uri );
                try
                {
                    transport.setTimeout( 10 );
                    transport.openFetch().close();
                }
                finally
                {
                    transport.close();
                }
            }
            finally
            {
                secCtx.leave();
                repo.close();
            }
        }
        catch ( NoRemoteRepositoryException e )
        {
            return new MultiStatus( getClass().getName(), 0, new IStatus[] { new Status( IStatus.ERROR,
                                                                                         getClass().getName(),
                                                                                         e.getMessage(), e ) },
                                    "No repository exists at remote location.", null );
        }
        catch ( TransportException e )
        {
            if ( e.getCause() != null && e.getCause().getMessage() != null
                && ( e.getCause().getMessage().indexOf( "401" ) >= 0 || e.getCause().getMessage().endsWith( "Auth cancel" ) ) )
            {
                return new UnauthorizedStatus( IStatus.ERROR, getClass().getName(), e.getMessage(), e );
            }
            return new Status( IStatus.ERROR, getClass().getName(), e.getMessage(), e );
        }
        catch ( URISyntaxException e )
        {
            return new Status( IStatus.ERROR, getClass().getName(), e.getMessage(), e );
        }
        catch ( IOException e )
        {
            return new Status( IStatus.ERROR, getClass().getName(), e.getMessage(), e );
        }

        return Status.OK_STATUS;
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
