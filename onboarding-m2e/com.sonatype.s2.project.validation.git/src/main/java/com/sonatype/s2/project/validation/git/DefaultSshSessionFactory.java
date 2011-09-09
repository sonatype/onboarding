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

import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshConfigSessionFactory;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.sonatype.s2.ssh.SshHandler;
import com.sonatype.s2.ssh.SshHandlerManager;

class DefaultSshSessionFactory
    extends SshConfigSessionFactory
{

    private final Logger log = LoggerFactory.getLogger( DefaultSshSessionFactory.class );

    @Override
    protected void configure( Host hc, Session session )
    {
        final SshHandler handler = SshHandlerManager.getInstance().getSshHandler();
        if ( handler != null )
        {
            log.debug( "Using SSH Handler {} for connection to {}", handler, hc.getHostName() );

            session.setUserInfo( new ThreadLocalUserInfo( handler ) );
        }
        else
        {
            log.debug( "No SSH Handler available for connection to {}", hc.getHostName() );
        }
    }

    @Override
    protected JSch getJSch( final OpenSshConfig.Host hc, FS fs )
        throws JSchException
    {
        final JSch jsch = new JSch();
        knownHosts( jsch );
        if ( hc.getIdentityFile() == null )
        {
            identities( jsch );
        }
        else
        {
            jsch.addIdentity( hc.getIdentityFile().getAbsolutePath() );
        }
        return jsch;
    }

    protected void knownHosts( final JSch jsch )
        throws JSchException
    {
        final File sshdir = SshHandlerManager.getInstance().getSshDirectory();
        if ( sshdir == null )
        {
            return;
        }
        final File known_hosts = new File( sshdir, "known_hosts" );
        jsch.setKnownHosts( known_hosts.getAbsolutePath() );
    }

    protected void identities( final JSch sch )
    {
        final File sshdir = SshHandlerManager.getInstance().getSshDirectory();
        if ( sshdir == null )
        {
            return;
        }
        if ( sshdir.isDirectory() )
        {
            loadIdentity( sch, new File( sshdir, "identity" ) );
            loadIdentity( sch, new File( sshdir, "id_rsa" ) );
            loadIdentity( sch, new File( sshdir, "id_dsa" ) );
        }
    }

    protected void loadIdentity( final JSch sch, final File priv )
    {
        if ( priv.isFile() )
        {
            try
            {
                sch.addIdentity( priv.getAbsolutePath() );
            }
            catch ( JSchException e )
            {
                // Instead, pretend the key doesn't exist.
            }
        }
    }

}
