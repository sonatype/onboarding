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
package com.sonatype.nexus.proxy.p2.its.nxcm1691;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.sonatype.nexus.proxy.repository.RemoteStatus;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

import com.sonatype.nexus.p2.P2Constants;
import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2IntegrationIT;

public class NXCM1691P2ProxyTestStatusIT
    extends AbstractNexusProxyP2IntegrationIT
{
    public NXCM1691P2ProxyTestStatusIT()
    {
        super( "p2proxycontentxml" );
    }

    @Test
    public void p2proxyStatus()
        throws Exception
    {
        RepositoryMessageUtil repoUtil =
            new RepositoryMessageUtil( this, this.getXMLXStream(), MediaType.APPLICATION_XML );

        for ( String s : P2Constants.METADATA_FILE_PATHS )
        {
            s = s.replaceAll( "/", "" ).replaceAll( "\\.", "" );
            testStatus( repoUtil, "p2proxy" + s, RemoteStatus.AVAILABLE );
        }

        testStatus( repoUtil, "notp2", RemoteStatus.UNAVAILABLE );
    }

    private void testStatus( RepositoryMessageUtil repoUtil, String repoId, RemoteStatus expectedStatus )
        throws Exception
    {
        int timeout = 30000; // 30 secs
        long start = System.currentTimeMillis();
        String status = RemoteStatus.UNKNOWN.toString();
        while ( RemoteStatus.UNKNOWN.toString().equals( status ) && ( System.currentTimeMillis() - start ) < timeout )
        {
            RepositoryStatusResource statusResource = repoUtil.getStatus( repoId );
            status = statusResource.getRemoteStatus();
            Thread.sleep( 100 );
        }
        Assert.assertEquals( "Unexpected status for repository id=" + repoId, expectedStatus.toString(), status );
    }
}
