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

import java.net.URISyntaxException;

import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;

import com.sonatype.s2.project.validation.api.IScmAccessData;

public class GitUtil
{
    static
    {
        SshSessionFactory.setInstance( new DefaultSshSessionFactory() );
    }

    public static final String SCM_GIT_PREFIX = "scm:git:";

    public static URIish getUri( IScmAccessData data )
        throws URISyntaxException
    {
        String url = data.getRepositoryUrl();
        url = new GitURINormalizer().normalize( url );
        URIish uri = new URIish( url );

        if ( isProtocolAuthAware( uri.getScheme() ) && data.getUsername() != null && data.getUsername().length() > 0 )
        {
            if ( uri.getUser() == null || data.getUsername().equals( uri.getUser() ) )
            {
                uri = uri.setUser( data.getUsername() );
//                uri = uri.setPass( data.getPassword() );
            }
        }

        return uri;
    }

    private static boolean isProtocolAuthAware( String protocol )
    {
        return !"file".equalsIgnoreCase( protocol );
    }
}
