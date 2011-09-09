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

public interface IP2LineupInstallableUnit
{
    public String getName();

    public String getId();

    public String getVersion();

    /**
     * I know this looks odd, but empty list means "any target environment".
     * 
     * @return
     */
    public Set<IP2LineupTargetEnvironment> getTargetEnvironments();

    public void setTargetEnvironments( Set<IP2LineupTargetEnvironment> list );
}
