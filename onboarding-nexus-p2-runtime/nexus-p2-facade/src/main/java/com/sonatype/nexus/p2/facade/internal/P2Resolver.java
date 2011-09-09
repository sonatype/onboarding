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

import java.net.URI;
import java.util.Properties;
import java.util.Set;

import org.sonatype.tycho.p2.facade.internal.P2Logger;

public interface P2Resolver
{
    public static final String TYPE_OSGI_BUNDLE = "eclipse-plugin";
    
    /**
     * Pseudo artifact type used to denote P2 installable unit dependencies
     */
    public static final String TYPE_INSTALLABLE_UNIT = "p2-installable-unit";

    public void addMetadataRepository( URI location );

    public void addArtifactRepository( String repositoryId, URI location );
    
    public void addP2Repository( String repositoryId, URI location );
    
    public void addRootInstallableUnit( String iuId, String iuName, String iuVersion,
                                        Set<Properties> iuTargetEnvironments,
                                        P2LineupResolutionResult result );
    
    public void setProperties( Properties properties );
    
    public Object resolve( P2LineupResolutionResult externalResult );

    public void addDependency( String type, String id, String version );

    public void setLogger( P2Logger logger );

    void setCredentials( URI location, String username, String password );

    public void cleanCredentials();

    public void cleanupRepositories();
}
