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
import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;

public class P2RepositoryData
{
    private String location;

    private String id;

    private String nexusRepositoryId;

    private String nexusRepositoryName;

    private String nexusRepositoryRelativePath;

    private String layout;

    private File localPath;

    public File getLocalPath()
    {
        return localPath;
    }

    public void setLocalPath( File localPath )
    {
        this.localPath = localPath;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation( String location )
    {
        this.location = location;
    }

    public String getId()
    {
        if ( id != null )
        {
            return id;
        }

        id = nexusRepositoryId;
        if ( nexusRepositoryRelativePath != null && nexusRepositoryRelativePath.trim().length() > 0 )
        {
            id += "/" + nexusRepositoryRelativePath;
        }
        return id;
    }

    public String getNexusRepositoryId()
    {
        return nexusRepositoryId;
    }

    public void setNexusRepositoryId( String nexusRepositoryId )
    {
        this.nexusRepositoryId = nexusRepositoryId;
    }

    public String getNexusRepositoryRelativePath()
    {
        return nexusRepositoryRelativePath;
    }

    public void setNexusRepositoryRelativePath( String nexusRepositoryRelativePath )
    {
        if ( nexusRepositoryRelativePath != null && nexusRepositoryRelativePath.trim().length() == 0 )
        {
            nexusRepositoryRelativePath = null;
        }
        this.nexusRepositoryRelativePath = nexusRepositoryRelativePath;
    }

    public String getLayout()
    {
        return layout;
    }

    public void setLayout( String layout )
    {
        this.layout = layout;
    }

    public void cleanup()
    {
        if ( localPath == null )
        {
            return;
        }

        try
        {
            FileUtils.deleteDirectory( localPath );
        }
        catch ( IOException e )
        {
            // TODO Proper logging
            e.printStackTrace();
        }
    }

    public String getNexusRepositoryName()
    {
        return nexusRepositoryName;
    }

    public void setNexusRepositoryName( String nexusRepositoryName )
    {
        this.nexusRepositoryName = nexusRepositoryName;
    }
}
