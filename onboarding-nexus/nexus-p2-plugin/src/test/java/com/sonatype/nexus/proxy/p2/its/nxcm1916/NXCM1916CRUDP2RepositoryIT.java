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
package com.sonatype.nexus.proxy.p2.its.nxcm1916;

import java.io.IOException;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2IntegrationIT;

public class NXCM1916CRUDP2RepositoryIT
    extends AbstractNexusProxyP2IntegrationIT
{

    private RepositoryMessageUtil messageUtil;

    public NXCM1916CRUDP2RepositoryIT()
        throws ComponentLookupException
    {
        this.messageUtil = new RepositoryMessageUtil( this, this.getJsonXStream(), MediaType.APPLICATION_JSON );
    }

    @Test
    public void createRepositoryTest()
        throws IOException
    {

        RepositoryResource resource = new RepositoryResource();

        resource.setId( "createTestRepo" );
        resource.setRepoType( "hosted" );
        resource.setName( "Create Test Repo" );
        resource.setProvider( "p2" );
        resource.setFormat( "p2" );
        resource.setRepoPolicy( RepositoryPolicy.MIXED.name() );

        this.messageUtil.createRepository( resource );
    }

    @Test
    public void readTest()
        throws IOException
    {

        RepositoryResource resource = new RepositoryResource();

        resource.setId( "readTestRepo" );
        resource.setRepoType( "hosted" );
        resource.setName( "Read Test Repo" );
        resource.setProvider( "p2" );
        resource.setFormat( "p2" );
        resource.setRepoPolicy( RepositoryPolicy.MIXED.name() );

        this.messageUtil.createRepository( resource );

        RepositoryResource responseRepo = (RepositoryResource) this.messageUtil.getRepository( resource.getId() );

        this.messageUtil.validateResourceResponse( resource, responseRepo );

    }

    @Test
    public void updateTest()
        throws IOException
    {

        RepositoryResource resource = new RepositoryResource();

        resource.setId( "updateTestRepo" );
        resource.setRepoType( "hosted" );
        resource.setName( "Update Test Repo" );
        resource.setProvider( "p2" );
        resource.setFormat( "p2" );
        resource.setRepoPolicy( RepositoryPolicy.MIXED.name() );

        resource = (RepositoryResource) this.messageUtil.createRepository( resource );

        resource.setName( "updated repo" );

        this.messageUtil.updateRepo( resource );

    }

    @Test
    public void deleteTest()
        throws IOException
    {
        RepositoryResource resource = new RepositoryResource();

        resource.setId( "deleteTestRepo" );
        resource.setRepoType( "hosted" );
        resource.setName( "Delete Test Repo" );
        resource.setProvider( "p2" );
        resource.setFormat( "p2" );
        resource.setRepoPolicy( RepositoryPolicy.MIXED.name() );

        resource = (RepositoryResource) this.messageUtil.createRepository( resource );

        Response response = null;
        try
        {
            response = this.messageUtil.sendMessage( Method.DELETE, resource );

            if ( !response.getStatus().isSuccess() )
            {
                Assert.fail( "Could not delete Repository: " + response.getStatus() );
            }
            Assert.assertNull( getNexusConfigUtil().getRepo( resource.getId() ) );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }
    }

}
