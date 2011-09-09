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
package com.sonatype.nexus.onboarding.its.meclipse1385headVersionRedeploy;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sonatype.nexus.onboarding.its.AbstractOnboardingIT;
import com.sonatype.s2.project.model.IS2Project;

public class CodebaseDescriptorRedeployIT
    extends AbstractOnboardingIT
{
    public CodebaseDescriptorRedeployIT()
    {
        super( "onboarding" );
    }

    @Test
    public void allowHeadRedeployment()
        throws Exception
    {
        getDeployUtils().deployWithWagon( "http", getNexusTestRepoUrl(), new File( "resources/descriptor.xml" ),
                                          "g/a/v-HEAD/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME );

        getDeployUtils().deployWithWagon( "http", getNexusTestRepoUrl(),
                                          new File( "resources/descriptor-with-property-reference.xml" ),
                                          "g/a/v-HEAD/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME );
    }

    @Test
    public void denyRedeployment()
        throws Exception
    {
        getDeployUtils().deployWithWagon( "http", getNexusTestRepoUrl(), new File( "resources/descriptor.xml" ),
                                          "g/a/v/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME );

        try
        {
            getDeployUtils().deployWithWagon( "http", getNexusTestRepoUrl(),
                                              new File( "resources/descriptor-with-property-reference.xml" ),
                                              "g/a/v/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME );
        
            Assert.fail();
        }
        catch ( Exception expected )
        {

        }
    }
}
