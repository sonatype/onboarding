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
package com.sonatype.s2.publisher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.maven.ide.eclipse.authentication.AuthFacade;

import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.S2ProjectFacade;
import com.sonatype.s2.project.model.descriptor.EclipsePreferencesLocation;
import com.sonatype.s2.project.model.descriptor.MavenSettingsLocation;
import com.sonatype.s2.project.model.descriptor.P2LineupLocation;
import com.sonatype.s2.project.model.descriptor.Project;
import com.sonatype.s2.publisher.internal.Messages;
import com.sonatype.s2.publisher.nexus.NexusCodebasePublisher;

public class S2PublisherTest
    extends TestCase
{
    private static final String USERNAME = "username";

    private static final String PASSWORD = "password";

    private static final String ROLE = "role";

    private HttpServer httpServer;

    private File tmpDir;

    private String baseUrl;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        tmpDir = File.createTempFile( "catalog", "" );
        tmpDir.delete();
        httpServer = new HttpServer();
        httpServer.addResources( "catalog", tmpDir.getAbsolutePath() );
        httpServer.addSecuredRealm( "/catalog/secured/*", ROLE );
        httpServer.addUser( USERNAME, PASSWORD, ROLE );
        httpServer.start();
        baseUrl = httpServer.getHttpUrl();
    }

    private void deleteDir( File directory )
        throws Exception
    {
        int retries = 0;
        while ( retries < 10 )
        {
            try
            {
                FileUtils.deleteDirectory( tmpDir );
                break;
            }
            catch ( IOException e )
            {
                if ( e.getMessage() != null && e.getMessage().endsWith( "unable to be deleted." ) )
                {
                    retries++;
                    if ( retries < 10 )
                    {
                        Thread.sleep( 1000 );
                        continue;
                    }
                }
                throw new RuntimeException( "Retries: " + retries + ". Error: " + e.getMessage(), e );
            }
        }
        if ( retries > 0 )
        {
            System.out.println( "Retries to delete directory '" + directory.getAbsolutePath() + "': " + retries );
        }
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            if ( httpServer != null )
            {
                httpServer.stop();
            }
            deleteDir( tmpDir );
        }
        finally
        {
            super.tearDown();
        }
    }

    private File getTmpDir( String name )
    {
        return new File( tmpDir, name + IS2Project.PROJECT_REPOSITORY_PATH );
    }

    public void testPublishProjectWithIcon_Authenticated_ImplicitCredentials()
        throws Exception
    {
        S2PublishRequest s2PublishRequest = new S2PublishRequest();
        s2PublishRequest.setNexusBaseUrl( baseUrl + "/catalog/secured" );
        AuthFacade.getAuthService().save( s2PublishRequest.getNexusBaseUrl(), USERNAME, PASSWORD );
        s2PublishRequest.addS2Project( getProjectLocation( "resources/projects/projectWithIcon" ) );

        NexusCodebasePublisher publisher = new NexusCodebasePublisher();
        publisher.publish( s2PublishRequest, new NullProgressMonitor() );

        assertTrue( new File( getTmpDir( "secured" ), "test/projectWithIcon/1.0.0/"
            + IS2Project.PROJECT_DESCRIPTOR_FILENAME ).exists() );
        assertTrue( new File( getTmpDir( "secured" ), "test/projectWithIcon/1.0.0/" + IS2Project.PROJECT_ICON_FILENAME ).exists() );
    }

    public void testPublishProjectWithIcon()
        throws Exception
    {
        S2PublishRequest s2PublishRequest = new S2PublishRequest();
        s2PublishRequest.setNexusBaseUrl( baseUrl + "/catalog" );
        s2PublishRequest.addS2Project( getProjectLocation( "resources/projects/projectWithIcon" ) );

        NexusCodebasePublisher publisher = new NexusCodebasePublisher();
        publisher.publish( s2PublishRequest, new NullProgressMonitor() );

        assertTrue( new File( getTmpDir( "" ), "test/projectWithIcon/1.0.0/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME ).exists() );
        assertTrue( new File( getTmpDir( "" ), "test/projectWithIcon/1.0.0/" + IS2Project.PROJECT_ICON_FILENAME ).exists() );
    }

    public void testPublishProjectWithoutIcon()
        throws Exception
    {
        S2PublishRequest s2PublishRequest = new S2PublishRequest();
        s2PublishRequest.setNexusBaseUrl( baseUrl + "/catalog" );
        s2PublishRequest.addS2Project( getProjectLocation( "resources/projects/projectWithoutIcon" ) );

        NexusCodebasePublisher publisher = new NexusCodebasePublisher();
        publisher.publish( s2PublishRequest, new NullProgressMonitor() );

        assertTrue( new File( getTmpDir( "" ), "test/projectWithoutIcon/1.0.0/"
            + IS2Project.PROJECT_DESCRIPTOR_FILENAME ).exists() );
        assertEquals( 1, new File( getTmpDir( "" ), "test/projectWithoutIcon/1.0.0/" ).list().length );
    }

    public void testPublishProjectWithPreferences()
        throws Exception
    {
        S2PublishRequest s2PublishRequest = new S2PublishRequest();
        s2PublishRequest.setNexusBaseUrl( baseUrl + "/catalog" );
        s2PublishRequest.addS2Project( getProjectLocation( "resources/projects/projectWithPreferences" ) );

        NexusCodebasePublisher publisher = new NexusCodebasePublisher();
        publisher.publish( s2PublishRequest, new NullProgressMonitor() );

        assertTrue( new File( getTmpDir( "" ), "test/projectWithPreferences/1.0.0/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME ).exists() );
        assertTrue( new File( getTmpDir( "" ), "test/projectWithPreferences/1.0.0/" + IS2Project.PROJECT_PREFERENCES_FILENAME ).exists() );
    }

    public void testPublishTwoProjects()
        throws Exception
    {
        S2PublishRequest s2PublishRequest = new S2PublishRequest();
        s2PublishRequest.setNexusBaseUrl( baseUrl + "/catalog" );
        s2PublishRequest.addS2Project( getProjectLocation( "resources/projects/projectWithIcon" ) );
        s2PublishRequest.addS2Project( getProjectLocation( "resources/projects/projectWithoutIcon" ) );

        NexusCodebasePublisher publisher = new NexusCodebasePublisher();
        publisher.publish( s2PublishRequest, new NullProgressMonitor() );

        assertTrue( new File( getTmpDir( "" ), "test/projectWithIcon/1.0.0/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME ).exists() );
        assertTrue( new File( getTmpDir( "" ), "test/projectWithIcon/1.0.0/" + IS2Project.PROJECT_ICON_FILENAME ).exists() );
        assertTrue( new File( getTmpDir( "" ), "test/projectWithoutIcon/1.0.0/"
            + IS2Project.PROJECT_DESCRIPTOR_FILENAME ).exists() );
    }

    public void testPublishProjectInvalid()
        throws Exception
    {
        S2PublishRequest s2PublishRequest = new S2PublishRequest();
        s2PublishRequest.setNexusBaseUrl( baseUrl + "/catalog" );
        s2PublishRequest.addS2Project( getProjectLocation( "resources/projects" ) );

        NexusCodebasePublisher publisher = new NexusCodebasePublisher();
        try
        {
            publisher.publish( s2PublishRequest, new NullProgressMonitor() );
            fail( "Expected S2PublisherException" );
        }
        catch ( S2PublisherException e )
        {
            if ( !e.getMessage().endsWith( IS2Project.PROJECT_DESCRIPTOR_FILENAME + " does not exist." ) )
            {
                throw e;
            }
        }
    }

    /*
     * Test that we fail attempting to overwrite a non-HEAD codebase.
     */
    public void testPublishExisting()
        throws Exception
    {
        try
        {
            File tmpRep = new File( getTmpDir( "" ), "test/projectWithIcon/1.0.0/" );
            tmpRep.mkdirs();
            FileUtils.copyDirectory( new File( "resources/projects/projectWithIcon" ), tmpRep );
        }
        catch ( Exception e )
        {
            fail( "Failed to copy test" );
        }

        S2PublishRequest s2PublishRequest = new S2PublishRequest();
        s2PublishRequest.setNexusBaseUrl( baseUrl + "/catalog" );
        s2PublishRequest.addS2Project( getProjectLocation( "resources/projects/projectWithIcon" ) );

        NexusCodebasePublisher publisher = new NexusCodebasePublisher();
        try
        {
            publisher.publish( s2PublishRequest, new NullProgressMonitor() );
        }
        catch ( S2PublisherException e )
        {
            assertEquals("Unexpected error messages", Messages.NexusCodebasePublisher_error_codebaseExists, e.getMessage());
            return;
        }
        fail( "Expected S2PublisherException to be thrown" );
    }

    /*
     * Test that we can overwrite the HEAD codebase.
     */
    public void testPublishExistingHead()
        throws Exception
    {
        try
        {
            File tmpRep = new File( getTmpDir( "" ), "test/projectHeadVersion/1.0.0-HEAD" );
            tmpRep.mkdirs();
            File parent = new File( "resources/projects/projectHeadVersion/mse" ).getAbsoluteFile();
            FileUtils.copyFile( new File( parent, "mse-codebase.xml" ), new File( tmpRep, "mse-codebase.xml" ) );
            FileUtils.copyFile( new File( parent, "mse-codebase-icon.png" ), new File( tmpRep, "mse-codebase-icon.png" ) );
        }
        catch ( Exception e )
        {
            fail( "Failed to copy test" );
        }

        S2PublishRequest s2PublishRequest = new S2PublishRequest();
        s2PublishRequest.setNexusBaseUrl( baseUrl + "/catalog" );
        s2PublishRequest.addS2Project( getProjectLocation( "resources/projects/projectHeadVersion" ) );

        NexusCodebasePublisher publisher = new NexusCodebasePublisher();
        publisher.publish( s2PublishRequest, new NullProgressMonitor() );

        assertTrue( new File( getTmpDir( "" ), "test/projectHeadVersion/1.0.0-HEAD/"
            + IS2Project.PROJECT_DESCRIPTOR_FILENAME ).exists() );
        assertEquals( 2, new File( getTmpDir( "" ), "test/projectHeadVersion/1.0.0-HEAD/" ).list().length );
    }

    public void testBaseUrlReplacement()
        throws Exception
    {
        String baseUrl = this.baseUrl + "/catalog";
        S2PublishRequest s2PublishRequest = new S2PublishRequest();
        s2PublishRequest.setNexusBaseUrl( baseUrl );

        IS2Project project = new Project();
        project.setGroupId( "a" );
        project.setArtifactId( "b" );
        project.setVersion( "1" );

        project.setP2LineupLocation( new P2LineupLocation() );
        project.getP2LineupLocation().setUrl( baseUrl + "/some/lineup/repo" );
        project.setMavenSettingsLocation( new MavenSettingsLocation() );
        project.getMavenSettingsLocation().setUrl( baseUrl + "/some/maven/settings.xml" );
        project.setEclipsePreferencesLocation( new EclipsePreferencesLocation() );
        project.getEclipsePreferencesLocation().setUrl( baseUrl + "/some/eclipse/preferences.zip" );

        File tmpDir = new File( "target/baseUrlSubstitution" );
        tmpDir.mkdirs();
        OutputStream os = new FileOutputStream( new File( tmpDir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ) );
        try
        {
            S2ProjectFacade.writeProject( project, os );
        }
        finally
        {
            IOUtil.close( os );
        }
        s2PublishRequest.addS2Project( Path.fromOSString( tmpDir.getCanonicalPath() ) );

        NexusCodebasePublisher publisher = new NexusCodebasePublisher();
        publisher.publish( s2PublishRequest, new NullProgressMonitor() );

        IS2Project result;
        InputStream is =
            new FileInputStream( new File( getTmpDir( "" ), "a/b/1/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME ) );
        try
        {
            result = S2ProjectFacade.loadProject( is, false );
        }
        finally
        {
            IOUtil.close( is );
        }

        assertEquals( "${nexus.baseURL}/some/lineup/repo", result.getP2LineupLocation().getUrl() );
        assertEquals( "${nexus.baseURL}/some/maven/settings.xml", result.getMavenSettingsLocation().getUrl() );
        assertEquals( "${nexus.baseURL}/some/eclipse/preferences.zip", result.getEclipsePreferencesLocation().getUrl() );
    }

    private IPath getProjectLocation( String path )
    {
        return Path.fromOSString( new File( path, S2PublisherConstants.PMD_PATH ).getAbsolutePath() );
    }
}
