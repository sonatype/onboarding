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
package com.sonatype.s2.project.ui.catalog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import com.sonatype.s2.project.model.IS2Project;

public class ProjectImage
{
    public final static int IMAGE_SIZE = 48;

    public static Image getWorkspaceImage( IPath location )
    {
        return getImage( location.append( IS2Project.PROJECT_ICON_FILENAME ) );
    }

    public static Image getImage( IPath location )
    {
        Image image = null;
        ImageRegistry imageRegistry = Activator.getDefault().getImageRegistry();
        String key = location.toString();

        imageRegistry.remove( key );

        image = loadImage( location );
        if ( image != null )
        {
            imageRegistry.put( key, image );
        }

        return image;
    }

    private static Image loadImage( IPath location )
    {
        if ( location.toFile().exists() )
        {
            Image image = new Image( Display.getDefault(), location.toString() );
            Rectangle r = image.getBounds();
            int imageMax = Math.max( r.width, r.height );

            if ( imageMax <= IMAGE_SIZE / 2 || imageMax > IMAGE_SIZE )
            {
                // if the image is too large or too small, scale it

                int w = r.width * IMAGE_SIZE / imageMax;
                int h = r.height * IMAGE_SIZE / imageMax;
                Image destination = new Image( image.getDevice(), w, h );
                GC gc = new GC( destination );
                gc.drawImage( image, 0, 0, r.width, r.height, 0, 0, w, h );
                gc.dispose();
                image.dispose();
                image = destination;
            }

            return image;
        }
        return null;
    }

    public static void saveWorkspaceImage( IPath location, Image image, IProgressMonitor monitor )
        throws CoreException
    {
        IFile imageFile =
            ResourcesPlugin.getWorkspace().getRoot().getFileForLocation( location.append( IS2Project.PROJECT_ICON_FILENAME ) );

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        ImageLoader imageLoader = new ImageLoader();
        imageLoader.data = new ImageData[] { image.getImageData() };
        imageLoader.save( buffer, SWT.IMAGE_PNG );

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( buffer.toByteArray() );
        if ( imageFile.exists() )
        {
            imageFile.setContents( byteArrayInputStream, false, true, monitor );
        }
        else
        {
            imageFile.create( byteArrayInputStream, true, monitor );
        }
    }
}
