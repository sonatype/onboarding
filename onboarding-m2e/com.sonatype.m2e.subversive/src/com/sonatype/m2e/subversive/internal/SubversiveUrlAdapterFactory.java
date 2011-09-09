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
package com.sonatype.m2e.subversive.internal;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.m2e.scm.ScmUrl;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.repository.model.IResourceTreeNode;

public class SubversiveUrlAdapterFactory
    implements IAdapterFactory
{

    private static final Class<?>[] LIST = { ScmUrl.class };

    public Class<?>[] getAdapterList()
    {
        return LIST;
    }

    @SuppressWarnings( "unchecked" )
    public Object getAdapter( Object adaptableObject, Class adapterType )
    {
        if ( ScmUrl.class.equals( adapterType ) && ( adaptableObject instanceof IResourceTreeNode ) )
        {
            IRepositoryResource repositoryResource = ( (IResourceTreeNode) adaptableObject ).getRepositoryResource();
            String scmUrl = "scm:svn:" + repositoryResource.getUrl();
            String scmParentUrl = null;
            IRepositoryResource parent = repositoryResource.getParent();
            if ( parent != null )
            {
                scmParentUrl = "scm:svn:" + parent.getUrl();
            }
            return new ScmUrl( scmUrl, scmParentUrl );
        }
        return null;
    }

}
