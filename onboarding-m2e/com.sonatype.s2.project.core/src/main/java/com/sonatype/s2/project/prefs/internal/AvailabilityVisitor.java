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

import java.util.Collection;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AvailabilityVisitor
    extends AbstractPreferenceNodeVisitor
    implements IPreferenceHandler
{

    private final Logger log = LoggerFactory.getLogger( AvailabilityVisitor.class );

    private final IPreferenceVisitor visitor;

    private boolean matched;

    public AvailabilityVisitor( String basePath, IPreferenceVisitor visitor )
    {
        super( basePath );
        this.visitor = visitor;
    }

    public boolean isMatched()
    {
        return matched;
    }

    @Override
    protected boolean visit( IEclipsePreferences node, String path )
        throws BackingStoreException
    {
        return !matched && visitor.visit( this, node, path );
    }

    public void handle( IEclipsePreferences node, Collection<String> keys )
    {
        log.debug( "Detected keys {} from preference node {}", keys, node );

        matched = true;
    }

}
