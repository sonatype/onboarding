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
package com.sonatype.s2.project.prefs;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Set;
import java.util.jar.JarInputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Assists in managing the workspace/project preferences for onboarding.
 */
public interface IPreferenceManager
{

    /**
     * Gets the set of preference groups applicable for the current workspace.
     * 
     * @return The preference groups applicable for the current workspace, may be empty but never {@code null}.
     * @throws CoreException If the preferences could not be inspected.
     */
    public Set<PreferenceGroup> getPreferenceGroups()
        throws CoreException;

    /**
     * Gets the set of preference groups that have been exported to the specified URL. Usually, the returned strings
     * denote members of {@link PreferenceGroup}. But preference archives created by different versions of S2 might list
     * other preference groups.
     * 
     * @param preferencesUrl The location of the export file to read the preference groups from, must not be {@code
     *            null}.
     * @param monitor The monitor to notify of progress feedback, may be {@code null}.
     * @return The preference groups exported to the given URL, may be empty but never {@code null}.
     * @throws CoreException If the operation failed.
     */
    public Set<PreferenceGroup> getPreferenceGroups( String preferencesUrl, IProgressMonitor monitor )
        throws CoreException;

    /**
     * Gets the set of preference groups contained in the given stream. Usually, the returned strings
     * denote members of {@link PreferenceGroup}. But preference archives created by different versions of S2 might list
     * other preference groups.
     * 
     * @param jis The jar stream to read from.
     * @return The preference groups exported to the given URL, may be empty but never {@code null}.
     */
    public Set<PreferenceGroup> getPreferenceGroups( JarInputStream jis );

    /**
     * Retrieves the contents of the preferences file stored in the given stream.
     * 
     * @param jis The jar stream to read from.
     * @return The {@code s2.epf} file contents.
     * @throws IOException If transfer fails.
     */
    public byte[] getEclipsePreferences( JarInputStream jis )
        throws IOException;

    /**
     * Exports the specified workspace preferences to the given stream.
     * 
     * @param output The output stream to write the preferences to, must not be {@code null}. This stream has to be
     *            closed by the caller.
     * @param preferences The group of preferences to export, may be {@code null} to export all groups.
     * @param monitor The monitor to notify of progress feedback, may be {@code null}.
     * @throws CoreException If the preferences could not be exported.
     */
    public void exportPreferences( OutputStream output, Collection<PreferenceGroup> preferences,
                                   IProgressMonitor monitor )
        throws CoreException;

    /**
     * Exports the specified workspace preferences to the given file.
     * 
     * @param file The output file to write the preferences to, must not be {@code null}. Non-existing parent
     *            directories will be created automatically.
     * @param preferences The group of preferences to export, may be {@code null} to export all groups.
     * @param monitor The monitor to notify of progress feedback, may be {@code null}.
     * @throws CoreException If the preferences could not be exported.
     */
    public void exportPreferences( File file, Collection<PreferenceGroup> preferences, IProgressMonitor monitor )
        throws CoreException;

    /**
     * Deploys a previously exported set of preferences to a remote server for reference within the project descriptor.
     * 
     * @param preferencesFile The path to the exported preferences, must not be {@code null}.
     * @param deploymentUrl The target location to store the preferences at, must not be {@code null}.
     * @param monitor The monitor to notify of progress feedback, may be {@code null}.
     * @throws CoreException If the deployment failed.
     */
    public void deployPreferences( File preferencesFile, String deploymentUrl, IProgressMonitor monitor )
        throws CoreException;

    /**
     * Imports the workspace/project preferences from the specified export file.
     * 
     * @param preferencesUrl The location of the export file to import the preferences from, must not be {@code null}.
     * @param monitor The monitor to notify of progress feedback, may be {@code null}.
     * @throws CoreException If the import failed.
     */
    public void importPreferences( String preferencesUrl, IProgressMonitor monitor )
        throws CoreException;

}
