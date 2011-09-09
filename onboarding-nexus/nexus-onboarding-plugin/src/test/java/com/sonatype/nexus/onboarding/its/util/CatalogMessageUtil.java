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
package com.sonatype.nexus.onboarding.its.util;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.sonatype.nexus.onboarding.rest.dto.CatalogDTO;
import com.sonatype.nexus.onboarding.rest.dto.CatalogEntryDTO;
import com.sonatype.nexus.onboarding.rest.dto.CatalogEntryRequest;
import com.sonatype.nexus.onboarding.rest.dto.CatalogRequest;
import com.sonatype.nexus.onboarding.rest.dto.CatalogResponse;

public class CatalogMessageUtil
{

    public static final String URI = "service/local/mse/catalogs";

    public static Response create( CatalogDTO catalog )
        throws IOException
    {
        XStreamRepresentation representation =
            new XStreamRepresentation( XStreamFactory.getXmlXStream(), "", MediaType.APPLICATION_XML );
        CatalogRequest data = new CatalogRequest();
        data.setData( catalog );
        representation.setPayload( data );
        return RequestFacade.sendMessage( URI + "/" + catalog.getId(), Method.PUT, representation );
    }

    public static CatalogDTO getCatalog( Response r )
        throws IOException
    {
        XStreamRepresentation representation =
            new XStreamRepresentation( XStreamFactory.getXmlXStream(), r.getEntity().getText(),
                                       MediaType.APPLICATION_XML );
        return ( (CatalogDTO) representation.getPayload( new CatalogDTO() ) );
    }

    public static List<CatalogDTO> list()
        throws IOException
    {
        return list( true );
    }

    public static List<CatalogDTO> list( boolean validate )
        throws IOException
    {
        Response r = RequestFacade.doGetRequest( URI );
        String t = r.getEntity().getText();

        if ( r.getStatus().isError() )
        {
            if ( validate )
            {
                Assert.fail( t + "\n" + r.getStatus() );
            }
            else
            {
                return null;
            }
        }

        XStreamRepresentation representation =
            new XStreamRepresentation( XStreamFactory.getXmlXStream(), t, MediaType.APPLICATION_XML );
        return ( (CatalogResponse) representation.getPayload( new CatalogResponse() ) ).getData();
    }

    public static CatalogDTO getCatalog( String id )
        throws IOException
    {
        Response r = RequestFacade.doGetRequest( URI + "/" + id );
        if ( r.getStatus().isError() )
        {
            Assert.fail( r.getEntity().getText() + "\n" + r.getStatus() );
        }
        return getCatalog( r );
    }

    public static Response updateCatalog( CatalogDTO catalog )
        throws IOException
    {
        XStreamRepresentation representation =
            new XStreamRepresentation( XStreamFactory.getXmlXStream(), "", MediaType.APPLICATION_XML );
        CatalogRequest data = new CatalogRequest();
        data.setData( catalog );
        representation.setPayload( data );
        return RequestFacade.sendMessage( URI + "/" + catalog.getId(), Method.PUT, representation );
    }

    public static Response delete( String catalogId )
        throws IOException
    {
        return RequestFacade.sendMessage( URI + "/" + catalogId, Method.DELETE );
    }

    public static Response addEntryCatalog( String catalogId, CatalogEntryDTO entry )
        throws IOException
    {
        XStreamRepresentation representation =
            new XStreamRepresentation( XStreamFactory.getXmlXStream(), "", MediaType.APPLICATION_XML );
        CatalogEntryRequest data = new CatalogEntryRequest();
        data.setData( entry );
        representation.setPayload( data );
        return RequestFacade.sendMessage( URI + "/" + catalogId + "/entries", Method.POST, representation );
    }

    public static CatalogEntryDTO getEntry( Response r )
        throws IOException
    {
        XStreamRepresentation representation =
            new XStreamRepresentation( XStreamFactory.getXmlXStream(), r.getEntity().getText(),
                                       MediaType.APPLICATION_XML );
        return ( (CatalogEntryDTO) representation.getPayload( new CatalogEntryDTO() ) );
    }

    public static Response updateCatalogEntry( String catalogId, CatalogEntryDTO entry )
        throws IOException
    {
        XStreamRepresentation representation =
            new XStreamRepresentation( XStreamFactory.getXmlXStream(), "", MediaType.APPLICATION_XML );
        CatalogEntryRequest data = new CatalogEntryRequest();
        data.setData( entry );
        representation.setPayload( data );
        return RequestFacade.sendMessage( URI + "/" + catalogId + "/entries/" + entry.getId(), Method.PUT,
                                          representation );
    }

    public static CatalogEntryDTO getEntry( String catalogId, String entryId )
        throws IOException
    {
        return getEntry( catalogId, entryId, true );
    }

    public static CatalogEntryDTO getEntry( String catalogId, String entryId, boolean validate )
        throws IOException
    {
        Response r = RequestFacade.doGetRequest( URI + "/" + catalogId + "/entries/" + entryId );

        if ( r.getStatus().isError() )
        {
            if ( validate )
            {
                Assert.fail( r.getEntity().getText() + "\n" + r.getStatus() );
            }
            else
            {
                return null;
            }
        }

        return getEntry( r );
    }

    public static Response deleteEntry( String catalogId, String entryId )
        throws IOException
    {
        return RequestFacade.sendMessage( URI + "/" + catalogId + "/entries/" + entryId, Method.DELETE );
    }

}
