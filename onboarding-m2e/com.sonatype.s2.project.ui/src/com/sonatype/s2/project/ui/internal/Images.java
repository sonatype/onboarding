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

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Images
{
    private static Logger log = LoggerFactory.getLogger( Images.class );

    public static final Image ARTIFACT = getImage( "artifact.gif" );

    public static final Image CATALOG_ROOT = getImage( "maven_indexes.gif" );

    public static final Image CATALOG_ROOT_ERROR = getImage( "maven_indexes_error.gif" );

    public static final Image CATALOG_ENTRY = getImage( "maven_index.gif" );

    public static final Image CATALOG_PROJECT = getImage( "pom_obj.gif" );

    public static final Image CODEBASE = getImage( "mse-codebase.png" );

    public static final Image DEFAULT_DOCUMENT_IMAGE = getImage( "document48.png" );

    public static final Image DEFAULT_FOLDER_IMAGE = getImage( "folder48.png" );

    public static final Image DEFAULT_PROJECT_IMAGE = getImage( "default_project_image.png" );

    public static final Image FEED = getImage( "rss_obj.gif" );

    public static final Image GROUP = getImage( "group.gif" );

    public static final Image INSTALLABLE_UNIT = getImage( "feature_obj.gif" );

    public static final Image ERROR_INSTALLABLE_UNIT = getOverlayImage( "feature_obj.gif", "error_ovr.gif",
                                                                        IDecoration.BOTTOM_LEFT );

    public static final Image WARN_INSTALLABLE_UNIT = getOverlayImage( "feature_obj.gif", "warn_ovr.gif",
                                                                       IDecoration.BOTTOM_LEFT );

    public static final Image INFO_INSTALLABLE_UNIT = getOverlayImage( "feature_obj.gif", "info_ovr.gif",
                                                                       IDecoration.BOTTOM_LEFT );

    public static final Image ERROR_BADGE = getImage( "error_ovr.gif" );

    public static final Image LOCK = getImage( "lock.png" );

    public static final Image LOCK_ERROR = getImage( "lock_error.png" );

    public static final Image LOCK_UNLOCKED = getImage( "lock_unlocked.png" );

    public static final Image MAVEN_OBJECT = getImage( "pom_obj.gif" );

    public static final Image PLATFORM_LINUX = getImage( "platform_linux.png" );

    public static final Image PLATFORM_MACOSX = getImage( "platform_macosx.png" );

    public static final Image PLATFORM_WIN32 = getImage( "platform_win32.png" );

    public static final Image PUBLISH = getImage( "publish.png" );

    public static final Image REPOSITORY = getImage( "metadata_repo_obj.gif" );

    public static final Image ERROR_REPOSITORY = getOverlayImage( "metadata_repo_obj.gif", "error_ovr.gif",
                                                                  IDecoration.BOTTOM_LEFT );

    public static final Image WARN_REPOSITORY = getOverlayImage( "metadata_repo_obj.gif", "warn_ovr.gif",
                                                                 IDecoration.BOTTOM_LEFT );

    public static final Image INFO_REPOSITORY = getOverlayImage( "metadata_repo_obj.gif", "info_ovr.gif",
                                                                 IDecoration.BOTTOM_LEFT );

    public static final Image SOURCE_TREE = getImage( "mse-sourcetree.png" );

    public static final Image STATUS_ERROR = getImage( "error_st_obj.gif" );

    public static final Image STATUS_INFO = getImage( "info_st_obj.gif" );

    public static final Image STATUS_OK = getImage( "ok_st_obj.gif" );

    public static final Image STATUS_WARNING = getImage( "warning_st_obj.gif" );

    public static final Image VERSION = getImage( "version.gif" );

    public static final ImageDescriptor ADD_CATALOG_DESCRIPTOR = getImageDescriptor( "add_index.gif" );

    public static final ImageDescriptor CATALOG_ENTRY_DESCRIPTOR = getImageDescriptor( "maven_index.gif" );

    public static final ImageDescriptor LOCK_DESCRIPTOR = getImageDescriptor( "lock.png" );

    public static final ImageDescriptor MATERIALIZE_PROJECT_DESCRIPTOR = getImageDescriptor( "import_m2_project.gif" );

    public static final ImageDescriptor REFRESH_DESCRIPTOR = getImageDescriptor( "refresh.gif" );

    public static ImageDescriptor getImageDescriptor( String key )
    {
        try
        {
            ImageRegistry imageRegistry = getImageRegistry();
            if ( imageRegistry != null )
            {
                ImageDescriptor imageDescriptor = imageRegistry.getDescriptor( key );
                if ( imageDescriptor == null )
                {
                    imageDescriptor = createDescriptor( key );
                    imageRegistry.put( key, imageDescriptor );
                }
                return imageDescriptor;
            }
        }
        catch ( Exception ex )
        {
            log.error( key, ex );
        }
        return null;
    }

    public static ImageDescriptor createImageDescriptor( String key, ImageData imageData )
    {
        try
        {
            ImageRegistry imageRegistry = getImageRegistry();
            if ( imageRegistry != null )
            {
                ImageDescriptor imageDescriptor = imageRegistry.getDescriptor( key );
                if ( imageDescriptor != null )
                {
                    imageRegistry.remove( key );
                }
                {
                    imageDescriptor = ImageDescriptor.createFromImageData( imageData );
                    imageRegistry.put( key, imageDescriptor );
                }
                return imageDescriptor;
            }
        }
        catch ( Exception ex )
        {
            log.error( key, ex );
        }
        return null;
    }

    public static ImageDescriptor getOverlayImageDescriptor( String basekey, String overlaykey, int quadrant )
    {
        String key = basekey + overlaykey;
        try
        {
            ImageRegistry imageRegistry = getImageRegistry();
            if ( imageRegistry != null )
            {
                ImageDescriptor imageDescriptor = imageRegistry.getDescriptor( key );
                if ( imageDescriptor == null )
                {
                    ImageDescriptor base = getImageDescriptor( basekey );
                    ImageDescriptor overlay = getImageDescriptor( overlaykey );
                    if ( base == null || overlay == null )
                    {
                        log.error( "cannot construct overlay image descriptor for " + basekey + " " + overlaykey );
                        return null;
                    }
                    imageDescriptor = createOverlayDescriptor( base, overlay, quadrant );
                    imageRegistry.put( key, imageDescriptor );
                }
                return imageDescriptor;
            }
        }
        catch ( Exception ex )
        {
            log.error( key, ex );
        }
        return null;
    }

    public static Image getImage( String key )
    {
        getImageDescriptor( key );
        ImageRegistry imageRegistry = getImageRegistry();
        return imageRegistry == null ? null : imageRegistry.get( key );
    }

    public static Image getOverlayImage( String base, String overlay, int quadrant )
    {
        getOverlayImageDescriptor( base, overlay, quadrant );
        ImageRegistry imageRegistry = getImageRegistry();
        return imageRegistry == null ? null : imageRegistry.get( base + overlay );
    }

    private static ImageRegistry getImageRegistry()
    {
        Activator plugin = Activator.getDefault();
        return plugin == null ? null : plugin.getImageRegistry();
    }

    private static ImageDescriptor createDescriptor( String image )
    {
        try
        {
            return ImageDescriptor.createFromURL( new URL( image ) );
        }
        catch ( MalformedURLException e )
        {
            return AbstractUIPlugin.imageDescriptorFromPlugin( Activator.PLUGIN_ID, "icons/" + image );
        }
    }

    private static ImageDescriptor createOverlayDescriptor( ImageDescriptor base, ImageDescriptor overlay, int quadrant )
    {
        return new DecorationOverlayIcon( base.createImage(), overlay, quadrant );
    }

}
