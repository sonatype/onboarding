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

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.RemoveRepositoryOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator;
import org.eclipse.ui.statushandlers.StatusManager;

public class RemoveRepositoryWrapper
{
    public static void removeRepositories( Collection<URI> toRemove )
    {
        RemoveRepositoryOperation op = getRepositoryManipulator().getRemoveOperation( toRemove.toArray( new URI[toRemove.size()] ) );
        ProvisioningOperationRunner.schedule(op, StatusManager.SHOW | StatusManager.LOG);
    }
    
    private static RepositoryManipulator getRepositoryManipulator() {
        return Policy.getDefault().getRepositoryManipulator();
    }

}
