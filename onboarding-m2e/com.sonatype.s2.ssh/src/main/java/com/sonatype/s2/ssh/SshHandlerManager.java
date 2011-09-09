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
package com.sonatype.s2.ssh;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshHandlerManager
{

    private static final SshHandlerManager INSTANCE = new SshHandlerManager();

    private final Logger log = LoggerFactory.getLogger( SshHandlerManager.class );

    private List<SshHandler> handlers = new ArrayList<SshHandler>();

    private volatile File sshdir;

    public static SshHandlerManager getInstance()
    {
        return INSTANCE;
    }

    public SshHandlerManager()
    {
        String userHome = System.getProperty( "user.home" );
        if ( userHome != null )
        {
            sshdir = new File( userHome, ".ssh" );
        }
        log.debug( "Using SSH directory {}", sshdir );
    }

    /**
     * Gets the directory holding the user's SSH related files, usually {@code ~/.ssh}.
     * 
     * @return The (possibly not yet created) SSH directory or {@code null} if unknown.
     */
    public File getSshDirectory()
    {
        return sshdir;
    }

    /**
     * Sets the directory holding the user's SSH related files, usually {@code ~/.ssh}.
     * 
     * @param sshdir The SSH directory, may be {@code null}.
     */
    public void setSshDirectory( File sshdir )
    {
        this.sshdir = sshdir;
        log.debug( "Set SSH directory to {}", sshdir );
    }

    /**
     * Gets the currently active SSH handler.
     * 
     * @return The curretly active SSH handler or {@code null} if none.
     */
    public SshHandler getSshHandler()
    {
        synchronized ( handlers )
        {
            return handlers.isEmpty() ? null : handlers.get( 0 );
        }
    }

    public void addSshHandler( SshHandler handler )
    {
        log.debug( "Adding SSH handler {}", handler );
        synchronized ( handlers )
        {
            handlers.remove( handler );
            handlers.add( handler );
            Collections.sort( handlers, newComparator() );
            log.debug( "Registered SSH handlers {}", handlers );
        }
    }

    public void removeSshHandler( SshHandler handler )
    {
        log.debug( "Removing SSH handler {}", handler );
        synchronized ( handlers )
        {
            handlers.remove( handler );
            log.debug( "Registered SSH handlers {}", handlers );
        }
    }

    private Comparator<SshHandler> newComparator()
    {
        return new Comparator<SshHandler>()
        {
            public int compare( SshHandler h1, SshHandler h2 )
            {
                return h2.getPriority() - h1.getPriority();
            }
        };
    }

}
