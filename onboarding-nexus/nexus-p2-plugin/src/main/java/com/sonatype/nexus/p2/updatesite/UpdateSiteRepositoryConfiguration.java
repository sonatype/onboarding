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
package com.sonatype.nexus.p2.updatesite;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.nexus.proxy.repository.AbstractProxyRepositoryConfiguration;

public class UpdateSiteRepositoryConfiguration
    extends AbstractProxyRepositoryConfiguration
{
    public static final String ARTIFACT_MAX_AGE = "artifactMaxAge";

    public static final String METADATA_MAX_AGE = "metadataMaxAge";
    
    public UpdateSiteRepositoryConfiguration( Xpp3Dom configuration )
    {
        super( configuration );
    }

    public int getArtifactMaxAge()
    {
        return Integer.parseInt( getNodeValue( getRootNode(), ARTIFACT_MAX_AGE, "1440" ) );
    }

    public void setArtifactMaxAge( int age )
    {
        setNodeValue( getRootNode(), ARTIFACT_MAX_AGE, String.valueOf( age ) );
    }

    public int getMetadataMaxAge()
    {
        return Integer.parseInt( getNodeValue( getRootNode(), METADATA_MAX_AGE, "1440" ) );
    }

    public void setMetadataMaxAge( int age )
    {
        setNodeValue( getRootNode(), METADATA_MAX_AGE, String.valueOf( age ) );
    }
}
