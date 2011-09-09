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
package com.sonatype.nexus.proxy.p2.its.nxcm2093;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2IntegrationIT;
import com.sonatype.nexus.proxy.p2.its.P2ITException;

public class NXCM2093CheckSumValidationIT
    extends AbstractNexusProxyP2IntegrationIT
{
    public NXCM2093CheckSumValidationIT()
    {
        super( "p2brokenchecksum" );
    }

    @Test
    public void p2repository()
        throws Exception
    {
        File installDir = new File( "target/eclipse/nxcm2093" );

        // the must work one
        installUsingP2( getNexusTestRepoUrl( "p2okchecksum" ), "org.mortbay.jetty.util",
                        installDir.getCanonicalPath(),
                        null, null, null );

        try
        {
            installUsingP2( getNexusTestRepoUrl(), "com.sonatype.nexus.p2.its.feature.feature.group",
                            installDir.getCanonicalPath(), null, null, null, "-Declipse.p2.MD5Check=false" );
            Assert.fail();
        }
        catch ( P2ITException e )
        {
            assertThat(
                               FileUtils.fileRead( nexusLog ),
                               containsString( "Validation failed due: The artifact /features/com.sonatype.nexus.p2.its.feature_1.0.0.jar and it's remote checksums does not match in repository p2brokenchecksum! The checksumPolicy of repository forbids downloading of it." ) );
        }

        RepositoryMessageUtil repoUtil = new RepositoryMessageUtil( this, getXMLXStream(), MediaType.APPLICATION_XML );
        RepositoryStatusResource repoStatusResource = repoUtil.getStatus( this.getTestRepositoryId() );

        Assert.assertEquals( ProxyMode.ALLOW.name(), repoStatusResource.getProxyMode() );
//        Assert.assertEquals( RemoteStatus.AVAILABLE.name(), repoStatusResource.getRemoteStatus() );
        Assert.assertEquals( LocalStatus.IN_SERVICE.name(), repoStatusResource.getLocalStatus() );

    }

}
