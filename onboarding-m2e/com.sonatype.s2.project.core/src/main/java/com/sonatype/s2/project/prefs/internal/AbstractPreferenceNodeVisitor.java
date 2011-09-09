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
package com.sonatype.s2.project.prefs.internal;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.osgi.service.prefs.BackingStoreException;

abstract class AbstractPreferenceNodeVisitor
    implements IPreferenceNodeVisitor
{

    private final String basePath;

    public AbstractPreferenceNodeVisitor( String basePath )
    {
        this.basePath = basePath;
    }

    public boolean visit( IEclipsePreferences node )
        throws BackingStoreException
    {
        String absPath = node.absolutePath();

        if ( basePath.startsWith( absPath ) )
        {
            return true;
        }
        else if ( !absPath.startsWith( basePath ) )
        {
            return false;
        }

        String relPath = absPath.substring( basePath.length() + 1 );

        return visit( node, relPath );
    }

    protected abstract boolean visit( IEclipsePreferences node, String path )
        throws BackingStoreException;

}
