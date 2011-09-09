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
package com.sonatype.s2.extractor;

import java.net.URI;
import java.util.Collection;

import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

public class RemoveRepositoryWrapper
{
    public static void removeRepositories( Collection<URI> toRemove )
    {
        getRepositoryTracker().removeRepositories( toRemove.toArray( new URI[toRemove.size()] ), getSession() );
    }

    /**
     * Return a RepositoryTracker appropriate for validating and adding the repository. The default tracker is described
     * by the ProvisioningUI.
     * 
     * @return the repository tracker
     */
    private static RepositoryTracker getRepositoryTracker()
    {
        return ProvisioningUI.getDefaultUI().getRepositoryTracker();
    }

    private static ProvisioningSession getSession()
    {
        return ProvisioningUI.getDefaultUI().getSession();
    }
}
