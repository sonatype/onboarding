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
package com.sonatype.nexus.onboarding.its.meclipse697;

import java.io.File;
import java.net.URL;

import org.codehaus.plexus.util.FileUtils;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.testng.annotations.Test;

import com.sonatype.nexus.onboarding.its.AbstractOnboardingIT;
import com.sonatype.s2.project.model.IS2Project;

public class MECLIPSE697UploadRandomFileIT
    extends AbstractOnboardingIT
{    
    @Test
    public void uploadRandomFile()
        throws Exception
    {        
        // deploy a random file (i.e. not an s2 project descriptor)
        String data = "random text goes here."; 
        File file = new File( "target/textfile.txt" );
        FileUtils.fileWrite( file.getAbsolutePath(), data );
        
        getDeployUtils().deployWithWagon( "http", getNexusTestRepoUrl( IS2Project.PROJECT_REPOSITORY_ID ), file, "g/a/v/randomFile.txt" );
        
        File resultFile = this.downloadFile( new URL( getNexusTestRepoUrl( IS2Project.PROJECT_REPOSITORY_ID ) + "/"+ "g/a/v/randomFile.txt"), "target/meclipse697result.txt" );

        FileTestingUtils.compareFileSHA1s( file, resultFile );

    }
}
