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

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.osgi.service.prefs.BackingStoreException;

class ProxyPreferenceGroup
    extends AbstractPreferenceGroup
{

    @Override
    protected IPreferenceVisitor newVisitor()
    {
        return new Visitor();
    }

    @Override
    public IPreferenceFilter getFilter( String projectName, IEclipsePreferences rootNode )
        throws BackingStoreException
    {
        String basePath;
        if ( projectName == null || projectName.length() <= 0 )
        {
            basePath = '/' + ConfigurationScope.SCOPE;
        }
        else
        {
            basePath = '/' + ProjectScope.SCOPE + '/' + projectName;
        }
        FilterVisitor visitor = new FilterVisitor( basePath, newVisitor() );
        rootNode.accept( visitor );
        return visitor.getFilter();
    }

    static class Visitor
        implements IPreferenceVisitor
    {

        public boolean visit( IPreferenceHandler handler, IEclipsePreferences node, String path )
            throws BackingStoreException
        {
            if ( !path.startsWith( "org.eclipse.core.net" ) )
            {
                return false;
            }

            Collection<String> keys = new ArrayList<String>();
            for ( String key : node.keys() )
            {
                keys.add( key );
            }

            handler.handle( node, keys );

            return true;
        }

    }

}
