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
package com.sonatype.nexus.onboarding.events;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.events.AbstractEventInspector;
import org.sonatype.nexus.proxy.events.EventInspector;
import org.sonatype.nexus.proxy.events.NexusStartedEvent;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.templates.NoSuchTemplateIdException;
import org.sonatype.nexus.templates.TemplateManager;
import org.sonatype.nexus.templates.repository.RepositoryTemplate;
import org.sonatype.plexus.appevents.Event;

import com.sonatype.nexus.onboarding.project.repository.OnboardingContentClass;
import com.sonatype.nexus.onboarding.project.repository.OnboardingProjectRepository;
import com.sonatype.s2.project.model.IS2Project;

@Component( role = EventInspector.class, hint = "TeamStartedEventInspector" )
public class OnboardingNexusStartedEventInspector
    extends AbstractEventInspector
{    
    @Requirement
    private NexusConfiguration configuration;
    
    @Requirement
    private TemplateManager templateManager;
    
    @Requirement
    private RepositoryRegistry repoRegistry;
    
    private File onboardingRepoDir = null;
    
    public boolean accepts( Event<?> evt )
    {
        return evt instanceof NexusStartedEvent;
    }

    public void inspect( Event<?> evt )
    {
        try
        {
            repoRegistry.getRepository( IS2Project.PROJECT_REPOSITORY_ID );
            getLogger().debug( "Default Onboarding repository is present, no changes required." );
        }
        catch ( NoSuchRepositoryException e1 )
        {
            //no repo, go ahead and setup default
            try
            {
                createRepository();
                copyFile( "org/sonatype/mse/codebases/demo-maven-git-e36/0.0.1/" + IS2Project.PROJECT_ICON_FILENAME );
                copyFile( "org/sonatype/mse/codebases/demo-maven-git-e36/0.0.1/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME );
                copyFile( "org/sonatype/mse/codebases/demo-maven-git/0.0.1/" + IS2Project.PROJECT_ICON_FILENAME );
                copyFile( "org/sonatype/mse/codebases/demo-maven-git/0.0.1/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME );
                copyFile( "org/sonatype/mse/codebases/demo-maven/0.0.1/" + IS2Project.PROJECT_ICON_FILENAME );
                copyFile( "org/sonatype/mse/codebases/demo-maven/0.0.1/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME );
            }
            catch ( IOException e )
            {
                getLogger().error( "Unable to setup default onboarding repository properly", e );
            }
            catch ( NoSuchTemplateIdException e )
            {
                getLogger().error( "Unable to setup default onboarding repository properly", e );
            }
            catch ( ConfigurationException e )
            {
                getLogger().error( "Unable to setup default onboarding repository properly", e );
            }
        }
    }
    
    private void createRepository() 
        throws NoSuchTemplateIdException, 
            ConfigurationException, 
            IOException
    {
        List<OnboardingProjectRepository> existingRepos = repoRegistry.getRepositoriesWithFacet( OnboardingProjectRepository.class );
        
        if ( existingRepos != null 
            && existingRepos.size() > 0 )
        {
            getLogger().info( "Onboard repository already exists, default onboarding repository will not be created!" );
        }
        else
        {
            getLogger().info( "Default Onboarding repository is missing, creating with default content." );
            RepositoryTemplate template = ( RepositoryTemplate ) templateManager.getTemplate( RepositoryTemplate.class, OnboardingContentClass.ID );
            
            template.getConfigurableRepository().setId( IS2Project.PROJECT_REPOSITORY_ID );
            template.getConfigurableRepository().setName( IS2Project.PROJECT_REPOSITORY_NAME );
            template.getConfigurableRepository().setSearchable( false );
            template.getConfigurableRepository().setNotFoundCacheActive( false );
            template.getConfigurableRepository().setUserManaged( false );
            
            template.create();
        }
    }
    
    private void copyFile( String filename )
        throws IOException
    {
        InputStream is = null;
        OutputStream os = null;

        try
        {
            is = getClass().getResourceAsStream( "/META-INF/codebases/" + filename );
            
            File repoFile = new File( getOnboardingRepoDirectory(), filename );
            repoFile.getParentFile().mkdirs();
            
            os = new FileOutputStream( repoFile );
            IOUtil.copy( is, os );
        }
        finally
        {
            IOUtil.close( is );
            IOUtil.close( os );
        }
    }
    
    private File getOnboardingRepoDirectory()
    {
        if ( onboardingRepoDir == null )
        {
            onboardingRepoDir = new File( configuration.getWorkingDirectory( "storage" ), IS2Project.PROJECT_REPOSITORY_ID );
            onboardingRepoDir.mkdirs();
        }
        
        return onboardingRepoDir;
    }
}
