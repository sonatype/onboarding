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
package com.sonatype.s2.project.integration.test.svn;

import org.eclipse.core.resources.IProject;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.SVNTeamProvider;
import org.eclipse.team.svn.core.operation.file.SVNFileStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.AbstractSVNStorage;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;

import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.integration.test.AbstractMavenProjectMaterializationTest;
import com.sonatype.s2.project.tests.common.SvnServer;

abstract class AbstractSvnTest
    extends AbstractMavenProjectMaterializationTest
{
    protected static final String SCM_SVN_PATH = "resources/scm/svn/";
    protected SvnServer svnServer;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        cleanSVNTeamPluginConfig();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            if ( svnServer != null )
            {
                svnServer.stop();
                svnServer = null;
            }
            cleanSVNTeamPluginConfig();
        }
        finally
        {
            super.tearDown();
        }
    }

    protected void cleanSVNTeamPluginConfig()
        throws Exception
    {
        removeSvnLocations( SVNRemoteStorage.instance() );
        removeSvnLocations( SVNFileStorage.instance() );
    }

    private void removeSvnLocations( AbstractSVNStorage svnStorage )
        throws Exception
    {
        IRepositoryLocation[] locations = svnStorage.getRepositoryLocations();
        if ( locations == null || locations.length == 0 )
        {
            return;
        }
        for ( IRepositoryLocation location : locations )
        {
            svnStorage.removeRepositoryLocation( location );
        }
        SVNTeamPlugin.instance().setLocationsDirty( true );
        svnStorage.saveConfiguration();
    }

    protected SvnServer startSvnServer( String dump, String config )
        throws Exception
    {
        svnServer = new SvnServer();
        svnServer.setDumpFile( SCM_SVN_PATH + dump );
        svnServer.setConfDir( SCM_SVN_PATH + config );
        svnServer.start();
        return svnServer;
    }

    @Override
    protected HttpServer newHttpServer()
    {
        HttpServer httpServer = super.newHttpServer();
        if ( svnServer != null )
        {
            httpServer.setFilterToken( "@port.svn@", Integer.toString( svnServer.getPort() ) );
        }
        return httpServer;
    }

    protected static void assertAuthSet( IProject project )
    {
        IRepositoryLocation location =
            ( (SVNTeamProvider) SVNTeamProvider.getProvider( project ) ).getRepositoryLocation();
        IAuthData data = AuthFacade.getAuthService().select( location.getUrl() );
        assertNotNull( "Cannot find auth data for " + location.getUrl(), data );
        assertEquals( "Saved username does not match", data.getUsername(), location.getUsername() );
        assertEquals( "Saved password does not match", data.getPassword(), location.getPassword() );
    }
}
