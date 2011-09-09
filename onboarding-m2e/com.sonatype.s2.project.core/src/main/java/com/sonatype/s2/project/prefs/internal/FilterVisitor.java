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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.PreferenceFilterEntry;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FilterVisitor
    extends AbstractPreferenceNodeVisitor
    implements IPreferenceHandler
{

    private final Logger log = LoggerFactory.getLogger( FilterVisitor.class );

    private final IPreferenceVisitor visitor;

    private final String scope;

    private final Map<String, PreferenceFilterEntry[]> mapping;

    public FilterVisitor( String basePath, IPreferenceVisitor visitor )
    {
        super( basePath );
        this.visitor = visitor;

        mapping = new HashMap<String, PreferenceFilterEntry[]>();
        if ( basePath.startsWith( '/' + InstanceScope.SCOPE ) )
        {
            scope = InstanceScope.SCOPE;
        }
        else if ( basePath.startsWith( '/' + ConfigurationScope.SCOPE ) )
        {
            scope = ConfigurationScope.SCOPE;
        }
        else if ( basePath.startsWith( '/' + ProjectScope.SCOPE ) )
        {
            scope = ProjectScope.SCOPE;
        }
        else
        {
            scope = DefaultScope.SCOPE;
        }
    }

    public IPreferenceFilter getFilter()
    {
        return new IPreferenceFilter()
        {

            public String[] getScopes()
            {
                return new String[] { scope };
            }

            @SuppressWarnings( "unchecked" )
            public Map getMapping( String scope )
            {
                return mapping;
            }

        };
    }

    @Override
    protected boolean visit( IEclipsePreferences node, String path )
        throws BackingStoreException
    {
        return visitor.visit( this, node, path );
    }

    public void handle( IEclipsePreferences node, Collection<String> keys )
    {
        log.debug( "Filtering keys {} from preference node {}", keys, node );

        String nodePath = node.absolutePath();
        nodePath = nodePath.substring( nodePath.indexOf( '/', 1 ) + 1 );

        Collection<PreferenceFilterEntry> entries = new ArrayList<PreferenceFilterEntry>();
        for ( String key : keys )
        {
            entries.add( new PreferenceFilterEntry( key ) );
        }

        mapping.put( nodePath, entries.toArray( new PreferenceFilterEntry[entries.size()] ) );
    }

}
