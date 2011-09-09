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

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.test.scm.FileTeamProvider;

public class MECLIPSE1583ModulesAddRemoveTest
    extends AbstractMavenProjectMaterializationTest
{
    public void testMaterializationBasic()
        throws Exception
    {
        materialize( "resources/projects/MECLIPSE-1583-modules-add-remove/basic/s2.xml" );

        assertMavenProject( "MECLIPSE-1583-basic", "basic", "0.0.1-SNAPSHOT" );
        assertMavenProject( "MECLIPSE-1583-basic", "basic-module01", "0.0.1-SNAPSHOT" );

        IProject aggregator = getWorkspaceProject( "basic" );

        // add module
        copyContent( aggregator, "pom-add-module.xml", "pom.xml" );
        assertMavenProject( "MECLIPSE-1583-basic", "basic-module02", "0.0.1-SNAPSHOT" );

        FileTeamProvider.assertTeamProviderEnabled( getWorkspaceProject( "basic-module02" ) );

        // remove module, but not actual project
        copyContent( aggregator, "pom-remove-module.xml", "pom.xml" );
        assertMavenProject( "MECLIPSE-1583-basic", "basic-module01", "0.0.1-SNAPSHOT" );

        // remove modules and the project
        assertNotNull( getWorkspaceProject( "basic-module01" ) );
        copyContent( aggregator, "pom-add-module.xml", "pom.xml" );
        FileUtils.deleteDirectory( new File( aggregator.getLocation().toFile(), "module01" ) );
        copyContent( aggregator, "pom-remove-module.xml", "pom.xml" );
        assertNull( getWorkspaceProject( "basic-module01" ) );
    }

    public void testMaterializationRelativePath()
        throws Exception
    {
        materialize( "resources/projects/MECLIPSE-1583-modules-add-remove/relative-path/s2.xml" );

        IProject aggregator = getWorkspaceProject( "aggregator" );

        copyContent( aggregator, "pom-add-module.xml", "pom.xml" );
        assertMavenProject( "MECLIPSE-1583-relative-path", "relative-path-module01", "0.0.1-SNAPSHOT" );

        copyContent( aggregator, "pom-remove-module.xml", "pom.xml" );
        assertMavenProject( "MECLIPSE-1583-relative-path", "relative-path-module01", "0.0.1-SNAPSHOT" );

        copyContent( aggregator, "pom-add-module.xml", "pom.xml" );
        IWorkspaceCodebase codebase = S2ProjectCore.getInstance().getWorkspaceCodebases().get( 0 );
        FileUtils.deleteDirectory( new File( codebase.getSourceTrees().get( 0 ).getLocation(), "module01" ) );
        copyContent( aggregator, "pom-remove-module.xml", "pom.xml" );
        assertNull( getWorkspaceProject( "relative-path-module01" ) );
    }

    public void testMaterializationProfiles()
        throws Exception
    {
        materialize( "resources/projects/MECLIPSE-1583-modules-add-remove/profiles/s2.xml" );

        IProject aggregator = getWorkspaceProject( "profiles-parent" );
        copyContent( aggregator, "pom-add-module.xml", "pom.xml" );

        IProject module01 = getWorkspaceProject( "profiles-module01" );

        IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().create( module01, monitor );

        assertActiveProfile( "profile-a", facade.getMavenProject( monitor ) );
        assertActiveProfile( "test", facade.getMavenProject( monitor ) );

        assertTrue( facade.getResolverConfiguration().getActiveProfileList().contains( "profile-a" ) );
        assertTrue( facade.getResolverConfiguration().getActiveProfileList().contains( "test" ) );
    }

    public void testMaterializationBadModule()
        throws Exception
    {
        materialize( "resources/projects/MECLIPSE-1583-modules-add-remove/badmodule/s2.xml" );

        assertMavenProject( "MECLIPSE-1583-badmodule", "badmodule", "0.0.1-SNAPSHOT" );

        IProject aggregator = getWorkspaceProject( "badmodule" );

        copyContent( aggregator, "pom-add-module.xml", "pom.xml" );
        assertWorkspaceProjects( 1 );

        copyContent( aggregator, "pom-remove-module.xml", "pom.xml" );
        assertWorkspaceProjects( 1 );
    }

    public void testImportBasic()
        throws Exception
    {
        importProjects( "resources/projects/MECLIPSE-1583-modules-add-remove/basic", new String[] { "pom.xml",
            "module01/pom.xml" }, new ResolverConfiguration() );

        // sanity check
        assertMavenProject( "MECLIPSE-1583-basic", "basic", "0.0.1-SNAPSHOT" );
        assertMavenProject( "MECLIPSE-1583-basic", "basic-module01", "0.0.1-SNAPSHOT" );

        IProject aggregator = getWorkspaceProject( "basic" );

        // add module
        copyContent( aggregator, "pom-add-module.xml", "pom.xml" );
        assertMavenProject( "MECLIPSE-1583-basic", "basic-module02", "0.0.1-SNAPSHOT" );

        // there is no good way to share projects that were not create during materialization
        //FileTeamProvider.assertTeamProviderEnabled( getWorkspaceProject( "basic-module02" ) );

        // remove module, but not actual project
        copyContent( aggregator, "pom-remove-module.xml", "pom.xml" );
        assertMavenProject( "MECLIPSE-1583-basic", "basic-module01", "0.0.1-SNAPSHOT" );

        // remove modules and the project
        assertNotNull( getWorkspaceProject( "basic-module01" ) );
        copyContent( aggregator, "pom-add-module.xml", "pom.xml" );
        FileUtils.deleteDirectory( new File( aggregator.getLocation().toFile(), "module01" ) );
        copyContent( aggregator, "pom-remove-module.xml", "pom.xml" );
        assertNull( getWorkspaceProject( "basic-module01" ) );
    }

    public void testImportBadModule()
        throws Exception
    {
        importProjects( "resources/projects/MECLIPSE-1583-modules-add-remove/badmodule", new String[] { "pom.xml" },
                        new ResolverConfiguration() );

        assertMavenProject( "MECLIPSE-1583-badmodule", "badmodule", "0.0.1-SNAPSHOT" );

        IProject aggregator = getWorkspaceProject( "badmodule" );

        copyContent( aggregator, "pom-add-module.xml", "pom.xml" );
        assertWorkspaceProjects( 1 );

        copyContent( aggregator, "pom-remove-module.xml", "pom.xml" );
        assertWorkspaceProjects( 1 );
    }
}
