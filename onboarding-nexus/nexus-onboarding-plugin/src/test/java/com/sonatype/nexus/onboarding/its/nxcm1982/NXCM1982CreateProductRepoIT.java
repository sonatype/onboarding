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
package com.sonatype.nexus.onboarding.its.nxcm1982;

import java.io.File;
import java.net.URL;

import org.codehaus.plexus.util.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.sonatype.nexus.onboarding.its.AbstractOnboardingIT;
import com.sonatype.nexus.onboarding.project.repository.OnboardingProjectRepository;
import com.sonatype.s2.project.model.IS2Project;

public class NXCM1982CreateProductRepoIT
    extends AbstractOnboardingIT
{

    @Test
    public void testCreateRepository()
        throws Exception
    {
        String localStorageDir = nexusWorkDir + "/storage/mse-installer-repo";
        FileUtils.copyFile( getTestFile( "artifacts.xml" ), new File( localStorageDir, "artifacts.xml" ) );
        FileUtils.copyFile( getTestFile( "com.sonatype.s2.installer.jnlp_1.0.4.201007010531.jnlp" ),
                            new File( localStorageDir,
                                      "mse_installer/com.sonatype.s2.installer.jnlp_1.0.4.201007010531.jnlp" ) );

        String descriptorFilename = IS2Project.PROJECT_DESCRIPTOR_FILENAME;

        // deploy project descriptor
        File testProjectDescriptor = this.getTestFile( "project.xml" );
        String deployURI = getNexusTestRepoUrl( IS2Project.PROJECT_REPOSITORY_ID ) + "/group/id/1.2.3";

        getDeployUtils().deployWithWagon( "http", deployURI, testProjectDescriptor, descriptorFilename );

        // download jnlp
        File resultProjectXml = this.downloadFile( new URL( deployURI + "/" + OnboardingProjectRepository.INSTALL_JNLP_FILENAME ), "target/NXCM1982CreateProductRepoIT/" + OnboardingProjectRepository.INSTALL_JNLP_FILENAME );
        Assert.assertTrue( FileUtils.fileRead( resultProjectXml ).contains( "Generated by nexus-onboarding-plugin" ) );
    }

    @AfterClass
    public static void removeLicense()
        throws Exception
    {
        // removed before the test runs
        // Preferences.userRoot().node( DefaultNexusLicenseBuilder.PACKAGE ).removeNode();
    }
}