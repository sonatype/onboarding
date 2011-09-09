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

import java.io.File;
import java.net.MalformedURLException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.scm.MavenProjectScmInfo;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;

import com.sonatype.m2e.subversive.SubversiveHelper;

@SuppressWarnings( "restriction" )
public class SubversiveScmHandler
    extends org.sonatype.m2e.subversive.internal.SubversiveScmHandler
{

    @Override
    public void checkoutProject( MavenProjectScmInfo info, File location, IProgressMonitor monitor )
        throws CoreException, InterruptedException
    {
        SubversiveHelper.installNonInteractiveOptionProvider();
        try
        {
            super.checkoutProject( info, location, monitor );
        }
        finally
        {
            SubversiveHelper.restoreOptionProvider();
        }
    }

    @Override
    protected IRepositoryContainer getRepositoryContainer( MavenProjectScmInfo info )
        throws CoreException
    {
        IRepositoryContainer container = super.getRepositoryContainer( info );
        try
        {
            SubversiveHelper.setCredentials( info.getFolderUrl(), container.getRepositoryLocation() );
        }
        catch ( MalformedURLException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, "SubversiveScmHandler", 0, "Invalid url "
                + info.getFolderUrl().substring( SVN_SCM_ID.length() ), e ) );
        }
        return container;
    }

}
