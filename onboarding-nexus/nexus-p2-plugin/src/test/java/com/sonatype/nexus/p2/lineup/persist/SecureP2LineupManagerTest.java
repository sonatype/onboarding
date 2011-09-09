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
package com.sonatype.nexus.p2.lineup.persist;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.email.NexusEmailer;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeRepositoryPropertyDescriptor;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeRepositoryTargetPropertyDescriptor;
import org.sonatype.nexus.proxy.target.Target;
import org.sonatype.nexus.proxy.target.TargetRegistry;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authorization.AuthorizationManager;
import org.sonatype.security.authorization.Privilege;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegeMethodPropertyDescriptor;
import org.sonatype.security.usermanagement.DefaultUser;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserStatus;

import com.sonatype.nexus.p2.lineup.resolver.CannotResolveP2LineupException;
import com.sonatype.s2.p2lineup.model.P2Lineup;

public class SecureP2LineupManagerTest
    extends AbstractP2LineupManagerTest
{

    private SecuritySystem securitySystem;

    private User testUser;

    private AuthorizationManager authzManager;

    private Role userRole;

    // privileges
    private String createPrivId;

    private String readPrivId;

    private String updatePrivId;

    private String deletePrivId;

    private String readTestPrivId;

    private Subject subject;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        this.lookup( NexusEmailer.class ).configure( this.lookup( ApplicationConfiguration.class ) );

        TargetRegistry targetRegistry = this.lookup( TargetRegistry.class );
        String allTargetId = "all-p2-lineups";
        String testTargetId = "test-p2-lineups";

        targetRegistry.addRepositoryTarget( new Target( allTargetId, "All P2 Lineups",
            this.getP2LineupRepository().getRepositoryContentClass(), Collections.singleton( ".*" ) ) );
        targetRegistry.addRepositoryTarget( new Target( testTargetId, "TESTTEST P2 Lineups",
            this.getP2LineupRepository().getRepositoryContentClass(), Collections.singleton( ".*TESTTEST.*" ) ) );
        targetRegistry.commitChanges();

        this.securitySystem = this.lookup( SecuritySystem.class );
        this.securitySystem.start();

        this.authzManager = this.securitySystem.getAuthorizationManager( "default" );

        this.userRole = new Role();
        userRole.setRoleId( "test-role" );
        userRole.setDescription( "test role" );
        userRole.setName( "test role" );
        userRole.setReadOnly( false );
        userRole.setSource( "default" );
        this.authzManager.addRole( userRole );

        this.testUser = new DefaultUser();
        testUser.setName( "testuser" );
        testUser.setUserId( "testuser" );
        testUser.setEmailAddress( "testuser@foo.com" );
        testUser.setReadOnly( false );
        testUser.setStatus( UserStatus.active );
        testUser.setSource( "default" );
        testUser.addRole( new RoleIdentifier( "default", this.userRole.getRoleId() ) );

        this.securitySystem.addUser( testUser, "password" );

        this.createPrivId =
            this.createTargetPrivilege( "SecureP2LineupManagerTest", "create", allTargetId,
                this.getP2LineupRepository().getId() ).getId();
        this.readPrivId =
            this.createTargetPrivilege( "SecureP2LineupManagerTest", "read", allTargetId,
                this.getP2LineupRepository().getId() ).getId();
        this.updatePrivId =
            this.createTargetPrivilege( "SecureP2LineupManagerTest", "update", allTargetId,
                this.getP2LineupRepository().getId() ).getId();
        this.deletePrivId =
            this.createTargetPrivilege( "SecureP2LineupManagerTest", "delete", allTargetId,
                this.getP2LineupRepository().getId() ).getId();

        // TESTTEST target
        this.readTestPrivId =
            this.createTargetPrivilege( "SecureP2LineupManagerTest", "read", testTargetId,
                this.getP2LineupRepository().getId() ).getId();

        // now log the user in (we are keeping him logged in and just changing his roles/privs)
        this.subject =
            this.securitySystem.login( new UsernamePasswordToken( testUser.getUserId(), "password".toCharArray() ) );
    }

    @Override
    public void tearDown()
        throws Exception
    {
        this.subject.logout();
        super.tearDown();
    }

    private void giveUserPermission( String... privIds )
    {
        this.userRole.setPrivileges( new HashSet<String>( Arrays.asList( privIds ) ) );
        try
        {
            this.authzManager.updateRole( this.userRole );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            Assert.fail( "Could not update Role" );
        }
    }

    private void clearPermissions()
    {
        this.userRole.setPrivileges( new HashSet<String>() );
        try
        {
            this.authzManager.updateRole( this.userRole );

            // Clear cache: This is a hack, the correct way to do this is to fire an event. But it should be easier then
            // that. NEXUS-3435
            this.securitySystem.start();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            Assert.fail( "Could not update Role" );
        }
    }

    @Test
    public void testAddLineup()
        throws Exception
    {

        P2Lineup lineup = new P2Lineup();
        lineup.setDescription( "description" );
        lineup.setGroupId( "groupId-testAddLineup" );
        lineup.setId( "id-testAddLineup" );
        lineup.setVersion( "version-testAddLineup" );

        try
        {
            this.getP2LineupManager().addLineup( lineup );
            Assert.fail( "Expected Security Exception" );
        }
        catch ( P2AccessDeniedException e )
        {
            // expected
        }
        // give the user the priv and try again
        this.giveUserPermission( this.createPrivId );
        this.getP2LineupManager().addLineup( lineup );
        this.validateLineup( lineup );
    }

    @Test
    public void testReadLineup()
        throws Exception
    {
        this.giveUserPermission( this.createPrivId );

        // add it
        P2Lineup lineup = new P2Lineup();
        lineup.setDescription( "description" );
        lineup.setGroupId( "groupId-testReadLineup" );
        lineup.setId( "id-testReadLineup" );
        lineup.setVersion( "version-testReadLineup" );
        this.getP2LineupManager().addLineup( lineup );

        // remove priv
        this.clearPermissions();

        // try to read it
        P2Gav gav = new P2Gav( lineup );

        try
        {
            this.getP2LineupManager().getLineup( gav );
            Assert.fail( "Expected Security Exception" );
        }
        catch ( P2AccessDeniedException e )
        {
            // expected
        }
        // give the user the priv and try again
        this.giveUserPermission( this.readPrivId );
        P2Lineup resultLineup = this.getP2LineupManager().getLineup( gav );
        Assert.assertEquals( lineup, resultLineup );

    }

    @Test
    public void testUpdateLineup()
        throws Exception
    {
        this.giveUserPermission( this.createPrivId );

        // add it
        P2Lineup lineup = new P2Lineup();
        lineup.setDescription( "description" );
        lineup.setGroupId( "groupId-testUpdateLineup" );
        lineup.setId( "id-testUpdateLineup" );
        lineup.setVersion( "version-testUpdateLineup" );
        this.getP2LineupManager().addLineup( lineup );

        // remove priv
        this.clearPermissions();

        lineup.setName( "UPDATED" );

        // try to update it
        try
        {
            this.getP2LineupManager().updateLineup( lineup );
            Assert.fail( "Expected Security Exception" );
        }
        catch ( P2AccessDeniedException e )
        {
            // expected
        }
        // give the user the priv and try again
        this.giveUserPermission( this.updatePrivId );
        this.getP2LineupManager().updateLineup( lineup );
        this.validateLineup( lineup );

    }

    @Test
    public void testDeleteLineup()
        throws Exception
    {
        this.giveUserPermission( this.createPrivId );

        // add it
        P2Lineup lineup = new P2Lineup();
        lineup.setDescription( "description" );
        lineup.setGroupId( "groupId-testDeleteLineup" );
        lineup.setId( "id-testDeleteLineup" );
        lineup.setVersion( "version-testDeleteLineup" );
        this.getP2LineupManager().addLineup( lineup );

        // remove priv
        this.clearPermissions();

        // try to remove it
        P2Gav gav = new P2Gav( lineup );
        try
        {
            this.getP2LineupManager().deleteLineup( gav );
            Assert.fail( "Expected Security Exception" );
        }
        catch ( P2AccessDeniedException e )
        {
            // expected
        }
        // give the user the priv and try again
        this.giveUserPermission( this.deletePrivId );
        this.getP2LineupManager().deleteLineup( gav );
        Assert.assertFalse( "Lineup should have been removed: " + this.getLineupFile( lineup ),
            this.getLineupFile( lineup ).exists() );

    }

    @Test
    public void testGetList()
        throws CannotResolveP2LineupException, P2LineupStorageException, P2AccessDeniedException,
        P2ConfigurationException
    {
        this.giveUserPermission( this.readPrivId );
        int numOldLineups = this.getP2LineupManager().getLineups().size();
        this.clearPermissions();

        this.giveUserPermission( this.createPrivId );

        // add it
        P2Lineup lineup = new P2Lineup();
        lineup.setDescription( "description" );
        lineup.setGroupId( "groupId-testGetList1" );
        lineup.setId( "id-testGetList1" );
        lineup.setVersion( "version-testGetList1" );
        this.getP2LineupManager().addLineup( lineup );

        // add it
        P2Lineup lineup2 = new P2Lineup();
        lineup2.setDescription( "description" );
        lineup2.setGroupId( "groupId-TESTTEST" );
        lineup2.setId( "id-testGetList2" );
        lineup2.setVersion( "version-testGetList2" );
        this.getP2LineupManager().addLineup( lineup2 );

        // remove priv
        this.clearPermissions();

        // now the list should be empty
        Assert.assertEquals( 0, this.getP2LineupManager().getLineups().size() );

        this.giveUserPermission( this.readPrivId );
        Assert.assertEquals( numOldLineups + 2, this.getP2LineupManager().getLineups().size() );

        // now filter for a user
        this.giveUserPermission( this.readTestPrivId );
        Set<P2Lineup> lineups = this.getP2LineupManager().getLineups();
        Assert.assertEquals( 1, lineups.size() );
        Assert.assertEquals( "id-testGetList2", lineups.iterator().next().getId() );

    }

    private Privilege createTargetPrivilege( String name, String method, String targetId, String repoId )
        throws InvalidConfigurationException
    {
        Privilege priv = new Privilege();

        priv.setId( name + "-" + method );
        priv.setName( name + " - (" + method + ")" );
        priv.setDescription( priv.getName() );
        priv.setType( TargetPrivilegeDescriptor.TYPE );

        priv.addProperty( ApplicationPrivilegeMethodPropertyDescriptor.ID, method );

        priv.addProperty( TargetPrivilegeRepositoryTargetPropertyDescriptor.ID, targetId );

        priv.addProperty( TargetPrivilegeRepositoryPropertyDescriptor.ID, repoId );

        return this.authzManager.addPrivilege( priv );
    }
}
