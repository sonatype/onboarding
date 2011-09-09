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
package com.sonatype.nexus.proxy.p2.its.nxcm1955;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.UserMessageUtil;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.sonatype.nexus.p2.rest.model.P2LineupSummaryDto;
import com.sonatype.nexus.proxy.p2.its.AbstractSecureP2LineupIT;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupInstallableUnit;
import com.sonatype.s2.p2lineup.model.P2LineupSourceRepository;
import com.sonatype.s2.p2lineup.model.io.xstream.XStreamUtil;
import com.thoughtworks.xstream.XStream;

public class Nxcm1955SecureRestResourceIT
    extends AbstractSecureP2LineupIT
{
    private static final String URI_PART = RequestFacade.SERVICE_LOCAL + "p2/lineups";

    private final String p2RepoUrl;

    private final String p2IUId;

    private final String p2IUVersion;

    protected UserMessageUtil userUtil;

    public Nxcm1955SecureRestResourceIT()
    {
        super( "p2lineups" );
        p2RepoUrl = "${nexus.baseURL}content/repositories/p2proxy";
        p2IUId = "com.sonatype.nexus.p2.its.bundle";
        p2IUVersion = "1.0.0";
    }

    @Test
    public void testPutAccess()
        throws Exception
    {
        this.giveUserRole( TEST_USER_NAME, "p2-all-read" );

        // user can create lineups but it depends on the target privs he has.
        this.giveUserPrivilege( TEST_USER_NAME, "p2-lineup-create" );
        this.giveUserPrivilege( TEST_USER_NAME, "p2-lineup-update" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        // PUT
        P2Lineup lineup = new P2Lineup();
        lineup.setId( "testPutAccess" );
        lineup.setGroupId( "groupid.one" );
        lineup.setVersion( "1.2.3" );
        lineup.setName( "name1" );
        lineup.setDescription( "description one" );
        lineup.addRepository( new P2LineupSourceRepository( p2RepoUrl ) );
        lineup.addRootInstallableUnit( new P2LineupInstallableUnit( p2IUId, p2IUVersion ) );
        XStreamRepresentation representation = new XStreamRepresentation(
            getXMLXStream(),
            "",
            MediaType.APPLICATION_XML );
        representation.setPayload( lineup );
        Response response = null;
        try
        {
            response = RequestFacade.sendMessage(
                URI_PART + "/groupid/one/testPutAccess/1.2.3",
                Method.PUT,
                representation );
            String responseText = response.getEntity().getText();

            Assert
                .assertEquals( "Response Text:\n" + responseText, Status.CLIENT_ERROR_FORBIDDEN, response.getStatus() );

            // give user access to .* we have UT's that test the actual target privs, we just want to make sure the
            // response
            // codes are correct. (and that it gets created)
            this.giveUserPrivilege( TEST_USER_NAME, "p2lineup-create" ); // target priv
            this.giveUserPrivilege( TEST_USER_NAME, "p2lineup-update" ); // target priv

            // this method switch the user, it should have been _more smarter_ (get it, ha!)
            TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
            TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }

        // now the user should be able to create a lineup
        try
        {
            response = RequestFacade.sendMessage(
                URI_PART + "/groupid/one/testPutAccess/1.2.3",
                Method.PUT,
                representation );
            String responseText = response.getEntity().getText();
            assertResponse( response, responseText );

            P2LineupSummaryDto resultLineup = getObjectResponseText( responseText, new P2LineupSummaryDto() );
            Assert.assertEquals( "testPutAccess", resultLineup.getId() );
            Assert.assertEquals( "groupid.one", resultLineup.getGroupId() );
            Assert.assertEquals( "1.2.3", resultLineup.getVersion() );
            Assert.assertEquals( "name1", resultLineup.getName() );
            Assert.assertEquals( "description one", resultLineup.getDescription() );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }

    }

    @Test
    public void testGetAccess()
        throws Exception
    {
        P2Lineup lineup = new P2Lineup();
        lineup.setId( "testGetAccess" );
        lineup.setGroupId( "groupid.one" );
        lineup.setVersion( "1.2.3" );
        lineup.setName( "name1" );
        lineup.setDescription( "description one" );
        lineup.addRepository( new P2LineupSourceRepository( p2RepoUrl ) );
        lineup.addRootInstallableUnit( new P2LineupInstallableUnit( p2IUId, p2IUVersion ) );
        XStreamRepresentation representation = new XStreamRepresentation(
            getXMLXStream(),
            "",
            MediaType.APPLICATION_XML );
        representation.setPayload( lineup );
        Response response = null;
        try
        {
            response = RequestFacade.sendMessage(
                URI_PART + "/groupid/one/testGetAccess/1.2.3",
                Method.PUT,
                representation );
            String responseText = response.getEntity().getText();

            Assert.assertEquals( "Response Text:\n" + responseText, Status.SUCCESS_OK, response.getStatus() );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }
        // that was the admin, now use the test user
        this.giveUserPrivilege( TEST_USER_NAME, "p2-lineup-read" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        try
        {
            response = RequestFacade.doGetRequest( URI_PART + "/groupid/one/testGetAccess/1.2.3" );
            String responseText = response.getEntity().getText();

            Assert
                .assertEquals( "Response Text:\n" + responseText, Status.CLIENT_ERROR_FORBIDDEN, response.getStatus() );

            this.giveUserPrivilege( TEST_USER_NAME, "p2lineup-read" ); // target priv
            TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
            TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }

        try
        {
            response = RequestFacade.doGetRequest( URI_PART + "/groupid/one/testGetAccess/1.2.3" );
            String responseText = response.getEntity().getText();

            Assert.assertEquals( "Response Text:\n" + responseText, Status.SUCCESS_OK, response.getStatus() );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }
    }

    @Test
    public void testDeleteAccess()
        throws Exception
    {

        P2Lineup lineup = new P2Lineup();
        lineup.setId( "testDeleteAccess" );
        lineup.setGroupId( "groupid.one" );
        lineup.setVersion( "1.2.3" );
        lineup.setName( "name1" );
        lineup.setDescription( "description one" );
        lineup.addRepository( new P2LineupSourceRepository( p2RepoUrl ) );
        lineup.addRootInstallableUnit( new P2LineupInstallableUnit( p2IUId, p2IUVersion ) );
        XStreamRepresentation representation = new XStreamRepresentation(
            getXMLXStream(),
            "",
            MediaType.APPLICATION_XML );
        representation.setPayload( lineup );
        Response response = null;
        try
        {
            response = RequestFacade.sendMessage(
                URI_PART + "/groupid/one/testDeleteAccess/1.2.3",
                Method.PUT,
                representation );
            String responseText = response.getEntity().getText();

            Assert.assertEquals( "Response Text:\n" + responseText, Status.SUCCESS_OK, response.getStatus() );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }

        // that was the admin, now use the test user
        this.giveUserPrivilege( TEST_USER_NAME, "p2-lineup-delete" );
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        try
        {
            response = RequestFacade.sendMessage( URI_PART + "/groupid/one/testDeleteAccess/1.2.3", Method.DELETE );
            String responseText = response.getEntity().getText();

            Assert
                .assertEquals( "Response Text:\n" + responseText, Status.CLIENT_ERROR_FORBIDDEN, response.getStatus() );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }
        this.giveUserPrivilege( TEST_USER_NAME, "p2lineup-delete" ); // target priv
        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        try
        {
            response = RequestFacade.sendMessage( URI_PART + "/groupid/one/testDeleteAccess/1.2.3", Method.DELETE );
            String responseText = response.getEntity().getText();

            Assert.assertEquals( "Response Text:\n" + responseText, Status.SUCCESS_NO_CONTENT, response.getStatus() );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }

    }

    private void assertResponse( Response response, String reponseText )
    {
        if ( !response.getStatus().isSuccess() )
        {
            Throwable error = response.getStatus().getThrowable();
            if ( error != null )
            {
                error.printStackTrace();
            }
        }
        Assert.assertTrue( "Status: " + response.getStatus() + "\nURL:" + response.getRequest().getResourceRef()
            + "\nResponseText:\n" + reponseText, response.getStatus().isSuccess() );
    }

    private <T> T getObjectResponseText( String responseText, T expectedType )
    {
        return XStreamUtil.unmarshal( responseText, getXMLXStream(), expectedType );
    }

    @Override
    public XStream getJsonXStream()
    {
        return XStreamUtil.initializeXStream( super.getJsonXStream() );
    }

    @Override
    public XStream getXMLXStream()
    {
        return XStreamUtil.initializeXStream( super.getXMLXStream() );
    }
}
