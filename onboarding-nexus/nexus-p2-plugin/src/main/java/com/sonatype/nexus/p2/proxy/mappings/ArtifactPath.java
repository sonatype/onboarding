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

public class ArtifactPath
{
    private String md5;

    private String path;

    public ArtifactPath( String path, String md5 )
    {
        super();
        this.path = path;
        this.md5 = md5;
    }

    public String getMd5()
    {
        return md5;
    }

    public String getPath()
    {
        return path;
    }

    public void setMd5( String md5 )
    {
        this.md5 = md5;
    }

    public void setPath( String path )
    {
        this.path = path;
    }
}
