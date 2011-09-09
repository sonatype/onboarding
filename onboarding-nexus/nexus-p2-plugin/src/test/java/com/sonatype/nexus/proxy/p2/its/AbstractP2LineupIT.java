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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.sonatype.nexus.p2.lineup.persist.P2Gav;
import com.sonatype.nexus.p2.rest.model.P2LineupErrorResponse;
import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Reader;
import com.sonatype.s2.p2lineup.model.io.xstream.P2LineupXstreamIO;
import com.sonatype.s2.p2lineup.model.io.xstream.XStreamUtil;
import com.thoughtworks.xstream.XStream;

public abstract class AbstractP2LineupIT
    extends AbstractNexusProxyP2IntegrationIT
{
    protected static final String URI_PART = RequestFacade.SERVICE_LOCAL + "p2/lineups";

    public AbstractP2LineupIT( String p2LineupRepoId )
    {
        super( p2LineupRepoId );
    }

    @Override
    public String getBaseNexusUrl()
    {
        String baseNexusUrl = super.getBaseNexusUrl();
        if ( baseNexusUrl != null && !baseNexusUrl.endsWith( "/" ) )
        {
            baseNexusUrl += "/";
        }
        return baseNexusUrl;
    }

    protected IP2Lineup loadP2Lineup( String p2LineupFileName )
        throws IOException,
            XmlPullParserException
    {
        File src = getTestFile( p2LineupFileName );
        Assert.assertTrue( src.exists() );

        FileReader fr = new FileReader( src );
        try
        {
            return new P2LineupXpp3Reader().read( fr, true /* strict */);
        }
        finally
        {
            fr.close();
        }
    }

    protected Response uploadP2Lineup( IP2Lineup p2Lineup )
        throws IOException
    {
        XStreamRepresentation representation = new XStreamRepresentation(
            this.getXMLXStream(),
            "",
            MediaType.APPLICATION_XML );
        representation.setPayload( p2Lineup );
        P2Gav gav = new P2Gav( p2Lineup );
        return RequestFacade.sendMessage( URI_PART + "/" + gav.toPathString(), Method.PUT, representation );
    }

    protected IP2Lineup uploadP2Lineup( String p2LineupFileName )
        throws IOException,
            XmlPullParserException
    {
        IP2Lineup p2Lineup = loadP2Lineup( p2LineupFileName );

        Response response = null;
        try
        {
            response = uploadP2Lineup( p2Lineup );
            String responseText = response.getEntity().getText();
            assertResponse( response, responseText );

            return p2Lineup;
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }
    }

    protected P2LineupErrorResponse uploadInvalidP2LineupDryRun( String p2LineupFileName )
        throws IOException,
            XmlPullParserException
    {
        return uploadInvalidP2Lineup( p2LineupFileName, true /* dryRun */);
    }

    protected P2LineupErrorResponse uploadInvalidP2Lineup( String p2LineupFileName )
        throws IOException,
            XmlPullParserException
    {
        return uploadInvalidP2Lineup( p2LineupFileName, false /* dryRun */);
    }

    protected P2LineupErrorResponse uploadInvalidP2Lineup( String p2LineupFileName, boolean dryRun )
        throws IOException,
            XmlPullParserException
    {
        IP2Lineup p2Lineup = loadP2Lineup( p2LineupFileName );

        XStreamRepresentation representation = new XStreamRepresentation(
            this.getXMLXStream(),
            "",
            MediaType.APPLICATION_XML );
        representation.setPayload( p2Lineup );
        P2Gav gav = new P2Gav( p2Lineup );
        String message = URI_PART + "/" + gav.toPathString();
        if ( dryRun )
        {
            message += "?dry-run=true";
        }
        Response response = null;
        InputStream responseStream = null;
        try
        {
            response = RequestFacade.sendMessage( message, Method.PUT, representation );
            responseStream = response.getEntity().getStream();

            return new P2LineupXstreamIO().readErrorResponse( responseStream );
        }
        finally
        {
            IOUtil.close( responseStream );
            RequestFacade.releaseResponse( response );
        }
    }

    protected void dryRunValidP2Lineup( String p2LineupFileName )
        throws IOException,
            XmlPullParserException
    {
        IP2Lineup p2Lineup = loadP2Lineup( p2LineupFileName );

        XStreamRepresentation representation = new XStreamRepresentation(
            this.getXMLXStream(),
            "",
            MediaType.APPLICATION_XML );
        representation.setPayload( p2Lineup );
        P2Gav gav = new P2Gav( p2Lineup );
        Response response = null;
        InputStream responseStream = null;
        try
        {
            response = RequestFacade.sendMessage(
                URI_PART + "/" + gav.toPathString() + "?dry-run=true",
                Method.PUT,
                representation );
            if ( response.getStatus().getCode() != 204 )
            {
                responseStream = response.getEntity().getStream();

                P2LineupErrorResponse p2LineupErrorResponse = new P2LineupXstreamIO()
                    .readErrorResponse( responseStream );
                Assert.fail( p2LineupErrorResponse.toString() );
            }
        }
        finally
        {
            IOUtil.close( responseStream );
            RequestFacade.releaseResponse( response );
        }

    }

    protected String getP2RepoURL( IP2Lineup p2Lineup )
    {
        return getNexusTestRepoUrl() + new P2Gav( p2Lineup ).toPathString();
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

    @Override
    public XStream getXMLXStream()
    {
        return XStreamUtil.initializeXStream( super.getXMLXStream() );
    }
}
