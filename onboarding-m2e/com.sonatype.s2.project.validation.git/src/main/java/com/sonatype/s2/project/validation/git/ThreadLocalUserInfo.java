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

import org.maven.ide.eclipse.authentication.IAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.UserInfo;
import com.sonatype.s2.ssh.SshHandler;

class ThreadLocalUserInfo
    implements UserInfo
{
    private final Logger log = LoggerFactory.getLogger( ThreadLocalUserInfo.class );

    private int passphraseRequestCount;

    private int passowordRequestCount;

    private final SshHandler handler;

    public static final ThreadLocal<IAuthData> authInfo = new ThreadLocal<IAuthData>();

    public ThreadLocalUserInfo( SshHandler handler )
    {
        this.handler = handler;
    }

    public String getPassphrase()
    {
        IAuthData data = authInfo.get();
        if ( data == null )
        {
            return null;
        }

        return data.getCertificatePassphrase();
    }

    public String getPassword()
    {
        IAuthData data = authInfo.get();
        if ( data == null )
        {
            return null;
        }

        return data.getPassword();
    }

    public boolean promptPassword( String message )
    {
        IAuthData data = authInfo.get();
        if ( data == null )
        {
            return false;
        }

        if ( data.getPassword() == null )
        {
            return false;
        }

        if ( passowordRequestCount++ > 0 )
        {
            return false;
        }

        return true;
    }

    public boolean promptPassphrase( String message )
    {
        IAuthData data = authInfo.get();
        if ( data == null )
        {
            return false;
        }

        if ( data.getCertificatePassphrase() == null )
        {
            return false;
        }

        if ( passphraseRequestCount++ > 1 )
        {
            // the passphrase we have does not work, so we fail passphrase
            // request until we have new one
            return false;
        }

        return true;
    }

    public boolean promptYesNo( String message )
    {
        log.debug( "Prompting Yes/No: {}", message );
        boolean result = handler.promptYesNo( message );
        log.debug( "Result: {}", result );
        return result;
    }

    public void showMessage( String message )
    {
        log.debug( "Showing message: {}", message );
        handler.showMessage( message );
    }

}
