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
package com.sonatype.s2.project.prefs.internal;

import java.io.File;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Encapsulates the logic to realize a member of {@link com.sonatype.s2.project.prefs.PreferenceGroup} when accessing
 * the preference service.
 */
interface IPreferenceGroup
{

    /**
     * Determines whether the preference group is applicable for the current workspace, i.e. whether the corresponding
     * plugins are actually installed.
     * 
     * @param rootNode The (root node of the) preferences to inspect for the group, must not be {@code null}.
     * @return {@code true} if the preference group is available, {@code false} otherwise.
     * @throws BackingStoreException If the preferences could not be inspected.
     */
    public boolean isAvailable( IEclipsePreferences rootNode )
        throws BackingStoreException;

    /**
     * Gets a preference filter that describes the nodes and keys corresponding to this preference group.
     * 
     * @param projectName The name of the project whose preferences should be filtered, may be {@code null} or empty to
     *            create a filter for the workspace preferences.
     * @param rootNode The (root node of the) preferences to filter, must not be {@code null}.
     * @return The preference filter for this group, never {@code null}.
     * @throws BackingStoreException If the preferences could not be inspected.
     */
    public IPreferenceFilter getFilter( String projectName, IEclipsePreferences rootNode )
        throws BackingStoreException;

    /**
     * Gets the external preferences files that are relevant for the preferences group.
     * 
     * @param preferencesDirectory The path to the base directory for external plugin preferences files, i.e. {@code
     *            <workspace>/.metadata/.plugins}, must not be {@code null}.
     * @return The (relative) names of (possibly non-existent) files from the preferences directory, may be empty but
     *         never {@code null}.
     */
    public String[] getFiles( File preferencesDirectory );

    public void notifyFileImported( String file );

    /**
     * Resets the preferences of this group in preparation for an import.
     * 
     * @param rootNode The (root node of the) preferences to filter, must not be {@code null}.
     * @throws BackingStoreException If the preferences could not be reset.
     */
    public void resetPreferences( IEclipsePreferences rootNode )
        throws BackingStoreException;

}
