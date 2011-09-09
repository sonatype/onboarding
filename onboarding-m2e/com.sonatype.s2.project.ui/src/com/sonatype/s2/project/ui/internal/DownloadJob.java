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
package com.sonatype.s2.project.ui.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.maven.ide.eclipse.io.ForbiddenException;
import org.maven.ide.eclipse.io.UnauthorizedException;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputComposite;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputDialog;

import com.sonatype.s2.project.validation.api.UnauthorizedStatus;

abstract public class DownloadJob<T>
{
    protected String title;

    protected String urlLabel;

    protected String url;

    private boolean certificateSupportEnabled;

    public DownloadJob( String title, String urlLabel, String url )
    {
        this.title = title;
        this.urlLabel = urlLabel;
        this.url = url;
    }

    abstract public T run( IProgressMonitor monitor )
        throws CoreException;

    public T download( IProgressMonitor monitor )
        throws CoreException
    {
        while ( true )
        {
            try
            {
                return run( monitor );
            }
            catch ( CoreException e )
            {
                final IStatus status = e.getStatus();
                final boolean[] tryAgain = new boolean[] { false };

                if ( unauthorized( status ) )
                {
                    Display.getDefault().syncExec( new Runnable()
                    {
                        public void run()
                        {
                            UrlInputDialog dlg =
                                new UrlInputDialog(
                                                    Display.getDefault().getActiveShell(),
                                                    title,
                                                    urlLabel,
                                                    url,
                                                    UrlInputComposite.ALLOW_ANONYMOUS
                                                        | UrlInputComposite.READ_ONLY_URL
                                                        | ( certificateSupportEnabled ? UrlInputComposite.CERTIFICATE_CONTROLS
                                                                        : 0 ) );
                            dlg.setErrorText( status.getException().getMessage() );
                            if ( dlg.open() == Window.OK )
                            {
                                tryAgain[0] = true;
                            }
                        }
                    } );

                    if ( !tryAgain[0] )
                    {
                        throw new CoreException( new Status( IStatus.CANCEL, Activator.PLUGIN_ID,
                                                             Messages.status_cancel, e ) );
                    }
                }
                else
                {
                    throw e;
                }
            }
        }
    }

    protected boolean unauthorized( IStatus status )
    {
        if ( status instanceof UnauthorizedStatus || status.getException() instanceof UnauthorizedException
            || status.getException() instanceof ForbiddenException )
        {
            return true;
        }
        if ( status.isMultiStatus() )
        {
            for ( IStatus child : status.getChildren() )
            {
                if ( unauthorized( child ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    public void setCertificateSupportEnabled( boolean b )
    {
        certificateSupportEnabled = b;
    }
}
