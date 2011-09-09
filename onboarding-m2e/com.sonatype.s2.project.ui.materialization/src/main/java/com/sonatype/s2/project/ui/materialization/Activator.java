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
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator
    extends AbstractUIPlugin
{

    // The plug-in ID
    public static final String PLUGIN_ID = "com.sonatype.s2.project.ui.materialization";

    public static final String IMAGE_PATH = "icons/";

    // The shared instance
    private static Activator plugin;

    /**
     * The constructor
     */
    public Activator()
    {
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    public void start( BundleContext context )
        throws Exception
    {
        super.start( context );
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    public void stop( BundleContext context )
        throws Exception
    {
        plugin = null;
        super.stop( context );
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static Activator getDefault()
    {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in relative path
     * 
     * @param path the path
     * @return the image descriptor
     */
    public ImageDescriptor getImageDescriptor( String path )
    {
        ImageRegistry imageRegistry = getImageRegistry();
        if ( imageRegistry != null )
        {
            ImageDescriptor imageDescriptor = imageRegistry.getDescriptor( path );
            if ( imageDescriptor == null )
            {
                imageDescriptor = imageDescriptorFromPlugin( PLUGIN_ID, IMAGE_PATH + path );
                imageRegistry.put( path, imageDescriptor );
            }
            return imageDescriptor;
        }

        return null;
    }

    public Image getImage( String path )
    {
        ImageRegistry imageRegistry = getImageRegistry();
        if ( imageRegistry != null )
        {
            getImageDescriptor( path );
            return imageRegistry.get( path );
        }
        return null;
    }
}
