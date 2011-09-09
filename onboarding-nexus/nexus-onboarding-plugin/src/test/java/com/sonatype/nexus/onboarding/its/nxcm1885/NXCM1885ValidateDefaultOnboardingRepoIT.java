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
package com.sonatype.nexus.onboarding.its.nxcm1885;

import java.io.File;
import java.net.URL;

import org.restlet.data.MediaType;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sonatype.nexus.onboarding.its.AbstractOnboardingIT;
import com.sonatype.s2.project.model.IS2Project;

public class NXCM1885ValidateDefaultOnboardingRepoIT
    extends AbstractOnboardingIT
{
    @Test
    public void validateDefaultsOnStartup() 
        throws Exception
    {
        // make sure repo exists
        RepositoryMessageUtil repoUtil = new RepositoryMessageUtil( this, getXMLXStream(), MediaType.APPLICATION_XML );
        
        RepositoryBaseResource repo = repoUtil.getRepository( IS2Project.PROJECT_REPOSITORY_ID );
        
        Assert.assertNotNull( repo );
        Assert.assertEquals( repo.getName(), IS2Project.PROJECT_REPOSITORY_NAME );
        
        //now get the expected content
        File file =
            downloadFile( new URL( getNexusTestRepoUrl( IS2Project.PROJECT_REPOSITORY_ID )
                + "/org/sonatype/mse/codebases/demo-maven-git/0.0.1/" + IS2Project.PROJECT_ICON_FILENAME ),
                          "./target/downloads/nxcm1885/org/sonatype/mse/codebases/demo-maven-git/0.0.1/"
                              + IS2Project.PROJECT_ICON_FILENAME );
        Assert.assertTrue( file.exists() );
        file =
            downloadFile( new URL( getNexusTestRepoUrl( IS2Project.PROJECT_REPOSITORY_ID )
                + "/org/sonatype/mse/codebases/demo-maven-git/0.0.1/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME ),
                          "./target/downloads/nxcm1885/org/sonatype/mse/codebases/demo-maven-git/0.0.1/"
                              + IS2Project.PROJECT_DESCRIPTOR_FILENAME );
        Assert.assertTrue( file.exists() );
        file =
            downloadFile( new URL( getNexusTestRepoUrl( IS2Project.PROJECT_REPOSITORY_ID )
                + "/org/sonatype/mse/codebases/demo-maven/0.0.1/" + IS2Project.PROJECT_ICON_FILENAME ),
                          "./target/downloads/nxcm1885/org/sonatype/mse/codebases/demo-maven/0.0.1/"
                              + IS2Project.PROJECT_ICON_FILENAME );
        Assert.assertTrue( file.exists() );
        file =
            downloadFile( new URL( getNexusTestRepoUrl( IS2Project.PROJECT_REPOSITORY_ID )
                + "/org/sonatype/mse/codebases/demo-maven/0.0.1/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME ),
                          "./target/downloads/nxcm1885/org/sonatype/mse/codebases/demo-maven/0.0.1/"
                              + IS2Project.PROJECT_DESCRIPTOR_FILENAME );
        Assert.assertTrue( file.exists() );
    }
}
