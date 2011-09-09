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
package com.sonatype.s2.project.ui.materialization;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.swt.graphics.Image;

public class Images
{
    public static final ImageDescriptor CHECK_FOR_UPDATES = getImageDescriptor( "check_for_updates.gif" );

    public static final Image ECLIPSE = getImage( "eclipse.gif" );

    public static final Image ECLIPSE_PREFERENCES = getImage( "preferences.gif" );

    public static final Image MAVEN_SETTINGS = getImage( "maven_settings.gif" );

    public static final Image SOURCE_TREE = getImage( "mse-sourcetree.png" );

    public static final ImageDescriptor SYNCHRONIZE = getImageDescriptor( "synchronize.gif" );

    public static final Image TREES = getImage( "tree.gif" );

    public static final Image UPDATE = getImage( "update.gif" );

    public static final ImageDescriptor UPDATE_DESCRIPTOR = getImageDescriptor( "update.gif" );

    public static final Image WORKSPACE = getImage( "workspace.gif" );

    private Images()
    {
    }

    public static Image getImage( String image )
    {
        return Activator.getDefault().getImage( image );
    }

    public static ImageDescriptor getImageDescriptor( String image )
    {
        return Activator.getDefault().getImageDescriptor( image );
    }

    public static Image getOverlayImage( String basekey, String overlaykey, int quadrant )
    {
        String key = basekey + overlaykey + quadrant;
        ImageRegistry imageRegistry = Activator.getDefault().getImageRegistry();
        Image image = imageRegistry.get( key );
        if ( image == null )
        {
            Image baseImage = getImage( basekey );
            ImageDescriptor overlayDescriptor = getImageDescriptor( overlaykey );

            ImageDescriptor imageDescriptor = createOverlayDescriptor( baseImage, overlayDescriptor, quadrant );
            imageRegistry.put( key, imageDescriptor );
            image = imageRegistry.get( key );
        }
        return image;
    }

    private static ImageDescriptor createOverlayDescriptor( Image baseImage, ImageDescriptor overlay, int quadrant )
    {
        return new DecorationOverlayIcon( baseImage, overlay, quadrant );
    }
}
