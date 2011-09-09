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
package com.sonatype.nexus.onboarding.installer;

import java.util.Set;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.StorageFileItem;

public interface MSEInstallerManager
{
    /** Returns a set of all available installers */
    Set<MSEInstallerInfo> getInstallers()
        throws NoSuchRepositoryException, StorageException, AccessDeniedException, ItemNotFoundException, IllegalOperationException;

    /**
     * Returns the installer to be used for the specified version or null if there is no installer for the specified
     * version
     */
    MSEInstallerInfo resolveInstaller( String version )
        throws NoSuchRepositoryException, StorageException, AccessDeniedException, ItemNotFoundException, IllegalOperationException;;

    /** Returns the jnlp item for the given MSE installer */
    StorageFileItem getJnlpItem( MSEInstallerInfo installer )
        throws StorageException, AccessDeniedException, ItemNotFoundException, IllegalOperationException;
}
