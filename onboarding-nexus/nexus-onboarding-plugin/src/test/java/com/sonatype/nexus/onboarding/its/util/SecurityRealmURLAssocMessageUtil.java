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
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.sonatype.s2.securityrealm.model.S2SecurityRealmURLAssoc;

public class SecurityRealmURLAssocMessageUtil
{
    public static final String URI = "service/local/mse/urls";

    public static Response createURL( S2SecurityRealmURLAssoc urlAssoc )
        throws IOException
    {
        XStreamRepresentation representation =
            new XStreamRepresentation( XStreamFactory.getXmlXStream(), "", MediaType.APPLICATION_XML );
        representation.setPayload( urlAssoc );
        return RequestFacade.sendMessage( URI, Method.POST, representation );
    }

    public static Response updateURL( S2SecurityRealmURLAssoc urlAssoc )
        throws IOException
    {
        return updateURL( urlAssoc.getId(), urlAssoc );
    }

    public static Response updateURL( String urlId, S2SecurityRealmURLAssoc urlAssoc )
        throws IOException
    {
        XStreamRepresentation representation =
            new XStreamRepresentation( XStreamFactory.getXmlXStream(), "", MediaType.APPLICATION_XML );
        representation.setPayload( urlAssoc );
        return RequestFacade.sendMessage( URI + "/" + urlId, Method.PUT, representation );
    }

    public static Response deleteURL( String urlId )
        throws IOException
    {
        return RequestFacade.sendMessage( URI + "/" + urlId, Method.DELETE );
    }

    public static List<S2SecurityRealmURLAssoc> listUrls( boolean validate )
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
        return (List<S2SecurityRealmURLAssoc>) representation.getPayload( new ArrayList<S2SecurityRealmURLAssoc>() );
    }

    public static S2SecurityRealmURLAssoc getURL( Response r )
        throws IOException
    {
        XStreamRepresentation representation =
            new XStreamRepresentation( XStreamFactory.getXmlXStream(), r.getEntity().getText(),
                                       MediaType.APPLICATION_XML );
        return ( (S2SecurityRealmURLAssoc) representation.getPayload( new S2SecurityRealmURLAssoc() ) );
    }

    public static S2SecurityRealmURLAssoc getURL( String id )
        throws IOException
    {
        return getURL( id, true /* validate */);
    }

    public static S2SecurityRealmURLAssoc getURL( String id, boolean validate )
        throws IOException
    {
        Response r = RequestFacade.doGetRequest( URI + "/" + id );
        if ( r.getStatus().isError() )
        {
            if ( validate )
            {
                Assert.fail( r.getEntity().getText() + "\n" + r.getStatus() );
            }
            return null;
        }
        return getURL( r );
    }

    public static List<S2SecurityRealmURLAssoc> listUrls()
        throws IOException
    {
        return listUrls( true );
    }
}
