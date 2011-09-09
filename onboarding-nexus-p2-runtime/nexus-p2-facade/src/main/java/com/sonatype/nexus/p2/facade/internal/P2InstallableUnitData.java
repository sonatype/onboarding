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
package com.sonatype.nexus.p2.facade.internal;

import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public class P2InstallableUnitData
{
    private String id;

    private String version;

    private String name;

    private Set<Properties> targetEnvironments = new LinkedHashSet<Properties>();

    public P2InstallableUnitData( String id, String version )
    {
        this.id = id;
        this.version = version;
    }

    public P2InstallableUnitData()
    {
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public Set<Properties> getTargetEnvironments()
    {
        return targetEnvironments;
    }
}
