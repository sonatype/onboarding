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
package com.sonatype.nexus.p2.auth;

import java.net.URI;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.junit.Test;

public class P2AuthSessionTest
{
    @Test
    public void setCredentials_Basic()
        throws Exception
    {
        URI testURI = new URI("http://fakeForP2AuthSessionTest/tests");

        String nodeName = P2AuthSession.getNodeNameForUnitTests( testURI );

        ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
        Assert.assertFalse( securePreferences.nodeExists( nodeName ) );

        P2AuthSession authSession = new P2AuthSession();
        try
        {
            String username = "Mark";
            String password = "MyWords";

            setAndAssertCredentials( authSession, nodeName, testURI, username, password );

            Map<String, Integer> globalNodeRefCounts = P2AuthSession.getGlobalNodeRefCountsForUnitTests();
            Assert.assertEquals( 1, globalNodeRefCounts.size() );
            Assert.assertEquals( 1, (int) globalNodeRefCounts.get( nodeName ) );
        }
        finally
        {
            authSession.cleanup();
            Assert.assertFalse( securePreferences.nodeExists( nodeName ) );
            Assert.assertEquals( 0, P2AuthSession.getGlobalNodeRefCountsForUnitTests().size() );
            Assert.assertEquals( 0, authSession.getNodeNamesToCleanupForUnitTests().size() );
            Assert.assertFalse( securePreferences.nodeExists( nodeName ) );
        }
    }

    @Test
    public void setCredentials_DifferentHostnames()
        throws Exception
    {
        URI testURI1 = new URI( "http://fakeForP2AuthSessionTest1/tests" );
        URI testURI2 = new URI( "http://fakeForP2AuthSessionTest2/tests" );

        String nodeName1 = P2AuthSession.getNodeNameForUnitTests( testURI1 );
        String nodeName2 = P2AuthSession.getNodeNameForUnitTests( testURI2 );

        ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
        Assert.assertFalse( securePreferences.nodeExists( nodeName1 ) );
        Assert.assertFalse( securePreferences.nodeExists( nodeName2 ) );

        P2AuthSession authSession1 = new P2AuthSession();
        try
        {
            String username1 = "Mark";
            String password1 = "MyWords";

            setAndAssertCredentials( authSession1, nodeName1, testURI1, username1, password1 );

            Map<String, Integer> globalNodeRefCounts = P2AuthSession.getGlobalNodeRefCountsForUnitTests();
            Assert.assertEquals( 1, globalNodeRefCounts.size() );
            Assert.assertEquals( 1, (int) globalNodeRefCounts.get( nodeName1 ) );

            P2AuthSession authSession2 = new P2AuthSession();
            try
            {
                String username2 = "Albedo";
                String password2 = "0.69";

                setAndAssertCredentials( authSession2, nodeName2, testURI2, username2, password2 );

                globalNodeRefCounts = P2AuthSession.getGlobalNodeRefCountsForUnitTests();
                Assert.assertEquals( 2, globalNodeRefCounts.size() );
                Assert.assertEquals( 1, (int) globalNodeRefCounts.get( nodeName1 ) );
                Assert.assertEquals( 1, (int) globalNodeRefCounts.get( nodeName2 ) );
            }
            finally
            {
                authSession2.cleanup();
                Assert.assertFalse( securePreferences.nodeExists( nodeName2 ) );
                Assert.assertEquals( 1, P2AuthSession.getGlobalNodeRefCountsForUnitTests().size() );
                Assert.assertEquals( 0, authSession2.getNodeNamesToCleanupForUnitTests().size() );
                Assert.assertEquals( 1, authSession1.getNodeNamesToCleanupForUnitTests().size() );
                Assert.assertFalse( securePreferences.nodeExists( nodeName2 ) );
            }
        }
        finally
        {
            authSession1.cleanup();
            Assert.assertFalse( securePreferences.nodeExists( nodeName1 ) );
            Assert.assertEquals( 0, P2AuthSession.getGlobalNodeRefCountsForUnitTests().size() );
            Assert.assertEquals( 0, authSession1.getNodeNamesToCleanupForUnitTests().size() );
            Assert.assertFalse( securePreferences.nodeExists( nodeName1 ) );
        }
    }

