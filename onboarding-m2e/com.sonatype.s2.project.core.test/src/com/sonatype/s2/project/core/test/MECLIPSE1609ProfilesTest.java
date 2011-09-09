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

import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;

public class MECLIPSE1609ProfilesTest
    extends AbstractMavenProjectMaterializationTest
{
    public void testEnabledProfiles()
        throws Exception
    {
        materialize( "resources/projects/MECLIPSE-1609-profiles/mse-codebase.xml" );

        IProject project = getWorkspaceProject( "MECLIPSE-1609-profiles" );

        IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().create( project, monitor );

        assertActiveProfile( "profile-a", facade.getMavenProject( monitor ) );

        assertTrue( facade.getResolverConfiguration().getActiveProfileList().contains( "profile-a" ) );

    }
}
