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
package com.sonatype.m2e.cvs.internal;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.scm.MavenProjectScmInfo;
import org.eclipse.m2e.scm.spi.ScmHandler;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.operations.CheckoutSingleProjectOperation;
import org.eclipse.ui.IWorkbenchPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.validation.cvs.CvsHelper;

public class CvsScmHandler
    extends ScmHandler
{
    private static final String ANONYMOUS = "anonymous";

    private static final String CVS_SCM_ID = "scm:cvs:";

    private static final Logger log = LoggerFactory.getLogger( CvsScmHandler.class );

    @SuppressWarnings( "restriction" )
    public void checkoutProject( MavenProjectScmInfo info, File location, IProgressMonitor monitor )
        throws CoreException, InterruptedException
    {
        log.debug( "Checking out project from {} to {}", info, location );

        SubMonitor subMon = SubMonitor.convert( monitor, 1000 );
        CVSURI uri;
        try
        {
            uri = CVSURI.fromUri( new URI( info.getRepositoryUrl().substring( CVS_SCM_ID.length() ) ) );
        }
        catch ( URISyntaxException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, getClass().getName(), -1,
                                                 "Invalid repository location", e ) );
        }

        CVSProviderPlugin.getPlugin().setAutoshareOnImport( true );

        ICVSRepositoryLocation repository = uri.getRepository();
        CvsHelper.setNonInteractiveUserAuthenticator( repository );

        if ( !repository.getMethod().getName().equals( "pserver" ) )
            throw new CoreException( new Status( IStatus.ERROR, getClass().getName(), "CVS method "
                + repository.getMethod().getName() + " is unsupported." ) );

        if ( ( (CVSRepositoryLocation) repository ).isUsernameMutable() )
        {
            repository.setUsername( info.getUsername() != null ? info.getUsername() : ANONYMOUS );
            repository.setPassword( info.getPassword() != null ? info.getPassword() : ANONYMOUS );
        }
        repository.validateConnection( monitor );

        ICVSRemoteFolder folder = repository.getRemoteFolder( uri.getPath().toString(), new CVSTag() );

        // We need to create an Eclipse Project to checkout the code into, once
        // the checkout is complete the project needs to be removed but the
        // contents should remain on disk
        try
        {
            IWorkbenchPart part = null;
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject( location.getName() );

            CheckoutSingleProjectOperation op = new CheckoutSingleProjectOperation( part, folder, project, null, false );
            op.run( subMon.newChild( 980 ) );

            project.delete( false, true, monitor );
        }
        catch ( InvocationTargetException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, getClass().getName(),
                                                 "An error occured during CVS checkout", e ) );
        }
    }
}