    @Test
    public void setCredentials_SameHostname_DifferentURIs()
        throws Exception
    {
        URI testURI1 = new URI( "http://fakeForP2AuthSessionTest/tests" );
        URI testURI2 = new URI( "http://fakeForP2AuthSessionTest/tests1" );

        String nodeName1 = P2AuthSession.getNodeNameForUnitTests( testURI1 );
        String nodeName2 = P2AuthSession.getNodeNameForUnitTests( testURI2 );
        Assert.assertEquals( nodeName1, nodeName2 );

        ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
        Assert.assertFalse( securePreferences.nodeExists( nodeName1 ) );

        P2AuthSession authSession1 = new P2AuthSession();
        try
        {
            String username1 = "Mark";
            String password1 = "MyWords";

            setAndAssertCredentials( authSession1, nodeName1, testURI1, username1, password1 );

            Map<String, Integer> globalNodeRefCounts = P2AuthSession.getGlobalNodeRefCountsForUnitTests();
            Assert.assertEquals( 1, globalNodeRefCounts.size() );
            Assert.assertEquals( 1, (int) globalNodeRefCounts.get( nodeName1 ) );

            P2AuthSession authSession2 = new P2AuthSession();
            String username2 = "Albedo";
            String password2 = "0.69";

            try
            {
                setAndAssertCredentials( authSession2, nodeName2, testURI2, username2, password2 );
                Assert.fail( "Expected exception" );
            }
            catch ( RuntimeException e )
            {
                if ( !"Cannot redefine credentials for URI=http://fakeForP2AuthSessionTest/tests1, nodeName=org.eclipse.equinox.p2.repository/fakeForP2AuthSessionTest".equals( e.getMessage() ) )
                {
                    throw e;
                }
            }

            globalNodeRefCounts = P2AuthSession.getGlobalNodeRefCountsForUnitTests();
            Assert.assertEquals( 1, globalNodeRefCounts.size() );
            Assert.assertEquals( 1, (int) globalNodeRefCounts.get( nodeName1 ) );
            Assert.assertTrue( securePreferences.nodeExists( nodeName1 ) );
        }
        finally
        {
            authSession1.cleanup();
            Assert.assertFalse( securePreferences.nodeExists( nodeName1 ) );
            Assert.assertEquals( 0, P2AuthSession.getGlobalNodeRefCountsForUnitTests().size() );
            Assert.assertEquals( 0, authSession1.getNodeNamesToCleanupForUnitTests().size() );
            Assert.assertFalse( securePreferences.nodeExists( nodeName1 ) );
        }
    }

    @Test
    public void setCredentials_DifferentSessions_SameCredentials()
        throws Exception
    {
        URI testURI = new URI( "http://fakeForP2AuthSessionTest/tests" );

        String nodeName = P2AuthSession.getNodeNameForUnitTests( testURI );

        ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
        Assert.assertFalse( securePreferences.nodeExists( nodeName ) );

        P2AuthSession authSession1 = new P2AuthSession();
        try
        {
            String username = "Mark";
            String password = "MyWords";

            setAndAssertCredentials( authSession1, nodeName, testURI, username, password );

            Map<String, Integer> globalNodeRefCounts = P2AuthSession.getGlobalNodeRefCountsForUnitTests();
            Assert.assertEquals( 1, globalNodeRefCounts.size() );
            Assert.assertEquals( 1, (int) globalNodeRefCounts.get( nodeName ) );

            P2AuthSession authSession2 = new P2AuthSession();
            try
            {
                setAndAssertCredentials( authSession2, nodeName, testURI, username, password );

                globalNodeRefCounts = P2AuthSession.getGlobalNodeRefCountsForUnitTests();
                Assert.assertEquals( 1, globalNodeRefCounts.size() );
                Assert.assertEquals( 2, (int) globalNodeRefCounts.get( nodeName ) );
            }
            finally
            {
                authSession2.cleanup();
                globalNodeRefCounts = P2AuthSession.getGlobalNodeRefCountsForUnitTests();
                Assert.assertEquals( 1, (int) globalNodeRefCounts.get( nodeName ) );
                Assert.assertEquals( 1, globalNodeRefCounts.size() );
                Assert.assertEquals( 0, authSession2.getNodeNamesToCleanupForUnitTests().size() );
                Assert.assertEquals( 1, authSession1.getNodeNamesToCleanupForUnitTests().size() );
                Assert.assertTrue( securePreferences.nodeExists( nodeName ) );
            }
        }
        finally
        {
            authSession1.cleanup();
            Assert.assertFalse( securePreferences.nodeExists( nodeName ) );
            Assert.assertEquals( 0, P2AuthSession.getGlobalNodeRefCountsForUnitTests().size() );
            Assert.assertEquals( 0, authSession1.getNodeNamesToCleanupForUnitTests().size() );
            Assert.assertFalse( securePreferences.nodeExists( nodeName ) );
        }
    }

