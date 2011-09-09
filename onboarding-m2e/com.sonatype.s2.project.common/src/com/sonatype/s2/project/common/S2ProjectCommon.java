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
package com.sonatype.s2.project.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.IUrlLocation;
import com.sonatype.s2.project.model.S2ProjectFacade;

public class S2ProjectCommon
{
    public static final String PLUGIN_ID = "com.sonatype.s2.project.common";
    
    private final static Logger log = LoggerFactory.getLogger( S2ProjectCommon.class );
    
    public static IS2Project loadProject( InputStream is, boolean validate )
        throws CoreException, IOException
    {
        IS2Project s2Project = S2ProjectFacade.loadProject( is, validate );

        log.debug( "Loaded s2 project: {}", s2Project.getName() );
        return s2Project;
    }

    public static Map<String, Set<String>> getURLsBySecurityRealmIds( IS2Project s2Project )
        throws CoreException
    {
        Map<String, Set<String>> urlsByRealm = new LinkedHashMap<String, Set<String>>();
        for ( IS2Module module : s2Project.getModules() )
        {
            checkAndAddRealmForURLLocation( module.getScmLocation(), urlsByRealm );
        }

        checkAndAddRealmForURLLocation( s2Project.getP2LineupLocation(), urlsByRealm );

        checkAndAddRealmForURLLocation( s2Project.getMavenSettingsLocation(), urlsByRealm );

        checkAndAddRealmForURLLocation( s2Project.getEclipsePreferencesLocation(), urlsByRealm );

        return urlsByRealm;
    }

    private static void checkAndAddRealmForURLLocation( IUrlLocation urlLocation,
                                                        Map<String, Set<String>> urlsByRealm )
        throws CoreException
    {
        if ( urlLocation == null )
        {
            return;
        }

        String realmId;
        IAuthRealm realm = AuthFacade.getAuthRegistry().getRealmForURI( urlLocation.getUrl() );
        if ( realm != null )
        {
            realmId = realm.getId();
        }
        else
        {
            // Use the location url as realm id
            realmId = urlLocation.getUrl();
        }

        Set<String> urls = urlsByRealm.get( realmId );
        if ( urls == null )
        {
            urls = new LinkedHashSet<String>();
            urlsByRealm.put( realmId, urls );
        }
        urls.add( urlLocation.getUrl() );
    }
}
