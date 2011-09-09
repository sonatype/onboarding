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
package com.sonatype.s2.project.core.ide;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IDEUpdater
{
    private static final Logger log = LoggerFactory.getLogger( IDEUpdater.class );

    public static IIDEUpdater getUpdater()
    {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtension extension = registry.getExtension( "com.sonatype.s2.project.core.p2Updater" );
        if ( extension == null )
        {
            log.debug( "No extension found to update the IDE." );
            return null;
        }

        try
        {
            return (IIDEUpdater) extension.getConfigurationElements()[0].createExecutableExtension( "class" );
        }
        catch ( InvalidRegistryObjectException e )
        {
            log.error( "Problem creating the extension for the updater.", e );
            return null;
        }
        catch ( CoreException e )
        {
            log.error( "Problem creating the extension for the updater.", e );
            return null;
        }
    }
}
