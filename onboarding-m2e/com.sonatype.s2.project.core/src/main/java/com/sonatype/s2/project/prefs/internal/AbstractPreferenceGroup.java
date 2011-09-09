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

import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

abstract class AbstractPreferenceGroup
    implements IPreferenceGroup
{

    public boolean isAvailable( IEclipsePreferences rootNode )
        throws BackingStoreException
    {
        IPreferenceVisitor prefVisitor = newVisitor();

        AvailabilityVisitor visitor = new AvailabilityVisitor( '/' + InstanceScope.SCOPE, prefVisitor );
        rootNode.accept( visitor );
        if ( visitor.isMatched() )
        {
            return true;
        }

        visitor = new AvailabilityVisitor( '/' + ConfigurationScope.SCOPE, prefVisitor );
        rootNode.accept( visitor );
        if ( visitor.isMatched() )
        {
            return true;
        }

        visitor = new AvailabilityVisitor( '/' + DefaultScope.SCOPE, prefVisitor );
        rootNode.accept( visitor );
        return visitor.isMatched();
    }

    public IPreferenceFilter getFilter( String projectName, IEclipsePreferences rootNode )
        throws BackingStoreException
    {
        String basePath;
        if ( projectName == null || projectName.length() <= 0 )
        {
            basePath = '/' + InstanceScope.SCOPE;
        }
        else
        {
            basePath = '/' + ProjectScope.SCOPE + '/' + projectName;
        }
        FilterVisitor visitor = new FilterVisitor( basePath, newVisitor() );
        rootNode.accept( visitor );
        return visitor.getFilter();
    }

    public String[] getFiles( File preferencesDirectory )
    {
        return new String[0];
    }

    public void resetPreferences( IEclipsePreferences rootNode )
        throws BackingStoreException
    {
        String basePath = '/' + InstanceScope.SCOPE;
        ResetVisitor visitor = new ResetVisitor( basePath, newVisitor() );
        rootNode.accept( visitor );
    }

    protected abstract IPreferenceVisitor newVisitor();

    public void notifyFileImported( String file )
    {
        // Do nothing by default
    }
}
