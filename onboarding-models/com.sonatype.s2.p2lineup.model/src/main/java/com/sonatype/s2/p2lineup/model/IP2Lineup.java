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
package com.sonatype.s2.p2lineup.model;

import java.util.Set;

public interface IP2Lineup
{
    public static final String LINEUP_REPOSITORY_ID = "nx-p2lineup";
    
    public static final String LINEUP_REPOSITORY_NAME = "P2 Lineup Repository";

    public static final String LINEUP_FILENAME = "p2lineup.xml";

    public String getId();

    public void setId( String id );

    public String getGroupId();

    public void setGroupId( String groupId );

    public String getVersion();

    public String getName();

    public String getDescription();

    public Set<IP2LineupSourceRepository> getRepositories();

    public Set<IP2LineupInstallableUnit> getRootInstallableUnits();

    public IP2LineupP2Advice getP2Advice();

    public void setP2Advice( IP2LineupP2Advice p2Advice );

    public Set<IP2LineupTargetEnvironment> getTargetEnvironments();
}