    @Test
    public void redefineCredentials_Concurrent()
        throws Exception
    {
        URI testURI = new URI( "http://fakeForP2AuthSessionTest/tests" );

        String nodeName = P2AuthSession.getNodeNameForUnitTests( testURI );

        ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
        Assert.assertFalse( securePreferences.nodeExists( nodeName ) );

        P2AuthSession authSession = new P2AuthSession();
        try
        {
            String username = "Mark";
            String password = "MyWords";

            setAndAssertCredentials( authSession, nodeName, testURI, username, password );

            Map<String, Integer> globalNodeRefCounts = P2AuthSession.getGlobalNodeRefCountsForUnitTests();
            Assert.assertEquals( 1, globalNodeRefCounts.size() );
            Assert.assertEquals( 1, (int) globalNodeRefCounts.get( nodeName ) );

            P2AuthSession authSession2 = new P2AuthSession();
            try
            {
                authSession2.setCredentials( testURI, username, "New password" );
                Assert.fail( "Expected exception" );
            }
            catch ( RuntimeException e )
            {
                if ( !"Cannot redefine credentials for URI=http://fakeForP2AuthSessionTest/tests, nodeName=org.eclipse.equinox.p2.repository/fakeForP2AuthSessionTest".equals( e.getMessage() ) )
                {
                    throw e;
                }
            }

            P2AuthSession authSession3 = new P2AuthSession();
            try
            {
                authSession3.setCredentials( testURI, "NewUser", password );
                Assert.fail( "Expected exception" );
            }
            catch ( RuntimeException e )
            {
                if ( !"Cannot redefine credentials for URI=http://fakeForP2AuthSessionTest/tests, nodeName=org.eclipse.equinox.p2.repository/fakeForP2AuthSessionTest".equals( e.getMessage() ) )
                {
                    throw e;
                }
            }
        }
        finally
        {
            authSession.cleanup();
            Assert.assertFalse( securePreferences.nodeExists( nodeName ) );
            Assert.assertEquals( 0, P2AuthSession.getGlobalNodeRefCountsForUnitTests().size() );
            Assert.assertEquals( 0, authSession.getNodeNamesToCleanupForUnitTests().size() );
            Assert.assertFalse( securePreferences.nodeExists( nodeName ) );
        }
    }

    @Test
    public void redefineCredentials_Sequential()
        throws Exception
    {
        URI testURI = new URI( "http://fakeForP2AuthSessionTest/tests" );

        String nodeName = P2AuthSession.getNodeNameForUnitTests( testURI );

        ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
        Assert.assertFalse( securePreferences.nodeExists( nodeName ) );

        P2AuthSession authSession = new P2AuthSession();
        try
        {
            String username = "Mark";
            String password = "MyWords";

            setAndAssertCredentials( authSession, nodeName, testURI, username, password );

            Map<String, Integer> globalNodeRefCounts = P2AuthSession.getGlobalNodeRefCountsForUnitTests();
            Assert.assertEquals( 1, globalNodeRefCounts.size() );
            Assert.assertEquals( 1, (int) globalNodeRefCounts.get( nodeName ) );
        }
        finally
        {
            authSession.cleanup();
            Assert.assertFalse( securePreferences.nodeExists( nodeName ) );
            Assert.assertEquals( 0, P2AuthSession.getGlobalNodeRefCountsForUnitTests().size() );
            Assert.assertEquals( 0, authSession.getNodeNamesToCleanupForUnitTests().size() );
            Assert.assertFalse( securePreferences.nodeExists( nodeName ) );
        }

        P2AuthSession authSession2 = new P2AuthSession();
        try
        {
            String username = "ByeBye";
            String password = "And thanks for all the fish";

            setAndAssertCredentials( authSession2, nodeName, testURI, username, password );

            Map<String, Integer> globalNodeRefCounts = P2AuthSession.getGlobalNodeRefCountsForUnitTests();
            Assert.assertEquals( 1, globalNodeRefCounts.size() );
            Assert.assertEquals( 1, (int) globalNodeRefCounts.get( nodeName ) );
        }
        finally
        {
            authSession2.cleanup();
            Assert.assertFalse( securePreferences.nodeExists( nodeName ) );
            Assert.assertEquals( 0, P2AuthSession.getGlobalNodeRefCountsForUnitTests().size() );
            Assert.assertEquals( 0, authSession2.getNodeNamesToCleanupForUnitTests().size() );
            Assert.assertFalse( securePreferences.nodeExists( nodeName ) );
        }
    }

    private void setAndAssertCredentials( P2AuthSession authSession, String nodeName, URI testURI, String username, String password )
        throws StorageException
    {
        authSession.setCredentials( testURI, username, password );
        ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
        Assert.assertTrue( securePreferences.nodeExists( nodeName ) );

        ISecurePreferences prefNode = securePreferences.node( nodeName );
        Assert.assertEquals( username, prefNode.get( IRepository.PROP_USERNAME, null ) );
        Assert.assertEquals( password, prefNode.get( IRepository.PROP_PASSWORD, null ) );

        List<String> nodeNamesToCleanup = authSession.getNodeNamesToCleanupForUnitTests();
        Assert.assertEquals( 1, nodeNamesToCleanup.size() );
        Assert.assertEquals( nodeName, nodeNamesToCleanup.get( 0 ) );
    }
}
