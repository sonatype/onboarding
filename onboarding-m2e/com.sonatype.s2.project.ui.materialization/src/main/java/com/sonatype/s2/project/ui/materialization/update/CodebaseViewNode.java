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
package com.sonatype.s2.project.ui.materialization.update;

import org.eclipse.swt.graphics.Image;

import com.sonatype.s2.project.core.IWorkspaceCodebase;

public class CodebaseViewNode
{
    private IWorkspaceCodebase workspaceCodebase;

    private String name;

    private String url;

    private Image image;

    private CodebaseViewNode[] children;

    public CodebaseViewNode( IWorkspaceCodebase workspaceCodebase, String name, String url, Image image )
    {
        this.workspaceCodebase = workspaceCodebase;
        this.name = name;
        this.url = url;
        this.image = image;
    }

    public CodebaseViewNode( IWorkspaceCodebase workspaceCodebase, String name, String url, Image image,
                             CodebaseViewNode[] children )
    {
        this( workspaceCodebase, name, url, image );
        this.children = children;
    }

    public String getName()
    {
        return name;
    }

    public String getUrl()
    {
        return url;
    }

    public Image getImage(boolean showStatus)
    {
        return image;
    }

    public String getStatus()
    {
        return null;
    }

    public String getTooltip()
    {
        return getStatus();
    }

    public IWorkspaceCodebase getWorkspaceCodebase()
    {
        return workspaceCodebase;
    }

    public boolean hasChildren()
    {
        return children != null && children.length > 0;
    }

    public CodebaseViewNode[] getChildren()
    {
        return children;
    }

    protected void setChildren( CodebaseViewNode[] children )
    {
        this.children = children;
    }

    public boolean contains( CodebaseViewNode node )
    {
        if ( hasChildren() )
        {
            for ( CodebaseViewNode child : children )
            {
                if ( child == node || child.contains( node ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isUpdateAvailable()
    {
        if ( hasChildren() )
        {
            for ( CodebaseViewNode child : children )
            {
                if ( child.isUpdateAvailable() )
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean isUpdateUnsupported()
    {
    	//not recursive by default..
    	return false;
    }

    public void update()
    {
        if ( hasChildren() )
        {
            for ( CodebaseViewNode child : children )
            {
                if ( child.isUpdateAvailable() )
                {
                    child.update();
                }
            }
        }
    }
}
