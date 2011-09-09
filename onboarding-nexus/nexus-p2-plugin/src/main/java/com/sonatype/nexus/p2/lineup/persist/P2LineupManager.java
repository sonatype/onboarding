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
package com.sonatype.nexus.p2.lineup.persist;

import java.util.Set;

import com.sonatype.nexus.p2.lineup.repository.P2LineupRepository;
import com.sonatype.nexus.p2.lineup.resolver.CannotResolveP2LineupException;
import com.sonatype.s2.p2lineup.model.P2Lineup;

public interface P2LineupManager
{

    public static final String FROM_LINEUP_API = "from-lineup-api";
    
    public Set<P2Lineup> getLineups()
        throws P2ConfigurationException;

    public P2Lineup getLineup( P2Gav gav )
        throws NoSuchP2LineupException, P2AccessDeniedException, P2ConfigurationException;

    public P2Lineup addLineup( P2Lineup lineup )
        throws P2LineupStorageException, CannotResolveP2LineupException, P2AccessDeniedException,
        P2ConfigurationException;

    public void validateAccess( P2Lineup lineup, boolean update ) throws P2ConfigurationException, P2AccessDeniedException;
    
    public void validateLineup( P2Lineup lineup ) throws CannotResolveP2LineupException, P2ConfigurationException, P2AccessDeniedException, P2LineupStorageException;

    public P2Lineup updateLineup( P2Lineup lineup )
        throws NoSuchP2LineupException, P2LineupStorageException, CannotResolveP2LineupException,
        P2AccessDeniedException, P2ConfigurationException;

    public void deleteLineup( P2Gav gav )
        throws NoSuchP2LineupException, P2LineupStorageException, P2AccessDeniedException, P2ConfigurationException;

    public P2LineupRepository getDefaultP2LineupRepository()
        throws P2ConfigurationException;
}
