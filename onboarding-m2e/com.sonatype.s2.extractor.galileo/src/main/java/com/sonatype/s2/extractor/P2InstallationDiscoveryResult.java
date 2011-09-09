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

import java.util.ArrayList;
import java.util.List;

import com.sonatype.s2.p2lineup.model.IP2LineupInstallableUnit;
import com.sonatype.s2.p2lineup.model.IP2LineupSourceRepository;

public class P2InstallationDiscoveryResult
{
    private final List<IP2LineupSourceRepository> repos;

    private final List<IP2LineupInstallableUnit> ius;

    public P2InstallationDiscoveryResult( ArrayList<IP2LineupSourceRepository> repos,
                                          ArrayList<IP2LineupInstallableUnit> ius )
    {
        this.repos = repos;
        this.ius = ius;
    }

    public List<IP2LineupSourceRepository> getSourceRepositories()
    {
        return repos;
    }

    public List<IP2LineupInstallableUnit> getRootIUs()
    {
        return ius;
    }
}
