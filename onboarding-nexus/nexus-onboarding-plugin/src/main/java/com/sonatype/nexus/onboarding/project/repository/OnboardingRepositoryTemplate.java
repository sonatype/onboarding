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

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplate;

public class OnboardingRepositoryTemplate
    extends AbstractRepositoryTemplate
{

    public OnboardingRepositoryTemplate( OnboardingRepositoryTemplateProvider provider, String id,
                                               String description )
    {
        super( provider, id, description, new OnboardingContentClass(), OnboardingProjectRepository.class );
    }

    @Override
    protected CRepositoryCoreConfiguration initCoreConfiguration()
    {
        CRepository repo = new DefaultCRepository();

        repo.setId( "" );
        repo.setName( "" );

        repo.setProviderRole( Repository.class.getName() );
        repo.setProviderHint( OnboardingContentClass.ID );

        Xpp3Dom ex = new Xpp3Dom( DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME );
        repo.setExternalConfiguration( ex );

        OnboardingRepositoryConfiguration exConf = new OnboardingRepositoryConfiguration( ex );

        repo.externalConfigurationImple = exConf;

        repo.setWritePolicy( RepositoryWritePolicy.ALLOW_WRITE_ONCE.name() );

        CRepositoryCoreConfiguration result =
            new CRepositoryCoreConfiguration( getTemplateProvider().getApplicationConfiguration(), 
                                              repo, 
                                              new CRepositoryExternalConfigurationHolderFactory<OnboardingRepositoryConfiguration>()
                                              {
                                                  public OnboardingRepositoryConfiguration createExternalConfigurationHolder(
                                                                                                                      CRepository config )
                                                  {
                                                      return new OnboardingRepositoryConfiguration( (Xpp3Dom) config
                                                          .getExternalConfiguration() );
                                                  }
                                              } );

        return result;
    }

}
