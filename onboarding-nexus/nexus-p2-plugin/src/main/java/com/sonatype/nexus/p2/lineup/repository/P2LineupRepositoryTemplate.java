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
package com.sonatype.nexus.p2.lineup.repository;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplate;

public class P2LineupRepositoryTemplate
    extends AbstractRepositoryTemplate
{
    public P2LineupRepositoryTemplate( P2LineupRepositoryTemplateProvider provider, String id, String description )
    {
        super( provider, id, description, new P2LineupContentClass(), P2LineupRepository.class );
    }

    @Override
    protected CRepositoryCoreConfiguration initCoreConfiguration()
    {
        CRepository repo = new DefaultCRepository();

        repo.setId( "" );
        repo.setName( "" );

        repo.setProviderRole( Repository.class.getName() );
        repo.setProviderHint( P2LineupContentClass.ID );
        
        Xpp3Dom ex = new Xpp3Dom( DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME );
        repo.setExternalConfiguration( ex );

        P2LineupRepositoryConfiguration exConf = new P2LineupRepositoryConfiguration( ex );

        repo.externalConfigurationImple = exConf;

        repo.setWritePolicy( RepositoryWritePolicy.ALLOW_WRITE.name() );

        CRepositoryCoreConfiguration result =
            new CRepositoryCoreConfiguration(
                                              getTemplateProvider().getApplicationConfiguration(),
                                              repo,
                                              new CRepositoryExternalConfigurationHolderFactory<P2LineupRepositoryConfiguration>()
                                              {
                                                  public P2LineupRepositoryConfiguration createExternalConfigurationHolder(
                                                                                                                      CRepository config )
                                                  {
                                                      return new P2LineupRepositoryConfiguration( (Xpp3Dom) config
                                                          .getExternalConfiguration() );
                                                  }
                                              } );

        return result;
    }
}
