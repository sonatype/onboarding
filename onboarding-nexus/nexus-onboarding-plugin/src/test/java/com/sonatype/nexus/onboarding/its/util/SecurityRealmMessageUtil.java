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

import com.sonatype.s2.securityrealm.model.S2SecurityRealm;

public class SecurityRealmMessageUtil
{
    public static final String URI = "service/local/mse/realms";

    public static Response createOrUpdateRealm( S2SecurityRealm realm )
        throws IOException
    {
        return createOrUpdateRealm( realm.getId(), realm );
    }

    public static Response createOrUpdateRealm( String realmId, S2SecurityRealm realm )
        throws IOException
    {
        XStreamRepresentation representation =
            new XStreamRepresentation( XStreamFactory.getXmlXStream(), "", MediaType.APPLICATION_XML );
        representation.setPayload( realm );
        return RequestFacade.sendMessage( URI + "/" + realmId, Method.PUT, representation );
    }

    public static Response deleteRealm( String realmId )
        throws IOException
    {
        return RequestFacade.sendMessage( URI + "/" + realmId, Method.DELETE );
    }

    public static List<S2SecurityRealm> listRealms()
        throws IOException
    {
        return listRealms( true );
    }

    public static List<S2SecurityRealm> listRealms( boolean validate )
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
        return (List<S2SecurityRealm>) representation.getPayload( new ArrayList<S2SecurityRealm>() );
    }

    public static S2SecurityRealm getRealm( Response r )
        throws IOException
    {
        XStreamRepresentation representation =
            new XStreamRepresentation( XStreamFactory.getXmlXStream(), r.getEntity().getText(),
                                       MediaType.APPLICATION_XML );
        return ( (S2SecurityRealm) representation.getPayload( new S2SecurityRealm() ) );
    }

    public static S2SecurityRealm getRealm( String id )
        throws IOException
    {
        Response r = RequestFacade.doGetRequest( URI + "/" + id );
        if ( r.getStatus().isError() )
        {
            Assert.fail( r.getEntity().getText() + "\n" + r.getStatus() );
        }
        return getRealm( r );
    }
}
