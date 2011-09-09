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
package com.sonatype.s2.project.model;

import java.util.List;

public interface IS2Project
{
    public static final String PROJECT_DESCRIPTOR_FILENAME = "mse-codebase.xml";

    public static final String PROJECT_DESCRIPTOR_PATH = "mse";

    public static final String PROJECT_ICON_FILENAME = "mse-codebase-icon.png";

    public static final String PROJECT_PREFERENCES_FILENAME = "mse-codebase-preferences.jar";

    public static final String PROJECT_REPOSITORY_ID = "nx-codebase-repo";

    /**
     * Conventional codebase repository path relative to Nexus base URL.
     */
    public static final String PROJECT_REPOSITORY_PATH = "/content/repositories/" + PROJECT_REPOSITORY_ID;

    public static final String PROJECT_REPOSITORY_NAME = "Codebase Repository";

    public static final String DEFAULT_INSTALL_PATH =
        "@user.home@/mse/@mse-codebase.name@-@mse-codebase.version@/eclipse";

    public static final String DEFAULT_WORKSPACE_PATH =
        "@user.home@/mse/@mse-codebase.name@-@mse-codebase.version@/workspace";

    // If you change the value of this constant, you have to add it to the list in the mse.installer.compatible.versions
    // variable, in the s2-installer-aggregator/pom.xml file
    // (https://svn.sonatype.com/repos/code/products/sonatype-studio/trunk/onboarding/installer/pom.xml)
    public static final String CURRENT_INSTALLER_VERSION = "1.0.6";

    /**
     * Version suffix, for example 1.2.3-HEAD, that denotes codebase descriptor matching head of a development branch.
     * As the code in the branch evolves over time, HEAD codebase descriptor can change to reflect the changes in the
     * code.
     */
    public static final String HEAD_VERSION_SUFFIX = "HEAD";

    public String getGroupId();

    public void setGroupId( String groupId );

    public String getArtifactId();

    public void setArtifactId( String name );

    public String getVersion();

    public String getName();

    public void setName( String name );

    public void setVersion( String version );

    public String getDescription();

    public void setDescription( String description );

    public String getInstallerVersion();

    public void setInstallerVersion( String installerVersion );

    public String getHomeUrl();

    public void setHomeUrl( String url );

    public String getDocsUrl();

    public void setDocsUrl( String url );

    /** Returns the location of the (optional) P2 lineup associated with this project */
    public IP2LineupLocation getP2LineupLocation();

    public void setP2LineupLocation( IP2LineupLocation p2LineupLocation );

    public List<IS2Module> getModules();

    public void addModule( IS2Module module );

    /** Returns an optional Maven settings.xml location */
    public IMavenSettingsLocation getMavenSettingsLocation();

    public void setMavenSettingsLocation( IMavenSettingsLocation mavenSettingsLocation );

    /**
     * Gets the location to the Eclipse preferences to import during onboarding.
     * 
     * @return The location to the Eclipse preferences to import or {@code null} if none.
     */
    public IEclipsePreferencesLocation getEclipsePreferencesLocation();

    public void setEclipsePreferencesLocation( IEclipsePreferencesLocation eclipsePreferencesLocation );

    /**
     * Gets the location for the local Eclipse installation to create during onboarding.
     * 
     * @return The location for the Eclipse installation to create or {@code null} if not specified.
     */
    public IEclipseInstallationLocation getEclipseInstallationLocation();

    public void setEclipseInstallationLocation( IEclipseInstallationLocation eclipseInstallationLocation );

    /**
     * Gets the location for the local Eclipse workspace to create during onboarding.
     * 
     * @return The location for the Eclipse workspace to create or {@code null} if not specified.
     */
    public IEclipseWorkspaceLocation getEclipseWorkspaceLocation();

    public void setEclipseWorkspaceLocation( IEclipseWorkspaceLocation eclipseWorkspaceLocation );

    /**
     * Gets the prerequisites to successfully materialize this project.
     * 
     * @return The project's prerequisites or {@code null} if unknown.
     */
    public IPrerequisites getPrerequisites();

    public void setPrerequisites( IPrerequisites prerequisites );

    /**
     * Original URL of this descriptor or null, if the descriptor was not loaded from a URL. This attribute is not
     * persisted in the model.
     */
    public String getDescriptorUrl();

    public void setDescriptorUrl( String url );

    public boolean isRequiresMavenSettings();
    public void setRequiresMavenSettings( boolean requiresMavenSettings );
}
