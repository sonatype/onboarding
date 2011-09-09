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
package com.sonatype.nexus.onboarding.its.meclipse700;

import java.io.File;

import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.util.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sonatype.nexus.onboarding.its.AbstractOnboardingIT;
import com.sonatype.nexus.onboarding.project.repository.OnboardingProjectRepository;
import com.sonatype.s2.project.model.IS2Project;

public class MECLIPSE700DenyUploadOfReservedFilesIT
    extends AbstractOnboardingIT
{    
    @Test
    public void denyUpload()
        throws Exception
    {        
        String data = "fake jnlp file"; 
        File file = new File( "target/" + IS2Project.PROJECT_REPOSITORY_ID + "/fake.jnlp" );
        file.getParentFile().mkdirs();
        FileUtils.fileWrite( file.getAbsolutePath(), data );
        
        // deploy catalog.xml
        try
        {
            getDeployUtils().deployWithWagon( "http", getNexusTestRepoUrl( IS2Project.PROJECT_REPOSITORY_ID ), file,  "something/foo/" + OnboardingProjectRepository.INSTALL_JNLP_FILENAME );
            Assert.fail( "Expected TransferFailedException" );
        }
        catch ( TransferFailedException e )
        {
            if ( !e.getMessage().endsWith(
                                           "/nexus/content/repositories/" + IS2Project.PROJECT_REPOSITORY_ID + "/something/foo/" + OnboardingProjectRepository.INSTALL_JNLP_FILENAME + ". Return code is: 400" ) )
            {
                throw e;
            }
        }
    }
}
