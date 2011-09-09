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
package com.sonatype.nexus.proxy.p2.its;

import java.io.IOException;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.restlet.data.MediaType;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.RoleMessageUtil;
import org.sonatype.nexus.test.utils.UserMessageUtil;
import org.sonatype.security.rest.model.RoleResource;
import org.sonatype.security.rest.model.UserResource;

import com.thoughtworks.xstream.XStream;

public abstract class AbstractSecureP2LineupIT
    extends AbstractP2LineupIT
{
    protected UserMessageUtil userUtil;

    protected RoleMessageUtil roleUtil;

    protected static final String TEST_USER_NAME = "test-user";

    protected static final String TEST_USER_PASSWORD = "admin123";

    public AbstractSecureP2LineupIT( String p2LineupRepoId )
    {
        super( p2LineupRepoId );
        try
        {
            this.init();
        }
        catch ( ComponentLookupException e )
        {
            Assert.fail( e.getMessage() );
        }
    }

    // Copied from AbstractPrivilegeTest
    @Before
    public void resetTestUserPrivs()
        throws Exception
    {
        TestContainer.getInstance().getTestContext().useAdminForRequests();

        UserResource testUser = this.userUtil.getUser( TEST_USER_NAME );
        testUser.getRoles().clear();
        testUser.addRole( "anonymous" );
        this.userUtil.updateUser( testUser );
    }

    // Copied from AbstractPrivilegeTest
    @Override
    @After
    public void afterTest()
        throws Exception
    {
        // reset any password
        TestContainer.getInstance().getTestContext().useAdminForRequests();
    }

    // Copied from AbstractPrivilegeTest
    private void init()
        throws ComponentLookupException
    {
        // turn on security for the test
        TestContainer.getInstance().getTestContext().setSecureTest( true );

        XStream xstream = this.getXMLXStream();

        this.userUtil = new UserMessageUtil( this, xstream, MediaType.APPLICATION_XML );
        this.roleUtil = new RoleMessageUtil( this, xstream, MediaType.APPLICATION_XML );
        TestContainer.getInstance().getTestContext().setSecureTest( true );
    }

    // Copied from AbstractPrivilegeTest
    protected void giveUserRole( String userId, String roleId )
        throws IOException
    {
        // use admin
        TestContainer.getInstance().getTestContext().useAdminForRequests();

        // add it
        UserResource testUser = this.userUtil.getUser( userId );
        testUser.addRole( roleId );
        this.userUtil.updateUser( testUser );
    }

    // Copied from AbstractPrivilegeTest
    protected void giveUserPrivilege( String userId, String priv )
        throws IOException
    {
        // use admin
        TestContainer.getInstance().getTestContext().useAdminForRequests();

        RoleResource role = null;

        // first try to retrieve
        for ( RoleResource roleResource : roleUtil.getList() )
        {
            if ( roleResource.getName().equals( priv + "Role" ) )
            {
                role = roleResource;

                if ( !role.getPrivileges().contains( priv ) )
                {
                    role.addPrivilege( priv );
                    // update the permissions
                    RoleMessageUtil.update( role );
                }
                break;
            }
        }

        if ( role == null )
        {
            // now give create
            role = new RoleResource();
            role.setDescription( priv + " Role" );
            role.setName( priv + "Role" );
            role.setSessionTimeout( 60 );
            role.addPrivilege( priv );
            // save it
            role = this.roleUtil.createRole( role );
        }

        // add it
        this.giveUserRole( userId, role.getId() );
    }
}
