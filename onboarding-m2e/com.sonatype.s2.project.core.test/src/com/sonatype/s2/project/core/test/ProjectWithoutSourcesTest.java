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
package com.sonatype.s2.project.core.test;

import java.io.File;

import org.eclipse.core.runtime.CoreException;

public class ProjectWithoutSourcesTest
    extends AbstractMavenProjectMaterializationTest
{
    public void testNoModules()
        throws Exception
    {
        materialize( new File( "resources/projects/MECLIPSE-966-no-sources/no-modules.xml" ).getCanonicalPath() );

        assertWorkspaceProjects( 0 );
    }

    public void testEmptyModules()
        throws Exception
    {
        materialize( new File( "resources/projects/MECLIPSE-966-no-sources/empty-modules.xml" ).getCanonicalPath() );

        assertWorkspaceProjects( 0 );
    }

    public void testNoScmUrl()
        throws Exception
    {
        try
        {
            materialize( new File( "resources/projects/MECLIPSE-966-no-sources/no-scm-url.xml" ).getCanonicalPath() );
        }
        catch ( CoreException e )
        {
            assertTrue( e.getMessage().contains( "does not have scm url" ) );
        }

        assertWorkspaceProjects( 0 );
    }
}
