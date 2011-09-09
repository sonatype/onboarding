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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.AddRepositoryOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryLocationValidator;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator;
import org.eclipse.ui.statushandlers.StatusManager;

public class AddRepositoryWrapper
{
    /*
     * Adds the given repository addresses to the manager
     * @return addresses previously unknown
     */
    public Collection<URI> addRepositories(Collection<String> urls) {
        Collection<URI> newRepos = new ArrayList<URI>();
        Collection<URI> known = new HashSet<URI>();
        known.addAll( Arrays.asList( getRepositoryManipulator().getKnownRepositories()) );

        for (String url : urls) {
            URI repo = RepositoryLocationValidator.locationFromString(url);
            if (known.contains( repo ))
                continue;
            ProvisioningOperationRunner.schedule(getOperation( repo ), StatusManager.SHOW | StatusManager.LOG);
            newRepos.add(repo);
        }
        return newRepos;
    }

    public final void addRepositoryToCurrentIDE(String location, String name) {
        URI addedLocation = RepositoryLocationValidator.locationFromString( location );
        AddRepositoryOperation op = getOperation(addedLocation);
        String nick = name != null ? name.trim() : "";
        if (nick.length() > 0)
            op.setNicknames(new String[] {nick});
        ProvisioningOperationRunner.schedule(op, StatusManager.SHOW | StatusManager.LOG);
    }
    
    /**
     * Get an add operation appropriate for this dialog.  The default behavior
     * is to retrieve it from the policy, but subclasses may override.
     * 
     * @param repositoryLocation to be added
     * @return the add operation
     */
    private AddRepositoryOperation getOperation(URI repositoryLocation) {
        return getRepositoryManipulator().getAddOperation(repositoryLocation);
    }
    /**
     * Return a RepositoryManipulator appropriate for validating and adding the
     * repository.
     * 
     * The default manipulator is described by the policy.  Subclasses may override.
     * @return the repository manipulator
     */
    private RepositoryManipulator getRepositoryManipulator() {
        return Policy.getDefault().getRepositoryManipulator();
    }        
}
