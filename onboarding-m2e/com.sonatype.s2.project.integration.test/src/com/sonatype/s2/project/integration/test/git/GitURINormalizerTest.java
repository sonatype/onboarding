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
package com.sonatype.s2.project.integration.test.git;

import java.net.URISyntaxException;

import junit.framework.TestCase;

import com.sonatype.s2.project.validation.git.GitURINormalizer;

// Git urls documentation: http://www.kernel.org/pub/software/scm/git/docs/git-clone.html#_git_urls_a_id_urls_a
public class GitURINormalizerTest
    extends TestCase
{
    private void testURI( String sUri, boolean accept, String result )
        throws URISyntaxException
    {
        GitURINormalizer normalizer = new GitURINormalizer();
        assertEquals( "Test accept for URI=" + sUri, accept, normalizer.accept( sUri ) );
        if ( accept )
        {
            assertEquals( "Test normalize for URI=" + sUri, result, normalizer.normalize( sUri ) );
        }
    }

    public void testFileUrls()
        throws Exception
    {
        // Valid urls
        testURI( "scm:git:/tmp", true, "file:///tmp" );
        testURI( "scm:git:file:///tmp", true, "file:///tmp" );

        // Invalid urls
        try
        {
            testURI( "scm:git:file:tmp", true, null );
            fail( "Expected URISyntaxException" );
        }
        catch ( URISyntaxException expected )
        {
        }
        try
        {
            testURI( "scm:git:file:/tmp", true, "file:///tmp" );
            fail( "Expected URISyntaxException" );
        }
        catch ( URISyntaxException expected )
        {
        }
        try
        {
            testURI( "scm:git:file://tmp", true, "file:///tmp" );
            fail( "Expected URISyntaxException" );
        }
        catch ( URISyntaxException expected )
        {
        }
    }

    public void testNoScmGitPrefix()
        throws Exception
    {
        testURI( "http://foo", false, null );
    }
    
    public void testScpLikeUrl()
        throws Exception
    {
        testURI( "scm:git:github.com:sonatype/sonatype-tycho.git", true, "ssh://github.com/sonatype/sonatype-tycho.git" );
        testURI( "scm:git:git@github.com:sonatype/sonatype-tycho.git", true,
                 "ssh://git@github.com/sonatype/sonatype-tycho.git" );
    }

    public void testSshUrls()
        throws Exception
    {
        testURI( "scm:git:ssh://git@github.com/sonatype/sonatype-tycho.git", true,
                 "ssh://git@github.com/sonatype/sonatype-tycho.git" );
        testURI( "scm:git:github.com:sonatype/sonatype-tycho.git", true, "ssh://github.com/sonatype/sonatype-tycho.git" );
        testURI( "scm:git:ssh://github.com/sonatype/sonatype-tycho.git", true,
                 "ssh://github.com/sonatype/sonatype-tycho.git" );
    }

    public void testGitUrls()
        throws Exception
    {
        testURI( "scm:git:git://github.com/sonatype/sonatype-tycho.git", true,
                 "git://github.com/sonatype/sonatype-tycho.git" );
    }

    public void testHttpsUrls()
        throws Exception
    {
        testURI( "scm:git:https://vladt@github.com/sonatype/sonatype-tycho.git", true,
                 "https://vladt@github.com/sonatype/sonatype-tycho.git" );
        testURI( "scm:git:https://github.com/sonatype/sonatype-tycho.git", true,
                 "https://github.com/sonatype/sonatype-tycho.git" );
    }
}
