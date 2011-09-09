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

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.templates.TemplateProvider;
import org.sonatype.nexus.templates.TemplateSet;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplateProvider;

@Component( role = TemplateProvider.class, hint = P2LineupContentClass.ID )
public class P2LineupRepositoryTemplateProvider
    extends AbstractRepositoryTemplateProvider
    implements Initializable
{
    @Requirement
    private RepositoryTypeRegistry repositoryTypeRegistry;

    public TemplateSet getTemplates()
    {
        TemplateSet templates = new TemplateSet( null );

        templates.add( new P2LineupRepositoryTemplate( this, P2LineupContentClass.ID, "P2 Lineup" ) );

        return templates;
    }

    public void initialize()
        throws InitializationException
    {
        repositoryTypeRegistry.registerRepositoryTypeDescriptors( new RepositoryTypeDescriptor(
            Repository.class, P2LineupContentClass.ID, "repositories", 1 ) );
    }

}
