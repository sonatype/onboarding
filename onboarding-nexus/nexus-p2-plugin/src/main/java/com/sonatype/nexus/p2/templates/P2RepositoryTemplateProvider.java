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

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.templates.TemplateProvider;
import org.sonatype.nexus.templates.TemplateSet;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplateProvider;

import com.sonatype.nexus.p2.group.P2GroupRepository;
import com.sonatype.nexus.p2.proxy.P2ProxyRepository;
import com.sonatype.nexus.p2.updatesite.UpdateSiteRepository;

@Component( role = TemplateProvider.class, hint = P2RepositoryTemplateProvider.PROVIDER_ID )
public class P2RepositoryTemplateProvider
    extends AbstractRepositoryTemplateProvider
    implements Initializable
{
    public static final String PROVIDER_ID = "p2-repository";

    private static final String P2_PROXY = "p2_proxy";

    private static final String P2_UPDATE_SITE = "p2_updatesite";

    private static final String P2_GROUP = "p2_group";

    @Requirement
    private RepositoryTypeRegistry repositoryTypeRegistry;

    public TemplateSet getTemplates()
    {
        TemplateSet templates = new TemplateSet( null );

        try
        {
            templates.add( new P2ProxyRepositoryTemplate( this, P2_PROXY, "P2 Proxy Repository" ) );
            templates.add( new UpdateSiteRepositoryTemplate( this, P2_UPDATE_SITE, "P2 Update Site Proxy Repository" ) );
            templates.add( new P2GroupRepositoryTemplate( this, P2_GROUP, "P2 Repository Group" ) );
        }
        catch ( Exception e )
        {
            // will not happen
        }

        return templates;
    }

    public void initialize()
        throws InitializationException
    {
        repositoryTypeRegistry.registerRepositoryTypeDescriptors( new RepositoryTypeDescriptor(
            Repository.class, P2ProxyRepository.ROLE_HINT, "repositories" ) );

        repositoryTypeRegistry.registerRepositoryTypeDescriptors( new RepositoryTypeDescriptor(
            Repository.class, UpdateSiteRepository.ROLE_HINT, "repositories" ) );

        repositoryTypeRegistry.registerRepositoryTypeDescriptors( new RepositoryTypeDescriptor(
            GroupRepository.class, P2GroupRepository.ROLE_HINT, "groups" ) );

    }
}
