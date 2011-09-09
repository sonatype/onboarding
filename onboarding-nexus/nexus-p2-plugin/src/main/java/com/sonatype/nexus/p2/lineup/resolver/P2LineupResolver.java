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
package com.sonatype.nexus.p2.lineup.resolver;

import com.sonatype.nexus.p2.lineup.repository.P2LineupRepository;
import com.sonatype.s2.p2lineup.model.IP2Lineup;

public interface P2LineupResolver
{
    void resolveLineup( P2LineupRepository p2LineupRepository, IP2Lineup p2Lineup, boolean validateOnly )
        throws CannotResolveP2LineupException;
}
