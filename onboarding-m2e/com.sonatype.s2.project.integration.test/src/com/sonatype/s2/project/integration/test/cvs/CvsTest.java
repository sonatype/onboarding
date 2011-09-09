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
package com.sonatype.s2.project.integration.test.cvs;

import java.net.UnknownHostException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.integration.test.AbstractMavenProjectMaterializationTest;
import com.sonatype.s2.project.tests.common.Util;
import com.sonatype.s2.project.validation.cvs.Messages;

public class CvsTest
    extends AbstractMavenProjectMaterializationTest
{
    /*
     * The ext method is currently unsupported.
     */
    public void testExtNotAccepted()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        try
        {
            materializeProjects( httpServer.getHttpUrl() + "/catalogs/cvs", "Project-B" );
            fail( "A CoreException should have been thrown" );
        }
        catch ( CoreException e )
        {
            if ( !"CVS method ext is unsupported.".equals( e.getMessage() ) )
            {
                throw e;
            }
        }
    }

    /*
     * The extssh method is currently unsupported.
     */
    public void testExtSshNotAccepted()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        try
        {
            materializeProjects( httpServer.getHttpUrl() + "/catalogs/cvs", "Project-C" );
            fail( "A CoreException should have been thrown" );
        }
        catch ( CoreException e )
        {
            if ( !"CVS method extssh is unsupported.".equals( e.getMessage() ) )
            {
                throw e;
            }
        }
    }

    /**
     * The original CVS URI cvs://[:]method:user[:password]@host:[port]/root/path#project/path[,tagName]
     */
    public void testCvsOriginalUri()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();
        addRealmAndURL( "testCvsOriginalUri", "cvs://:pserver:scmtest.grid.sonatype.com:/cvs", "hudson", "hudson" );
        materializeProjects( httpServer.getHttpUrl() + "/catalogs/cvs", "Project-A" );

        assertMavenProject( "test", "cvs-test", "0.0.1-SNAPSHOT" );
        Util.waitForTeamSharingJobs();
        assertWorkspaceProjectShared( "Test" );
    }

    /**
     * The original CVS URI cvs://[:]method:user[:password]@host:[port]/root/path#project/path[,tagName]
     */
    public void testCvsUsernameInURI()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();
        addRealmAndURL( "testCvsOriginalUri", "cvs://:pserver:hudson@scmtest.grid.sonatype.com:/cvs", "hudson",
                        "hudson" );
        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/cvs-username-in-uri.xml" );
        assertFalse( status.isOK() );
        assertEquals( status.toString(), Messages.username_in_uri, status.getChildren()[0].getMessage() );
    }

    /**
     * The Eclipse CVSURI class defines new style URIs as the following:
     * cvs://_method_user[_password]~host_[port]!root!path/project/path[?<version,branch,date,revision>=tagName]
     */
    public void testCvsNewStyleUri()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();
        addRealmAndURL( "testCvsOriginalUri", "cvs://_pserver_scmtest.grid.sonatype.com_!cvs/mse/s2/cvs-test",
                        "hudson",
                        "hudson" );
        materializeProjects( httpServer.getHttpUrl() + "/catalogs/cvs", "Project-D" );

        assertMavenProject( "test", "cvs-test", "0.0.1-SNAPSHOT" );
        Util.waitForTeamSharingJobs();
        assertWorkspaceProjectShared( "Test" );
    }

    /*
     * Tests SCM validation with an invalid host
     */
    public void testValidateBadHost()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();
        addRealmAndURL( "testValidateBadHost", "cvs://:pserver:scmtest.grid.sonatype.com:/cvs", "hudson", "hudson" );

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/cvs-bad-host.xml" );
        assertTrue( status.toString(), getException( status ) instanceof UnknownHostException );
    }

    /*
     * Tests SCM Validation with invalid credentials
     */
    public void testValidateInvalidCredentials()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "testValidateInvalidCredentials", "cvs://:pserver:scmtest.grid.sonatype.com:/cvs#", "bad",
                        "password" );

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/cvs-pserver.xml" );
        assertTrue( status.toString(), isUnauthorizedStatus( status ) );
    }

    /*
     * Tests SCM Validation does not support ext
     */
    public void testValidateExt()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "testValidateInvalidCredentials",
                        "cvs://:extssh:scmtest.grid.sonatype.com:/opt/repos/cvs/cvs#", "hudson", "S0natyp3@" );

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/cvs-extssh.xml" );
        assertErrorStatus( status, "The CVS method extssh is unsupported." );
    }

    /*
     * Tests SCM Validation when URI is missing a path
     */
    public void testValidateMissingPath()
        throws Exception
    {
        HttpServer httpServer = startHttpServer();

        addRealmAndURL( "testValidateMissingPath", "cvs://:pserver:scmtest.grid.sonatype.com:/cvs", "hudson",
                        "hudson" );

        IStatus status = validateScmAccess( httpServer.getHttpUrl() + "/projects/cvs-missing-path.xml" );
        assertErrorStatus( status, "The URI is missing path information." );
    }

    private Throwable getException( IStatus status )
    {
        if ( status.getException() != null )
        {
            return status.getException();
        }
        if ( status.isMultiStatus() )
        {
            for ( IStatus child : status.getChildren() )
            {
                Throwable childThrowable = getException( child );
                if ( childThrowable != null )
                {
                    return childThrowable;
                }
            }
        }
        return null;
    }
    //
    // private static class Listener
    // implements ICVSDecoratorEnablementListener
    // {
    // private boolean asdf;
    //
    // private Object lock = new Object();
    //
    // public void decoratorEnablementChanged( boolean enabled )
    // {
    // synchronized ( lock )
    // {
    // asdf = enabled;
    // }
    // }
    //
    // void waitOnEnablement()
    // {
    // while ( true )
    // {
    // try
    // {
    // synchronized ( lock )
    // {
    // if ( asdf )
    // break;
    // }
    // Thread.sleep( 2000L );
    // }
    // catch ( InterruptedException e )
    // {
    // e.printStackTrace();
    // }
    // }
    // }
    // }
}