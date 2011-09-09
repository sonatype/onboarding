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
package com.sonatype.m2e.egit.internal;

import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.team.TeamOperationResult;

public class AbstractSyncOperation
{
    protected static final Logger log = LoggerFactory.getLogger( AbstractSyncOperation.class );

    protected final Repository repository;

    protected final String branch;

    protected final String remoteName;

    protected final String remoteRefspec;

    protected AbstractSyncOperation( Repository repository )
        throws IOException
    {
        this.repository = repository;

        this.branch = repository.getBranch();

        Config config = repository.getConfig();

        this.remoteName = config.getString( "branch", branch, "remote" );
        this.remoteRefspec = config.getString( "branch", branch, "merge" );
    }

    protected void fetchFromRemote( RemoteConfig remoteConfig, IProgressMonitor monitor )
        throws NotSupportedException, TransportException
    {
        Transport transport = Transport.open( repository, remoteConfig );
        try
        {
            EclipseGitProgressTransformer pm = new EclipseGitProgressTransformer( monitor );
            transport.fetch( pm, null );
        }
        finally
        {
            transport.close();
        }
    }

    protected RemoteConfig getRemoteConfig( String remoteName )
        throws URISyntaxException
    {
        for ( RemoteConfig remoteConfig : RemoteConfig.getAllRemoteConfigs( repository.getConfig() ) )
        {
            if ( remoteName.equals( remoteConfig.getName() ) )
            {
                return remoteConfig;
            }
        }

        return null;
    }

    protected Ref getRemoteRef( String remoteName, String remoteRefspec )
        throws IOException
    {
        // TODO I am almost certain this does not cover many cases

        if ( !remoteRefspec.startsWith( Constants.R_HEADS ) )
        {
            return null;
        }

        String refspec = remoteRefspec.substring( Constants.R_HEADS.length() );

        return repository.getRef( Constants.R_REMOTES + remoteName + "/" + refspec );
    }

    protected TeamOperationResult notSupported( String message, String help, Object... params )
    {
        String _message = message != null ? NLS.bind( message, params ) : null;
        String _help = help != null ? NLS.bind( help, params ) : null;

        return new TeamOperationResult( IWorkspaceSourceTree.STATUS_NOT_SUPPORTED, _message, _help );
    }

}
