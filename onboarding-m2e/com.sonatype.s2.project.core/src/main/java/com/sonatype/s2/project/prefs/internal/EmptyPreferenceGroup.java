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

import java.io.File;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.osgi.service.prefs.BackingStoreException;

class EmptyPreferenceGroup
    implements IPreferenceGroup
{

    public boolean isAvailable( IEclipsePreferences rootNode )
        throws BackingStoreException
    {
        return false;
    }

    public IPreferenceFilter getFilter( String projectName, IEclipsePreferences rootNode )
        throws BackingStoreException
    {
        return new IPreferenceFilter()
        {

            public String[] getScopes()
            {
                return new String[0];
            }

            @SuppressWarnings( "unchecked" )
            public Map getMapping( String scope )
            {
                return null;
            }

        };
    }

    public String[] getFiles( File preferencesDirectory )
    {
        return new String[0];
    }

    public void resetPreferences( IEclipsePreferences rootNode )
        throws BackingStoreException
    {
    }

    public void notifyFileImported( String file )
    {
    }
}
