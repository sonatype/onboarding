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
package com.sonatype.nexus.onboarding;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.sonatype.nexus.AbstractNexusTestCase;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.local.fs.FileContentLocator;

import com.sonatype.nexus.onboarding.project.repository.OnboardingContentClass;

public abstract class AbstractNexusOnboardingTest
    extends AbstractNexusTestCase
{
    protected static final String MSE_INSTALLER_NEXUS_REPO_ID = "mse-installer-repo";

    protected RepositoryRegistry repositoryRegistry;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        copyDefaultConfigToPlace();
        
        repositoryRegistry = lookup( RepositoryRegistry.class );
    }

    /**
     * This is UT, and this plugin will be in Core during this test (when deployed, would be in own classloader managed
     * by PM). Meaning, no auto-repoType discovery will happen, so we need to "trick" the core and register plugin
     * contributed repository since nexus.xml provided by this test contains a repository that is of type provided by
     * this plugin. We have to do this before we lookup Nexus, since it triggers config load.
     * 
     * @throws Exception
     */
    protected void registerOnboardingRepository()
        throws Exception
    {
        final RepositoryTypeRegistry repositoryTypeRegistry = lookup( RepositoryTypeRegistry.class );

        repositoryTypeRegistry.registerRepositoryTypeDescriptors( new RepositoryTypeDescriptor( Repository.class,
            OnboardingContentClass.ID, "repositories", 1 ) );
    }

    protected void publishArtifactsXML()
        throws Exception
    {
        publishArtifactsXML( "artifacts.xml" );
    }

    protected void publishArtifactsXML( String artifactsXmlFileName )
        throws Exception
    {
        artifactsXmlFileName = "src/test/resources/" + artifactsXmlFileName;
        Assert.assertTrue( artifactsXmlFileName + " does not exist", new File( artifactsXmlFileName ).exists() );
        ResourceStoreRequest request = new ResourceStoreRequest( "/artifacts.xml" );

        FileContentLocator contentLocator = new FileContentLocator( getTestFile( artifactsXmlFileName ), "text/xml" );

        Repository repo = repositoryRegistry.getRepository( MSE_INSTALLER_NEXUS_REPO_ID );
        // create the file item
        DefaultStorageFileItem file = new DefaultStorageFileItem( repo, request, true, true, contentLocator );

        // store the file item
        repo.storeItem( false, file );
    }

    protected void publishJNLP()
        throws Exception
    {
        ResourceStoreRequest request =
            new ResourceStoreRequest( "/mse_installer/com.sonatype.s2.installer.jnlp_1.0.4.201007010531.jnlp" );

        FileContentLocator contentLocator =
            new FileContentLocator(
                getTestFile( "src/test/resources/com.sonatype.s2.installer.jnlp_1.0.4.201007010531.jnlp" ),
                "application/x-java-jnlp-file" );

        Repository repo = repositoryRegistry.getRepository( MSE_INSTALLER_NEXUS_REPO_ID );
        // create the file item
        DefaultStorageFileItem file = new DefaultStorageFileItem( repo, request, true, true, contentLocator );

        // store the file item
        repo.storeItem( false, file );
    }

    protected void copyDefaultConfigToPlace()
        throws IOException
    {
        copyResource( "/nexus.xml", new File( getConfHomeDir(), "nexus.xml" ).getAbsolutePath() );
    }
}
