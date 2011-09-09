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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.model.IS2Project;

public abstract class AbstractMavenProjectMaterializationTest
    extends AbstractMavenProjectTestCase
{

    private List<HttpServer> httpServers;

    protected void materialize( String descriptor )
        throws Exception
    {
        materialize( descriptor, true );
    }

    protected void materialize( String descriptor, boolean filter )
        throws Exception
    {
        String descriptorUrl;
        if ( descriptor.contains( "://" ) )
        {
            descriptorUrl = descriptor;
        }
        else
        {
            File descriptorFile = new File( descriptor ).getCanonicalFile();
            if ( filter )
            {
                descriptorFile = filter( descriptorFile );
            }
            descriptorUrl = descriptorFile.toURI().toString();
        }

        S2ProjectCore core = S2ProjectCore.getInstance();
        IS2Project project = core.loadProject( descriptorUrl, new NullProgressMonitor() );
        core.materialize( project, true, new NullProgressMonitor() );
        waitForJobsToComplete( monitor );
    }

    protected void assertMavenProject( String groupId, String artifactId, String version )
    {
        MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
        assertNotNull( groupId + ":" + artifactId + ":" + version,
            projectManager.getMavenProject( groupId, artifactId, version ) );
    }

    protected void assertWorkspaceProjects( int count )
    {
        assertEquals( count, workspace.getRoot().getProjects().length );
    }

    protected void assertWorkspaceProject( String projectName )
    {
        IProject project = getWorkspaceProject( projectName );
        assertNotNull( "Project does not exist in the workspace: '" + projectName + "'", project );
        assertTrue( project.isAccessible() );
    }

    protected IProject getWorkspaceProject( String projectName )
    {
        // NOTE: IWorkspaceRoot.getProject(String) creates new/missing projects which is not desired here
        assertNotNull( "workspace is null", workspace );
        assertNotNull( "workspace root is null", workspace.getRoot() );
        IProject[] projects = workspace.getRoot().getProjects();
        for ( IProject project : projects )
        {
            if ( project.getName().equals( projectName ) )
            {
                return project;
            }
        }
        return null;
    }

    private File filter( File file )
        throws IOException
    {
        File filteredFile = new File("target/" + getName() + "/mse-codebase.xml" );
        filteredFile.getParentFile().mkdirs();

        filterCodebaseDescriptor( file, filteredFile );

        return filteredFile;
    }

    protected void filterCodebaseDescriptor( File source, File target )
        throws UnsupportedEncodingException, FileNotFoundException, IOException
    {
        Map<String, String> tokens = new HashMap<String, String>();
        File basedir = new File( "" ).getCanonicalFile();
        String baseurl = "file://" + basedir.toURI().getPath();
        if ( baseurl.endsWith( "/" ) )
        {
            baseurl = baseurl.substring( 0, baseurl.length() - 1 );
        }
        tokens.put( "${baseurl}", baseurl );
        tokens.put( "${basedir}", basedir.getPath() );

        BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( source ), "UTF-8" ) );
        BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( target ), "UTF-8" ) );
        try
        {
            String line;
            while ( ( line = reader.readLine() ) != null )
            {
                line = filter( line, tokens );
                writer.write( line );
                writer.newLine();
            }
        }
        finally
        {
            writer.close();
            reader.close();
        }
    }

    private String filter( String str, Map<String, String> tokens )
    {
        for ( String token : tokens.keySet() )
        {
            str = str.replace( token, tokens.get( token ) );
        }
        return str;
    }

    protected HttpServer newHttpServer()
    {
        if ( httpServers == null )
        {
            httpServers = new ArrayList<HttpServer>();
        }

        HttpServer httpServer = new HttpServer();
        httpServer.addResources( "/", "resources", "xml", "properties" );
        httpServers.add( httpServer );

        return httpServer;
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        S2ProjectCore.getInstance().getCodebaseRegistry().clear();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            S2ProjectCore.getInstance().getCodebaseRegistry().clear();
            stopHttpServers();
        }
        finally
        {
            super.tearDown();
        }
    }

    protected void stopHttpServers()
        throws Exception
    {
        if ( httpServers != null )
        {
            for ( HttpServer httpServer : httpServers )
            {
                httpServer.stop();
            }
            httpServers = null;
        }
    }

    protected void assertActiveProfile( String expected, MavenProject project )
    {
        for ( Profile profile : project.getActiveProfiles() )
        {
            if ( expected.equals( profile.getId() ) )
            {
                return;
            }
        }

        fail( "Profile is not active '" + expected + "'" );
    }
}
