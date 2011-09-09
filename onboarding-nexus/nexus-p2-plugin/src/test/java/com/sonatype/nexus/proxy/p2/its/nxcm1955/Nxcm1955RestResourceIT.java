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

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.sonatype.nexus.p2.lineup.repository.P2LineupConstants;
import com.sonatype.nexus.p2.rest.model.P2LineupError;
import com.sonatype.nexus.p2.rest.model.P2LineupErrorResponse;
import com.sonatype.nexus.p2.rest.model.P2LineupListResponse;
import com.sonatype.nexus.p2.rest.model.P2LineupSummaryDto;
import com.sonatype.nexus.proxy.p2.its.AbstractP2LineupIT;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupInstallableUnit;
import com.sonatype.s2.p2lineup.model.P2LineupSourceRepository;
import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Reader;
import com.sonatype.s2.p2lineup.model.io.xstream.P2LineupXstreamIO;
import com.sonatype.s2.p2lineup.model.io.xstream.XStreamUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;

public class Nxcm1955RestResourceIT
    extends AbstractP2LineupIT
{
    private static final String URI_PART = RequestFacade.SERVICE_LOCAL + "p2/lineups";

    private final String p2RepoUrl;

    private final String p2IUId;

    private final String p2IUVersion;

    public Nxcm1955RestResourceIT()
    {
        super( "p2lineups" );
        p2RepoUrl = "${nexus.baseURL}content/repositories/p2proxy";
        p2IUId = "com.sonatype.nexus.p2.its.bundle";
        p2IUVersion = "1.0.0";
    }

    @Test
    public void simpleTest()
        throws Exception
    {
        // PUT
        P2Lineup lineup = new P2Lineup();
        lineup.setId( "id1" );
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
            response = RequestFacade.sendMessage( URI_PART + "/groupid/one/id1/1.2.3", Method.PUT, representation );
            String responseText = response.getEntity().getText();
            assertResponse( response, responseText );

            P2LineupSummaryDto resultLineupSummary = getObjectResponseText( responseText, new P2LineupSummaryDto() );
            Assert.assertEquals( "id1", resultLineupSummary.getId() );
            Assert.assertEquals( "groupid.one", resultLineupSummary.getGroupId() );
            Assert.assertEquals( "1.2.3", resultLineupSummary.getVersion() );
            Assert.assertEquals( "name1", resultLineupSummary.getName() );
            Assert.assertEquals( "description one", resultLineupSummary.getDescription() );
            Assert.assertEquals(
                this.getNexusTestRepoUrl() + "groupid/one/id1/1.2.3/",
                resultLineupSummary.getRepositoryUrl() );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }

        // GET
        try
        {
            response = RequestFacade.sendMessage( URI_PART + "/groupid/one/id1/1.2.3", Method.GET, representation );
            String responseText = response.getEntity().getText();
            assertResponse( response, responseText );
            P2Lineup resultLineup = getObjectResponseText( responseText, new P2Lineup() );
            Assert.assertEquals( "id1", resultLineup.getId() );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }

        // verify the p2Lineup.xml works from the repository too
        this.verifyLineupXpp3( lineup );

        // GET LIST
        try
        {
            response = RequestFacade.sendMessage( URI_PART, Method.GET, representation );
            String responseText = response.getEntity().getText();
            assertResponse( response, responseText );
            P2LineupListResponse listResponse = getObjectResponseText( responseText, new P2LineupListResponse() );
            responseText = response.getEntity().getText();
            // expecting 4, the 1 we added and 3 defaults
            Assert.assertEquals( 4, listResponse.getData().size() );

            P2LineupSummaryDto testLineup = null;

            for ( P2LineupSummaryDto dto : listResponse.getData() )
            {
                if ( dto.getId().equals( "id1" ) )
                {
                    testLineup = dto;
                    break;
                }
            }

            Assert.assertNotNull( "Didn't find lineup with id \"id1\"", testLineup );

            String expectedURIEnding = URI_PART + "/groupid/one/id1/1.2.3";
            Assert.assertTrue(
                "Expected uri to end with: " + expectedURIEnding + " actual uri: " + testLineup.getResourceUri(),
                testLineup.getResourceUri().endsWith( expectedURIEnding ) );
            Assert.assertEquals( "id1", testLineup.getId() );
            Assert.assertEquals( "groupid.one", testLineup.getGroupId() );
            Assert.assertEquals( "1.2.3", testLineup.getVersion() );
            Assert.assertEquals( "name1", testLineup.getName() );
            Assert.assertEquals( "description one", testLineup.getDescription() );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }

        // PUT
        // change something
        try
        {
            lineup.setName( "name1-updated" );
            representation.setPayload( lineup );
            response = RequestFacade.sendMessage( URI_PART + "/groupid/one/id1/1.2.3", Method.PUT, representation );
            String responseText = response.getEntity().getText();

            /**
             * NOTE: We have currently disabled the ability to update lineups, you can only add/delete them When this is
             * fixed, feel free to remove the new code, and put old code back
             */

            /**
             * OLD CODE
             */
            // assertResponse( response, responseText );
            // resultLineupSummary = getObjectResponseText( responseText, new P2LineupSummaryDto() );
            // Assert.assertEquals( "id1", resultLineupSummary.getId() );
            // Assert.assertEquals( "name1-updated", resultLineupSummary.getName() );
            /**
             * END OLD CODE
             */

            /**
             * NEW CODE
             */
            Assert.assertEquals( response.getStatus().getCode(), 400 );
            Assert.assertTrue( responseText
                .contains( "cannot be modified. Use different coordinates to publish a new lineup." ) );
            /**
             * END NEW CODE
             */
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }

        // DELETE
        try
        {
            response = RequestFacade.sendMessage( URI_PART + "/groupid/one/id1/1.2.3", Method.DELETE );
            Assert.assertEquals( 204, response.getStatus().getCode() );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }
    }

    @Test
    public void dryRunTest()
        throws Exception
    {
        // PUT
        P2Lineup lineup = new P2Lineup();
        lineup.setId( "dryRunTest" );
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
                URI_PART + "/groupid/one/dryRunTest/1.2.3?dry-run=true",
                Method.PUT,
                representation );

            // Here we get a warning and it is expected.
            InputStream responseStream = response.getEntity().getStream();
            P2LineupErrorResponse errorResponse = null;
            try
            {
                errorResponse = new P2LineupXstreamIO().readErrorResponse( responseStream );
            }
            finally
            {
                IOUtil.close( responseStream );
            }
            List<P2LineupError> errors = errorResponse.getErrors();
            Assert.assertTrue( errors.get( 0 ).isWarning() );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }

        // else
        // P2LineupSummaryDto dto = this.getObjectResponseText( responseText, new P2LineupSummaryDto() );
        // Assert.assertEquals( lineup.getId(), dto.getId() );
        // Assert.assertEquals( lineup.getGroupId(), dto.getGroupId() );
        // Assert.assertEquals( lineup.getVersion(), dto.getVersion() );
        // Assert.assertEquals( lineup.getDescription(), dto.getDescription() );
        // Assert.assertEquals( lineup.getName(), dto.getName() );
        // Assert.assertEquals( this.getBaseNexusUrl() + URI_PART + "/groupid/one/dryRunTest/1.2.3",
        // dto.getResourceUri() );
        // Assert.assertEquals(this.getNexusTestRepoUrl() + "groupid/one/dryRunTest/1.2.3", dto.getRepositoryUrl() );

        // make sure it was not created
        try
        {
            response = RequestFacade.doGetRequest( URI_PART + "/groupid/one/dryRunTest/1.2.3" );
            Assert.assertEquals( 404, response.getStatus().getCode() );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }
    }

    private void verifyLineupXpp3( P2Lineup lineup )
        throws Exception
    {
        URL lineupUrl = new URL( this.getRepositoryUrl( this.getTestRepositoryId() )
            + lineup.getGroupId().replaceAll( "\\.", "/" ) + "/" + lineup.getId() + "/" + lineup.getVersion()
            + P2LineupConstants.LINEUP_DESCRIPTOR_XML );
        File xpp3LineupFile = this
            .downloadFile( lineupUrl, "target/nxcm1955" + P2LineupConstants.LINEUP_DESCRIPTOR_XML );

        // done in two steps, because we already have methods in place to deal with auth, etc
        FileReader fileReader = null;
        try
        {
            fileReader = new FileReader( xpp3LineupFile );
            P2LineupXpp3Reader reader = new P2LineupXpp3Reader();
            P2Lineup repoLineupResult = reader.read( fileReader );
            Assert.assertEquals( lineup, repoLineupResult );
        }
        finally
        {
            IOUtil.close( fileReader );
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
        return XStreamUtil.initializeXStream( new XStream( new JsonHierarchicalStreamDriver() ) );
    }

    @Override
    public XStream getXMLXStream()
    {
        return XStreamUtil.initializeXStream( new XStream() );
    }
}
