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
package com.sonatype.nexus.p2.templates;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.proxy.storage.remote.commonshttpclient.CommonsHttpClientRemoteStorage;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplate;

import com.sonatype.nexus.p2.P2ContentClass;
import com.sonatype.nexus.p2.updatesite.UpdateSiteRepository;
import com.sonatype.nexus.p2.updatesite.UpdateSiteRepositoryConfiguration;

public class UpdateSiteRepositoryTemplate
    extends AbstractRepositoryTemplate
{
    public UpdateSiteRepositoryTemplate( P2RepositoryTemplateProvider provider, String id, String description )
    {
        super( provider, id, description, new P2ContentClass(), UpdateSiteRepository.class );
    }

    public UpdateSiteRepositoryConfiguration getExternalConfiguration( boolean forWrite )
    {
        return (UpdateSiteRepositoryConfiguration) getCoreConfiguration().getExternalConfiguration()
            .getConfiguration( forWrite );
    }

    @Override
    protected CRepositoryCoreConfiguration initCoreConfiguration()
    {
        CRepository repo = new DefaultCRepository();

        repo.setId( "" );
        repo.setName( "" );

        repo.setProviderRole( Repository.class.getName() );
        repo.setProviderHint( UpdateSiteRepository.ROLE_HINT );

        repo.setRemoteStorage( new CRemoteStorage() );
        repo.getRemoteStorage().setProvider( CommonsHttpClientRemoteStorage.PROVIDER_STRING );
        repo.getRemoteStorage().setUrl( "http://some-remote-repository/repo-root" );

        Xpp3Dom ex = new Xpp3Dom( DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME );
        repo.setExternalConfiguration( ex );

        UpdateSiteRepositoryConfiguration exConf = new UpdateSiteRepositoryConfiguration( ex );
        
        exConf.setArtifactMaxAge( -1 );
        exConf.setMetadataMaxAge( 1440 );

        repo.externalConfigurationImple = exConf;

        repo.setWritePolicy( RepositoryWritePolicy.READ_ONLY.name() );
        repo.setNotFoundCacheTTL( 1440 );

        CRepositoryCoreConfiguration result =
            new CRepositoryCoreConfiguration(
                                              getTemplateProvider().getApplicationConfiguration(),
                                              repo,
                                              new CRepositoryExternalConfigurationHolderFactory<UpdateSiteRepositoryConfiguration>()
                                              {
                                                  public UpdateSiteRepositoryConfiguration createExternalConfigurationHolder(
                                                                                                                      CRepository config )
                                                  {
                                                      return new UpdateSiteRepositoryConfiguration( (Xpp3Dom) config
                                                          .getExternalConfiguration() );
                                                  }
                                              } );

        return result;
    }
}
