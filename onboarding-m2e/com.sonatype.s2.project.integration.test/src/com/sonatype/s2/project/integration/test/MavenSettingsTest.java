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
package com.sonatype.s2.project.integration.test;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IProject;

import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.tests.common.SshServer;
import com.sonatype.s2.ssh.SshHandlerManager;

/**
 * Note: The SSH related tests in here require an external Git installation (1.7.0.2+) to be present in your
 * {@code PATH}.
 */
public class MavenSettingsTest
    extends AbstractMavenProjectMaterializationTest
{
    private SshServer sshServer;

    private File sshDirectory;

    private File knownHosts;

    @Override
    protected void setUp()
        throws Exception
    {
        File dir = new File( "target/localrepo-MavenSettingsTest-" + getName() );
        FileUtils.deleteDirectory( dir );
        assertFalse( dir.exists() );

        super.setUp();

        sshDirectory = File.createTempFile( "GitTest-", "-" + getName() );
        sshDirectory.delete();
        knownHosts = new File( sshDirectory, "known_hosts" );

        SshHandlerManager sshManager = SshHandlerManager.getInstance();
        sshManager.setSshDirectory( sshDirectory );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            if ( sshServer != null )
            {
                sshServer.stop();
                sshServer = null;
            }
            knownHosts.delete();
            sshDirectory.delete();
        }
        finally
        {
            super.tearDown();
        }
    }

    protected SshServer startSshServer()
        throws Exception
    {
        sshServer = newSshServer();
        sshServer.start();
        return sshServer;
    }

    protected SshServer newSshServer()
        throws Exception
    {
        sshServer = new SshServer();
        sshServer.addUser( "testuser", "testpass" );
        sshServer.addUser( "anon", "" );
        return sshServer;
    }

    @Override
    protected HttpServer newHttpServer()
    {
        HttpServer httpServer = super.newHttpServer();
        if ( sshServer != null )
        {
            httpServer.setFilterToken( "@port.ssh@", Integer.toString( sshServer.getPort() ) );
        }
        return httpServer;
    }

    public void testProjectWithMavenSettings()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        validateScmAccessAndMaterialize( httpServer.getHttpUrl() + "/projects/codebase-with-maven-settings.xml" );

        String projectName = "git-test-maven-settings";
        assertMavenProject( "test", "git-test-maven-settings", "0.0.1-SNAPSHOT" );
        assertWorkspaceProject( projectName );
        assertWorkspaceProjectShared( projectName );

        IProject project = getWorkspaceProject( projectName );
        assertNoErrors( project );
    }

    public void testProjectWithMavenSettingsAuthenticated()
        throws Exception
    {
        HttpServer httpServer = newHttpServer();
        httpServer.addUser( "testuser", "testpass", "authorized" );
        httpServer.addSecuredRealm( "/*", "authorized" );
        httpServer.start();
        addRealmAndURL( "test", httpServer.getHttpUrl(), "testuser", "testpass" );

        validateScmAccessAndMaterialize( httpServer.getHttpUrl()
            + "/projects/codebase-with-maven-settings-over-http.xml" );

        String projectName = "git-test-maven-settings";
        assertMavenProject( "test", "git-test-maven-settings", "0.0.1-SNAPSHOT" );
        assertWorkspaceProject( projectName );
        assertWorkspaceProjectShared( projectName );

        IProject project = getWorkspaceProject( projectName );
        assertNoErrors( project );
    }
}
