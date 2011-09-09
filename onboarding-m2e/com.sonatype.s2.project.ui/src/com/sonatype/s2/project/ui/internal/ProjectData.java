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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.common.S2ProjectCommon;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.IS2ProjectCatalogEntry;

public class ProjectData
{
    private static Logger log = LoggerFactory.getLogger( ProjectData.class );

    private IS2ProjectCatalogEntry entry;

    private String url;

    private IS2Project project;

    private Image image;

    public ProjectData( IS2ProjectCatalogEntry entry )
    {
        this( entry.getEffectiveDescriptorUrl() );
        this.entry = entry;
    }

    public ProjectData( String url )
    {
        this.url = url;
    }

    public IS2Project getProject()
    {
        return project;
    }

    public IS2ProjectCatalogEntry getEntry()
    {
        return entry;
    }

    public Image getImage()
    {
        return image;
    }

    public String getUrl()
    {
        return url;
    }

    public void load( IProgressMonitor monitor )
        throws CoreException
    {
        load( monitor, false );
    }

    public void load( IProgressMonitor monitor, final boolean loadImage )
        throws CoreException
    {
        new DownloadJob<IS2Project>( Messages.projectData_errors_projectDescriptorAuthenticationError,
                                     Messages.projectData_descriptorUrlLabel, url )
        {
            @Override
            public IS2Project run( IProgressMonitor monitor )
                throws CoreException
            {
                final SubMonitor progress =
                    SubMonitor.convert( monitor, NLS.bind( Messages.projectData_jobs_loadingProjectDetails,
                                                           entry == null ? url : entry.getName() ), 1 );

                String message = null;
                Exception cause = null;
                try
                {
                    InputStream is = S2IOFacade.openStream( url, progress.newChild( 1 ) );
                    try
                    {
                        project = S2ProjectCommon.loadProject( is, true );
                        project.setDescriptorUrl( url );
                    }
                    finally
                    {
                        IOUtil.close( is );
                    }

                    if ( loadImage )
                    {
                        int n = url.lastIndexOf( '/' );
                        if ( n > 0 )
                        {
                            final String imageUrl = url.substring( 0, n + 1 ) + IS2Project.PROJECT_ICON_FILENAME;
                            try
                            {
                                is = S2IOFacade.openStream( imageUrl, progress.newChild( 1 ) );
                                final ImageData imageData = new ImageData( is );

                                Display.getDefault().syncExec( new Runnable()
                                {
                                    public void run()
                                    {
                                        Images.createImageDescriptor( imageUrl, imageData );
                                        image = Images.getImage( imageUrl );
                                    }
                                } );
                            }
                            catch ( SWTException e )
                            {
                                log.error( "Error loading project icon: " + imageUrl, e );
                            }
                            finally
                            {
                                if ( is != null )
                                {
                                    is.close();
                                }
                            }
                        }
                    }
                    return project;
                }
                catch ( IOException e )
                {
                    message = NLS.bind( Messages.projectData_errors_errorLoadingProjectDescriptor, e.getMessage() );
                    cause = e;
                }
                catch ( URISyntaxException e )
                {
                    message = NLS.bind( Messages.projectData_errors_invalidUrl, url );
                    cause = e;
                }
                log.error( message, cause );

                throw new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID, message, cause ) );
            }
        }.download( monitor );
    }
}
