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
package com.sonatype.nexus.p2.facade;

import java.io.File;

import org.sonatype.plugin.metadata.GAVCoordinate;

import com.sonatype.nexus.p2.facade.internal.P2LineupResolutionRequest;
import com.sonatype.nexus.p2.facade.internal.P2LineupResolutionResult;

public interface P2Facade
{
    void initializeP2( GAVCoordinate pluginGav );

    public void getRepositoryArtifacts( String url, String username, String password, File destination, File artifactMappingsXmlFile );

    public void getRepositoryContent( String url, String username, String password, File destination );

    public void generateSiteMetadata( File location, File metadataDir, String name );

    public void resolveP2Lineup( P2LineupResolutionRequest request, P2LineupResolutionResult result );
    
    public boolean containsExecutable( File metadataRepository );
}
