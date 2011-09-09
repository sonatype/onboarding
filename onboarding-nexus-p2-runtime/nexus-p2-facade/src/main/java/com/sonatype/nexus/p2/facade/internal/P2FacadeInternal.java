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

import java.io.File;
import java.util.Set;

import org.sonatype.tycho.p2.facade.internal.P2Logger;

public interface P2FacadeInternal
{
    public void getRepositoryArtifacts( String uri, String username, String password, File destination, File artifactMappingsXmlFile );

    public void getRepositoryContent( String uri, String username, String password, File destination );

    public void setProxySettings( String proxyHostname, int proxyPort, String username, String password, Set<String> nonProxyHosts );

    public void generateSiteMetadata( File location, File metadataDir, String name );

    public P2Resolver createResolver();

    public void resolveP2Lineup( P2LineupResolutionRequest request, P2Resolver p2Resolver, P2LineupResolutionResult result );

    public void setLogger( P2Logger logger );
    
    public boolean containsExecutable( File metadataRepository );
}
