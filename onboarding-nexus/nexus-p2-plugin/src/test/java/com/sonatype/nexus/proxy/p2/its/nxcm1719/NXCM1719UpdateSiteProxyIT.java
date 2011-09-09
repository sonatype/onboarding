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
package com.sonatype.nexus.proxy.p2.its.nxcm1719;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.test.utils.TestProperties;

import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2IntegrationIT;

public class NXCM1719UpdateSiteProxyIT
    extends AbstractNexusProxyP2IntegrationIT
{

    public NXCM1719UpdateSiteProxyIT()
    {
        super( "updatesiteproxy" );
    }

    @Test
    public void updatesiteproxy()
        throws Exception
    {
        String nexusTestRepoUrl = getNexusTestRepoUrl();

        File installDir = new File( "target/eclipse/nxcm1719" );

        String correctURL = TestProperties.getString( "proxy-repo-base-url" ) + "updatesite/";
        TaskScheduleUtil.waitForAllTasksToStop();

        Response response = null;
        try
        {
            response = RequestFacade.doGetRequest( "content/repositories/" + this.getTestRepositoryId() + "/features/" );
            String responseText = response.getEntity().getText();
            Assert.assertFalse( "response: " + response.getStatus() + "\n" + responseText, response
                .getStatus().isSuccess() );
        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }

        RepositoryMessageUtil repoUtil = new RepositoryMessageUtil(
            this,
            this.getXMLXStream(),
            MediaType.APPLICATION_XML );
        RepositoryProxyResource repo = (RepositoryProxyResource) repoUtil.getRepository( "updatesiteproxy" );

        repo.getRemoteStorage().setRemoteStorageUrl( correctURL );
        repoUtil.updateRepo( repo );

        // wait for the tasks
        TaskScheduleUtil.waitForAllTasksToStop();

        try
        {
            response = RequestFacade.doGetRequest( "content/repositories/" + this.getTestRepositoryId() + "/features/" );
            Assert.assertTrue( "expected success: " + response.getStatus(), response.getStatus().isSuccess() );

            installUsingP2(
                nexusTestRepoUrl,
                "com.sonatype.nexus.p2.its.feature.feature.group",
                installDir.getCanonicalPath() );

            File feature = new File( installDir, "features/com.sonatype.nexus.p2.its.feature_1.0.0" );
            Assert.assertTrue( feature.exists() && feature.isDirectory() );

            File bundle = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" );
            Assert.assertTrue( bundle.canRead() );

        }
        finally
        {
            RequestFacade.releaseResponse( response );
        }
    }
}
