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

import java.io.File;
import java.io.InputStream;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.ui.PlatformUI;

import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.integration.test.AbstractMavenProjectMaterializationTest;
import com.sonatype.s2.project.tests.common.SshServer;
import com.sonatype.s2.ssh.SshHandlerManager;

public abstract class AbstractGitTest
    extends AbstractMavenProjectMaterializationTest
{
    protected SshServer sshServer;

    private File sshDirectory;

    protected File knownHosts;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        sshDirectory = File.createTempFile( "GitTest-", "-" + getName() );
        sshDirectory.delete();
        knownHosts = new File( sshDirectory, "known_hosts" );

        SshHandlerManager sshManager = SshHandlerManager.getInstance();
        sshManager.setSshDirectory( sshDirectory );

        // NOTE: The EGit resource decorator opens pack files which interferes with the workspace cleanup
        PlatformUI.getWorkbench().getDecoratorManager().setEnabled( "org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator",
                                                                    false );
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

    protected void assertProject( String groupId, String artifactId, String version )
        throws Exception
    {
        assertMavenProject( groupId, artifactId, version );
        assertWorkspaceProject( artifactId );
        assertWorkspaceProjectShared( artifactId );
        assertCoreAutocrlfNotSet( artifactId );
    }

    private void assertCoreAutocrlfNotSet( String projectName )
        throws Exception
    {
        IProject project = getWorkspaceProject( projectName );
        IFile configFile = project.getFile( ".git/config" );
        assertTrue( "git config file does not exist", configFile.exists() );
        InputStream configContentStream = configFile.getContents();
        try
        {
            String configContent = IOUtil.toString( configContentStream );
            assertTrue( "git config contains setting for " + ConfigConstants.CONFIG_KEY_AUTOCRLF,
                        configContent.indexOf( ConfigConstants.CONFIG_KEY_AUTOCRLF ) == -1 );
        }
        finally
        {
            IOUtil.close( configContentStream );
        }
    }
}
