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
package com.sonatype.nexus.p2.updatesite;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.proxy.events.EventInspector;
import org.sonatype.nexus.proxy.events.NexusStartedEvent;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.plexus.appevents.Event;

@Component( role = EventInspector.class, hint = UpdateSiteRepository.ROLE_HINT )
public class RepositoryCreationEventListener
    implements EventInspector
{

    private boolean active;

    public boolean accepts( Event<?> evt )
    {
        active |= evt instanceof NexusStartedEvent;

        return active && evt instanceof RepositoryRegistryEventAdd;
    }

    public void inspect( Event<?> evt )
    {
        Repository repository = ( (RepositoryRegistryEventAdd) evt ).getRepository();

        if ( repository instanceof UpdateSiteRepository )
        {
            repository.setExposed( false );
            ( (UpdateSiteRepository) repository ).mirror( true );
        }
    }

}
