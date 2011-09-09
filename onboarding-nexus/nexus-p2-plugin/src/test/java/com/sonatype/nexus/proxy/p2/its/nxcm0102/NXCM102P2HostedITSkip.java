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
package com.sonatype.nexus.proxy.p2.its.nxcm0102;

import java.io.File;

import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.test.utils.GavUtil;

import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2IntegrationIT;

public class NXCM102P2HostedITSkip
    extends AbstractNexusProxyP2IntegrationIT
{
    public NXCM102P2HostedITSkip()
    {
        super( "p2shadow" );
    }

    public void make()
        throws Exception
    {

        deploy( new File( getOverridableFile( "p2artifacts" ), "nexus-p2-its-bundle/pom.xml" ),
            "com.sonatype.nexus.plugin.p2", // groupId
            "com.sonatype.nexus.p2.its.bundle", // artifactId
            "1.0.0", // version
            "pom" // packaging
        );

        deploy( new File( getOverridableFile( "p2artifacts" ),
            "nexus-p2-its-bundle/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" ), "com.sonatype.nexus.plugin.p2", // groupId
            "com.sonatype.nexus.p2.its.bundle", // artifactId
            "1.0.0", // version
            "jar" // packaging
        );

        // check if we can install it from p2shadow
        String nexusTestRepoUrl = getNexusTestRepoUrl();

        File installDir = new File( "target/eclipse/nxcm0102" );

        installUsingP2( nexusTestRepoUrl, "com.sonatype.nexus.p2.its.bundle", installDir.getCanonicalPath() );

        File bundle = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" );
        Assert.assertTrue( bundle.canRead() );
    }

    public void deploy( File artifactFile, String groupId, String artifactId, String version, String packaging )
        throws Exception
    {
        Gav gav = GavUtil.newGav( groupId, artifactId, version, packaging );

        getDeployUtils().deployWithWagon( "http", this.getRepositoryUrl( "m2hosted" ), artifactFile,
            getRelitiveArtifactPath( gav ) );
    }

}
