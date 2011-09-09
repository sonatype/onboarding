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

import org.eclipse.jgit.transport.URIish;
import org.maven.ide.eclipse.authentication.IURINormalizer;

public class GitURINormalizer
    implements IURINormalizer
{
    public boolean accept( String sUri )
    {
        if ( sUri == null )
        {
            return false;
        }

        return sUri.startsWith( GitUtil.SCM_GIT_PREFIX );
    }

    public String normalize( String sUri )
        throws URISyntaxException
    {
        if ( !sUri.startsWith( GitUtil.SCM_GIT_PREFIX ) )
        {
            return sUri;
        }

        sUri = sUri.substring( GitUtil.SCM_GIT_PREFIX.length() );
        if ( sUri.startsWith( "file:" ) && !sUri.startsWith( "file:///" ) )
        {
            throw new URISyntaxException( sUri, "Invalid git URI" );
        }

        URIish gitUri = new URIish( sUri );
        if ( gitUri.getScheme() == null )
        {
            if ( gitUri.getHost() == null || "file".equals( gitUri.getHost() ) )
            {
                gitUri = gitUri.setHost( null );
                gitUri = gitUri.setScheme( "file" );
            }
            else
            {
                // This must be an scp-like syntax git URL
                // See http://www.kernel.org/pub/software/scm/git/docs/git-clone.html#_git_urls_a_id_urls_a
                gitUri = gitUri.setScheme( "ssh" );
            }
        }
        return gitUri.toString();
    }
}
