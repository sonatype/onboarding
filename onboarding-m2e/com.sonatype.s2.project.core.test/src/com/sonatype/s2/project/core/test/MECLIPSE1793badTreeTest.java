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

import com.sonatype.s2.project.core.test.scm.FileTeamProvider;

public class MECLIPSE1793badTreeTest
    extends AbstractMavenProjectMaterializationTest
{
    public void testEnabledProfiles()
        throws Exception
    {
        materialize( "resources/projects/MECLIPSE-1793-bad-tree/mse-codebase.xml" );

        assertWorkspaceProjects( 2 );

        assertWorkspaceProject( "no-pom" );
        FileTeamProvider.assertTeamProviderEnabled( getWorkspaceProject( "no-pom" ) );
        
        assertWorkspaceProject( "bad-pom" );
        FileTeamProvider.assertTeamProviderEnabled( getWorkspaceProject( "bad-pom" ) );
    }
}
