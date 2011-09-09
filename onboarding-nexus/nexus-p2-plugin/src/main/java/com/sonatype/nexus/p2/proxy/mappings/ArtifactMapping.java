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
package com.sonatype.nexus.p2.proxy.mappings;

import java.util.Map;

public class ArtifactMapping
{
    private Map<String, ArtifactPath> artifactsPath;

    private String repository;

    public ArtifactMapping( String repository, Map<String, ArtifactPath> artifactsPath )
    {
        super();
        this.repository = repository;
        this.artifactsPath = artifactsPath;
    }

    public Map<String, ArtifactPath> getArtifactsPath()
    {
        return artifactsPath;
    }

    public String getRepository()
    {
        return repository;
    }

    public void setArtifactsPath( Map<String, ArtifactPath> artifactsPath )
    {
        this.artifactsPath = artifactsPath;
    }

    public void setRepository( String repository )
    {
        this.repository = repository;
    }
}
