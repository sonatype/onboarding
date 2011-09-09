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

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

class JdtPreferenceGroup
    extends AbstractPreferenceGroup
{

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
            if ( !path.startsWith( "org.eclipse.jdt.core" ) && !path.startsWith( "org.eclipse.jdt.ui" ) )
            {
                return false;
            }

            Collection<String> keys = new ArrayList<String>();
            for ( String key : node.keys() )
            {
                if ( !key.startsWith( "org.eclipse.jdt.core.classpathVariable" ) )
                {
                    keys.add( key );
                }
            }

            handler.handle( node, keys );

            return true;
        }

    }

}
