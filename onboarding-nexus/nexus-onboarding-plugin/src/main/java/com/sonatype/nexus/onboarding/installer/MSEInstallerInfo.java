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

import org.sonatype.nexus.proxy.repository.Repository;

public class MSEInstallerInfo
{
    public Version getInstallerVersion()
    {
        return installerVersion;
    }

    private Set<String> canInstallVersions;

    private final Version installerVersion;

    private final Repository nexusRepository;

    public MSEInstallerInfo( String installerVersion, Repository nexusRepository )
    {
        this.installerVersion = new Version( installerVersion );
        this.nexusRepository = nexusRepository;
    }

    public Repository getNexusRepository()
    {
        return nexusRepository;
    }

    /**
     * @return true if this MSEInstaller can install the specified version
     */
    public boolean canInstallVersion( String version )
    {
        if (canInstallVersions == null)
        {
            return false;
        }
        return canInstallVersions.contains( version );
    }

    public Set<String> getCanInstallVersions()
    {
        return canInstallVersions;
    }

    public void setCanInstallVersions( Set<String> canInstallVersions )
    {
        this.canInstallVersions = canInstallVersions;
    }

    @Override
    public String toString()
    {
        return "MSE Installer: " + installerVersion + ", can install versions: " + canInstallVersions;
    }
}
