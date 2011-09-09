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
package com.sonatype.s2.project.validator.p2;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2AuthHelper
{
    private static Logger log = LoggerFactory.getLogger( P2AuthHelper.class );

    public static void removeCredentials( URI location )
    {
        log.debug( "Removing credentials for URI='{}'", location );
        String nodeName = getNodeName( location );

        ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
        ISecurePreferences prefNode = securePreferences.node( nodeName );
        prefNode.removeNode();
    }

    public static boolean hasCredentialsForURI( URI location )
    {
        String nodeName = getNodeName( location );
        ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
        if ( !securePreferences.nodeExists( nodeName ) )
        {
            log.debug( "No credentials for URI='{}'", location );
            return false;
        }

        ISecurePreferences prefNode = securePreferences.node( nodeName );
        try
        {
            if ( prefNode.get( IRepository.PROP_USERNAME, null ) != null
                || prefNode.get( IRepository.PROP_PASSWORD, null ) != null )
            {
                log.debug( "Credentials for URI='{}': username={}", location, prefNode.get( IRepository.PROP_USERNAME,
                                                                                            null ) );
                return true;
            }
        }
        catch ( StorageException e )
        {
            throw new RuntimeException( e );
        }

        log.debug( "No credentials for URI='{}'", location );
        return false;
    }

    // copied from org.sonatype.tycho.p2.P2ResolverImpl
    public static void setCredentials( URI location, String username, String password )
    {
        log.debug( "Setting credentials for URI='{}', username={}", location, username );

        String nodeName = getNodeName( location );

        ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
        ISecurePreferences prefNode = securePreferences.node( nodeName );

        try
        {
            if ( !username.equals( prefNode.get( IRepository.PROP_USERNAME, username ) )
                || !password.equals( prefNode.get( IRepository.PROP_PASSWORD, password ) ) )
            {
                log.warn( "Redefining access credentials for repository node {}", nodeName );
            }
            prefNode.put( IRepository.PROP_USERNAME, username, false );
            prefNode.put( IRepository.PROP_PASSWORD, password, false );
        }
        catch ( StorageException e )
        {
            throw new RuntimeException( e );
        }
    }

    private static String getNodeName( URI location )
    {
        // if URI is not opaque, just getting the host may be enough
        String host = location.getHost();
        if ( host == null )
        {
            String scheme = location.getScheme();
            if ( URIUtil.isFileURI( location ) || scheme == null )
            {
                // If the URI references a file, a password could possibly be needed for the directory
                // (it could be a protected zip file representing a compressed directory) - in this
                // case the key is the path without the last segment.
                // Using "Path" this way may result in an empty string - which later will result in
                // an invalid key.
                host = new Path( location.toString() ).removeLastSegments( 1 ).toString();
            }
            else
            {
                // it is an opaque URI - details are unknown - can only use entire string.
                host = location.toString();
            }
        }
        String nodeKey;
        try
        {
            nodeKey = URLEncoder.encode( host, "UTF-8" ); //$NON-NLS-1$
        }
        catch ( UnsupportedEncodingException e2 )
        {
            // fall back to default platform encoding
            try
            {
                // Uses getProperty "file.encoding" instead of using deprecated URLEncoder.encode(String location)
                // which does the same, but throws NPE on missing property.
                String enc = System.getProperty( "file.encoding" );//$NON-NLS-1$
                if ( enc == null )
                {
                    throw new UnsupportedEncodingException(
                                                            "No UTF-8 encoding and missing system property: file.encoding" ); //$NON-NLS-1$
                }
                nodeKey = URLEncoder.encode( host, enc );
            }
            catch ( UnsupportedEncodingException e )
            {
                throw new RuntimeException( e );
            }
        }
        return IRepository.PREFERENCE_NODE + '/' + nodeKey;
    }
}
