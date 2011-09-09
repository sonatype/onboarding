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
package com.sonatype.nexus.onboarding.project.repository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.Nexus;
import org.sonatype.nexus.configuration.ExternalConfiguration;
import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.ConfigurableRepository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.rest.ContentPlexusResource;
import org.sonatype.nexus.templates.TemplateProvider;
import org.sonatype.nexus.templates.repository.RepositoryTemplate;
import org.sonatype.security.SecuritySystem;

import com.sonatype.nexus.onboarding.AbstractNexusOnboardingTest;
import com.sonatype.s2.project.model.IP2LineupLocation;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.descriptor.P2LineupLocation;
import com.sonatype.s2.project.model.descriptor.Project;
import com.sonatype.s2.project.model.descriptor.io.xpp3.S2ProjectDescriptorXpp3Writer;

public class OnboardingProjectRepositoryTest
    extends AbstractNexusOnboardingTest
{
    private OnboardingProjectRepository onboardingProjectRepository;

    private File nexusWorkDir;

    private File applicationConfDir;

    private String testGavPath = "/group/artifact/version/";

    private String testGavPathNoLineup = "/group/artifact/version1/";

    protected boolean loadConfigurationAtSetUp()
    {
        return false;
    }

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        // need to setup the container first
        this.getContainer();

        // remove the old work dir
        FileUtils.deleteDirectory( this.nexusWorkDir );
        applicationConfDir.mkdirs();
        copyResource( "/nexus.xml", new File( applicationConfDir, "nexus.xml" ).getAbsolutePath() );

        TemplateProvider templateProvider = this.lookup( TemplateProvider.class, OnboardingContentClass.ID );
        RepositoryTemplate repoTemplate = (RepositoryTemplate) templateProvider.getTemplateById( "Onboarding" );

        // Awake it
        lookup(Nexus.class);
        NexusConfiguration nexusConfiguration = lookup( NexusConfiguration.class );

        RepositoryRegistry repoRegistry = this.lookup( RepositoryRegistry.class );
        if ( !repoRegistry.repositoryIdExists( IS2Project.PROJECT_REPOSITORY_ID ) )
        {
            ConfigurableRepository configurableRepository = repoTemplate.getConfigurableRepository();
            configurableRepository.setId( IS2Project.PROJECT_REPOSITORY_ID );
            configurableRepository.setName( "Onboarding Test Repo" );

            onboardingProjectRepository = (OnboardingProjectRepository) repoTemplate.create();
            onboardingProjectRepository.getExternalConfiguration( true /* forModification */).setMSEInstallersRepositoryId(
                MSE_INSTALLER_NEXUS_REPO_ID );

            // not needed, template create already does this!!!
            // repoRegistry.addRepository( onboardingProjectRepository );
        }
        else
        {
            onboardingProjectRepository =
                (OnboardingProjectRepository) repoRegistry.getRepository( IS2Project.PROJECT_REPOSITORY_ID );
            onboardingProjectRepository.getExternalConfiguration( true /* forModification */).setMSEInstallersRepositoryId(
                MSE_INSTALLER_NEXUS_REPO_ID );
        }

        // lets disable security for now
        // TODO: turn it back on
        this.lookup( SecuritySystem.class ).setSecurityEnabled( false );
        
        // set baseUrl
        final GlobalRestApiSettings globalRestApiSettings = lookup( GlobalRestApiSettings.class );
        globalRestApiSettings.setBaseUrl( "http://localhost:8081/nexus" );

        // save all the changes above
        nexusConfiguration.saveConfiguration();
    }

    @Override
    protected void customizeContext( Context context )
    {
        super.customizeContext( context );

        this.nexusWorkDir = new File( "target/plexus-home/OnboardingProjectRepositoryTest/" );
        this.applicationConfDir = new File( nexusWorkDir, "conf/" );

        context.put( "nexus-work", nexusWorkDir.getAbsolutePath() );
        context.put( "application-conf", applicationConfDir.getAbsolutePath() );
    }

    @Test
    public void testGetProjectJnlpWithLineup()
        throws Exception
    {
        publishJNLP();
        publishArtifactsXML();

        this.createProjectDescriptorWithLineup( this.testGavPath );

        ResourceStoreRequest request =
            new ResourceStoreRequest( this.testGavPath + OnboardingProjectRepository.INSTALL_JNLP_FILENAME );
        StorageItem storageItem = this.onboardingProjectRepository.retrieveItem( request );

        StorageFileItem fileItem = (StorageFileItem) storageItem;
        String jnlpResult = IOUtil.toString( fileItem.getInputStream() );

        this.validateInterpolatedValues( jnlpResult, "codebaseURL", "generationDate", "s2installerURL",
            "osgiInstallTempArea", "descriptorToInstall" );

        // now verify the name of the File item
        Assert.assertEquals( "codebase-group.artifact.version.jnlp",
            fileItem.getItemContext().get( ContentPlexusResource.OVERRIDE_FILENAME_KEY ) );
    }

    @Test
    public void testGetProjectJnlpWithoutLineup()
        throws Exception
    {
        createProjectDescriptorWithoutLineup( this.testGavPathNoLineup );
        ResourceStoreRequest request =
            new ResourceStoreRequest( this.testGavPathNoLineup + OnboardingProjectRepository.INSTALL_JNLP_FILENAME );
        try
        {
            this.onboardingProjectRepository.retrieveItem( request );
            fail( "Found jnlp for codebase without lineup" );
        }
        catch ( ItemNotFoundException expected )
        {
        }
    }

    @Test
    public void testGetProjectListingWithLineup()
        throws Exception
    {
        createProjectDescriptorWithLineup( this.testGavPath );

        ResourceStoreRequest request = new ResourceStoreRequest( testGavPath );
        StorageItem storageItem = this.onboardingProjectRepository.retrieveItem( request );

        StorageCollectionItem collectionItem = (StorageCollectionItem) storageItem;
        Collection<StorageItem> items = collectionItem.list();

        boolean found = false;
        List<String> entries = new ArrayList<String>();

        for ( StorageItem storageItemEntry : items )
        {
            entries.add( storageItem.getName() );
            if ( storageItemEntry.getName().equals( OnboardingProjectRepository.INSTALL_JNLP_FILENAME ) )
            {
                found = true;
                break;
            }
        }

        Assert.assertTrue( "Could not find " + OnboardingProjectRepository.INSTALL_JNLP_FILENAME
            + " in directory listing, found: " + entries, found );
    }

    @Test
    public void testGetProjectListingWithoutLineup()
        throws Exception
    {
        createProjectDescriptorWithoutLineup( this.testGavPathNoLineup );

        ResourceStoreRequest request = new ResourceStoreRequest( testGavPathNoLineup );
        StorageItem storageItem = this.onboardingProjectRepository.retrieveItem( request );

        StorageCollectionItem collectionItem = (StorageCollectionItem) storageItem;
        Collection<StorageItem> items = collectionItem.list();

        boolean found = false;
        List<String> entries = new ArrayList<String>();
        for ( StorageItem storageItemEntry : items )
        {
            entries.add( storageItem.getName() );
            if ( storageItemEntry.getName().equals( OnboardingProjectRepository.INSTALL_JNLP_FILENAME ) )
            {
                found = true;
                break;
            }
        }

        Assert.assertFalse( "Found " + OnboardingProjectRepository.INSTALL_JNLP_FILENAME
            + " in directory listing, found: " + entries, found );
    }

    @Test
    public void testGroupAbility()
    {
        Assert.assertTrue( this.onboardingProjectRepository.getRepositoryContentClass().isGroupable() );
    }

    @Test
    public void testSetSecurityRealm()
        throws ConfigurationException
    {
        ExternalConfiguration<?> externalConfig =
            this.onboardingProjectRepository.getCurrentCoreConfiguration().getExternalConfiguration();
        OnboardingRepositoryConfiguration extConfig =
            (OnboardingRepositoryConfiguration) externalConfig.getConfiguration( true );
        Assert.assertNull( extConfig.getSecurityRealmId() );
        extConfig.setSecurityRealmId( "foobar" );

        this.onboardingProjectRepository.getCurrentCoreConfiguration().commitChanges();

        extConfig = (OnboardingRepositoryConfiguration) externalConfig.getConfiguration( true );
        Assert.assertEquals( "foobar", extConfig.getSecurityRealmId() );
    }

    @Test
    public void testSetMSEInstallersRepositoryId()
        throws ConfigurationException
    {
        ExternalConfiguration<?> externalConfig =
            onboardingProjectRepository.getCurrentCoreConfiguration().getExternalConfiguration();
        OnboardingRepositoryConfiguration extConfig =
            (OnboardingRepositoryConfiguration) externalConfig.getConfiguration( true );
        Assert.assertEquals( MSE_INSTALLER_NEXUS_REPO_ID, extConfig.getMSEInstallersRepositoryId() );
        extConfig.setMSEInstallersRepositoryId( "foobar" );

        onboardingProjectRepository.getCurrentCoreConfiguration().commitChanges();

        extConfig = (OnboardingRepositoryConfiguration) externalConfig.getConfiguration( true );
        Assert.assertEquals( "foobar", extConfig.getMSEInstallersRepositoryId() );
    }

    private void createProjectDescriptorWithoutLineup( String gavPath )
        throws AccessDeniedException, UnsupportedStorageOperationException, ItemNotFoundException,
        IllegalOperationException, IOException
    {
        createProjectDescriptor( gavPath, null );
    }

    private void createProjectDescriptorWithLineup( String gavPath )
        throws AccessDeniedException, UnsupportedStorageOperationException, ItemNotFoundException,
        IllegalOperationException, IOException
    {
        createProjectDescriptor( gavPath, "foo" );
    }

    private void createProjectDescriptor( String gavPath, String lineupUrl )
        throws AccessDeniedException, UnsupportedStorageOperationException, ItemNotFoundException,
        IllegalOperationException, IOException
    {
        Project codebase = new Project();
        if ( lineupUrl != null )
        {
            IP2LineupLocation p2LineupLocation = new P2LineupLocation();
            p2LineupLocation.setUrl( lineupUrl );
            codebase.setP2LineupLocation( p2LineupLocation );
        }

        StringWriter writer = new StringWriter();
        new S2ProjectDescriptorXpp3Writer().write( writer, codebase );
        writer.flush();
        ByteArrayInputStream bias = new ByteArrayInputStream( writer.getBuffer().toString().getBytes() );

        String path = gavPath + ( gavPath.endsWith( "/" ) ? "" : "/" ) + IS2Project.PROJECT_DESCRIPTOR_FILENAME;
        this.onboardingProjectRepository.storeItem( new ResourceStoreRequest( path ), bias, null );
    }

    private void validateInterpolatedValues( String jnlpText, String... properties )
    {
        for ( String key : properties )
        {
            Assert.assertFalse( "JNLP  contains uninterpolated values:\n" + jnlpText,
                jnlpText.contains( "${" + key + "}" ) );
        }
    }
}
