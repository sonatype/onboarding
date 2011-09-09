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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.editor.xml.MvnIndexPlugin;
import org.osgi.service.prefs.BackingStoreException;

import com.sonatype.s2.project.core.internal.S2ProjectPlugin;

public class M2EclipsePreferenceGroup
    extends AbstractPreferenceGroup
{
    public static final String M2E_ID = IMavenConstants.PLUGIN_ID;

    public static final String M2E_XML_ID = MvnIndexPlugin.PLUGIN_ID;

    private static final String S2_ID = S2ProjectPlugin.PLUGIN_ID;

    private static final String PREFS_ARCHETYPES_FILE = M2E_ID + "/" + MavenPlugin.PREFS_ARCHETYPES; //$NON-NLS-1$

    @Override
    protected IPreferenceVisitor newVisitor()
    {
        return new Visitor();
    }

    static class Visitor
        implements IPreferenceVisitor
    {
        public boolean visit( IPreferenceHandler handler, IEclipsePreferences node, String path )
            throws BackingStoreException
        {
            if ( !path.startsWith( M2E_ID ) && !path.startsWith( S2_ID ) && !path.startsWith( M2E_XML_ID ) )
            {
                return false;
            }

            Collection<String> keys = new ArrayList<String>();
            for ( String key : node.keys() )
            {
                if ( !key.startsWith( "eclipse.m2.globalSettingsFile" )
                    && !key.startsWith( "eclipse.m2.userSettingsFile" ) && !key.startsWith( "eclipse.m2.runtimes" )
                    && !key.startsWith( "eclipse.m2.defaultRuntime" ) && !key.startsWith( "eclipse.m2.jiraUsername" )
                    && !key.startsWith( "eclipse.m2.jiraPassword" ) )
                {
                    keys.add( key );
                }
            }

            handler.handle( node, keys );

            return true;
        }

    }

    @Override
    public String[] getFiles( File preferencesDirectory )
    {
        return new String[] { PREFS_ARCHETYPES_FILE };
    }

    public void notifyFileImported( String file )
    {
        if ( PREFS_ARCHETYPES_FILE.equals( file ) )
        {
            try
            {
                MavenPlugin.getDefault().getArchetypeManager().readCatalogs();
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Error loading archetype catalogs", e );
            }
        }
    }
}
