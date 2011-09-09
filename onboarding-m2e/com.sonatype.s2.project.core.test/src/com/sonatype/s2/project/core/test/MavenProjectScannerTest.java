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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.model.Model;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

import com.sonatype.s2.project.core.internal.MavenProjectScanner;

public class MavenProjectScannerTest
    extends AbstractMavenProjectTestCase
{
    public void testBasic()
        throws Exception
    {
        File basedir = new File( "target/basic" );
        copyDir( new File( "resources/scanner/basic" ), basedir );

        MavenProjectScanner scanner = new MavenProjectScanner( basedir, Collections.singletonList( "d" ) );

        scanner.run( new NullProgressMonitor() );

        ArrayList<MavenProjectInfo> projects = getProjectsList( scanner );

        assertEquals( 7, projects.size() );

        assertEquals( "parent", projects.get( 0 ).getModel().getArtifactId() );
        // assertEquals( "parent", projects.get( 0 ).getPomFile().getParentFile().getName() );

        assertEquals( "moduleA", projects.get( 1 ).getModel().getArtifactId() );
        assertParentFolder( projects.get( 0 ), projects.get( 1 ) );

        assertEquals( "moduleB", projects.get( 2 ).getModel().getArtifactId() );
        assertParentFolder( projects.get( 0 ), projects.get( 2 ) );

        assertEquals( "moduleBA", projects.get( 3 ).getModel().getArtifactId() );
        assertParentFolder( projects.get( 0 ), projects.get( 3 ) );

        assertEquals( "moduleBB", projects.get( 4 ).getModel().getArtifactId() );
        assertParentFolder( projects.get( 2 ), projects.get( 4 ) );

        assertTrue( projects.get( 5 ).getPomFile().getAbsolutePath().replace( '\\', '/' ).endsWith( "/moduleC/pom.xml" ) );
        assertParentFolder( projects.get( 0 ), projects.get( 5 ) );

        assertEquals( "moduleD", projects.get( 6 ).getModel().getArtifactId() );
        assertParentFolder( projects.get( 0 ), projects.get( 6 ) );
    }

    private ArrayList<MavenProjectInfo> getProjectsList( MavenProjectScanner scanner )
    {
        MavenPlugin mavenPlugin = MavenPlugin.getDefault();
        IProjectConfigurationManager configurationManager = mavenPlugin.getProjectConfigurationManager();

        ArrayList<MavenProjectInfo> projects =
            new ArrayList<MavenProjectInfo>( configurationManager.collectProjects( scanner.getProjects() ) );
        return projects;
    }

    private void assertParentFolder( MavenProjectInfo parentInfo, MavenProjectInfo childInfo )
        throws IOException
    {
        File parent = parentInfo.getPomFile().getParentFile().getCanonicalFile();
        File child = childInfo.getPomFile().getParentFile().getCanonicalFile();

        assertEquals( parent, child.getParentFile() );
    }

    public void testCrisscross()
        throws Exception
    {
        File basedir = new File( "target/crisscross" );
        copyDir( new File( "resources/scanner/crisscross" ), basedir );

        MavenProjectScanner scanner = new MavenProjectScanner( basedir );

        scanner.run( new NullProgressMonitor() );

        ArrayList<MavenProjectInfo> projects = getProjectsList( scanner );

        assertEquals( 3, projects.size() );
        assertEquals( "moduleA", projects.get( 1 ).getModel().getArtifactId() );
        assertEquals( "moduleB", projects.get( 2 ).getModel().getArtifactId() );
    }

    public void testStatus()
        throws Exception
    {
        File basedir = new File( "target/status" );
        copyDir( new File( "resources/scanner/basic/moduleA" ), basedir );

        MavenProjectScanner scanner = new MavenProjectScanner( basedir );

        scanner.run( new NullProgressMonitor() );
        assertTrue( "The scanner should have OK status.", scanner.getStatus().isOK() );

        final String errorTest = "Error Test";
        MavenProjectScanner scanner2 = new MavenProjectScanner( basedir )
        {
            @Override
            protected MavenProjectInfo newMavenProjectInfo( String projectLabel, File pomFile, Model model,
                                                            MavenProjectInfo parentInfo )
            {
                addError( new Exception( errorTest ) );
                return super.newMavenProjectInfo( projectLabel, pomFile, model, parentInfo );
            }
        };

        scanner2.run( new NullProgressMonitor() );
        IStatus status = scanner2.getStatus();
        assertTrue( "The scanner should have an error status.", !status.isOK() );
        assertEquals( "The status should contain one error.", 1, status.getChildren().length );
        assertEquals( "Status message", errorTest, status.getChildren()[0].getMessage() );
    }

    /**
     * Tests that the project scanner considers both the profiles given by the PMD and any active profiles from the
     * settings when reading the POMs.
     */
    public void testScannerUsesBothPmdProfilesAndSettingsProfiles()
        throws Exception
    {
        File basedir = new File( "target/profiles" );
        copyDir( new File( "resources/scanner/profiles" ), basedir );

        MavenProjectScanner scanner;

        String userSettings = mavenConfiguration.getUserSettingsFile();
        try
        {
            mavenConfiguration.setUserSettingsFile( new File( basedir, "settings.xml" ).getPath() );

            scanner = new MavenProjectScanner( basedir, Arrays.asList( "profile-a" ) );

            scanner.run( monitor );
        }
        finally
        {
            mavenConfiguration.setUserSettingsFile( userSettings );
        }

        ArrayList<MavenProjectInfo> projects = getProjectsList( scanner );

        assertEquals( 3, projects.size() );
        assertEquals( "parent", projects.get( 0 ).getModel().getArtifactId() );
        assertEquals( "mod-a", projects.get( 1 ).getModel().getArtifactId() );
        assertEquals( "mod-b", projects.get( 2 ).getModel().getArtifactId() );
    }
}
