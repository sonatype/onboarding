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

import org.eclipse.jgit.transport.SshSessionFactory;
import org.maven.ide.eclipse.authentication.IAuthData;

public class JSchSecurityContext
{

    private final SshSessionFactory oldSshSessionFactory;

    private JSchSecurityContext()
    {
        oldSshSessionFactory = SshSessionFactory.getInstance();
        if ( !( oldSshSessionFactory instanceof DefaultSshSessionFactory ) )
        {
            SshSessionFactory.setInstance( new DefaultSshSessionFactory() );
        }
    }

    public static JSchSecurityContext enter( IAuthData authData )
    {
        ThreadLocalUserInfo.authInfo.set( authData );
        return new JSchSecurityContext();
    }

    public void leave()
    {
        SshSessionFactory.setInstance( oldSshSessionFactory );
        ThreadLocalUserInfo.authInfo.set( null );
    }

}
