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
package com.sonatype.nexus.p2.auth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.sonatype.tycho.p2.facade.internal.P2Logger;

public class P2AuthSession
{
    private static Map<String, Integer> globalNodeRefCounts = new LinkedHashMap<String, Integer>();

    private List<String> nodeNamesToCleanup = new ArrayList<String>();

    public void cleanup()
    {
        synchronized ( globalNodeRefCounts )
        {
            for ( String nodeName : nodeNamesToCleanup )
            {
                decrementNodeRefCount( nodeName );
            }
            nodeNamesToCleanup.clear();
        }
    }

    private static void incrementNodeRefCount( String nodeName )
    {
        Integer refCount = globalNodeRefCounts.get( nodeName );
        if ( refCount == null )
        {
            // We don't "own" this node
            return;
        }

        refCount++;
        globalNodeRefCounts.put( nodeName, refCount );
    }

    private static void decrementNodeRefCount( String nodeName )
    {
        Integer refCount = globalNodeRefCounts.get( nodeName );
        if ( refCount == null )
        {
            // We don't "own" this node
            return;
        }
        if ( refCount == 0 )
        {
            throw new IllegalStateException( "NodeName=" + nodeName + ", ref count = 0" );
        }

        if ( refCount == 1 )
        {
            globalNodeRefCounts.remove( nodeName );

            ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
            ISecurePreferences prefNode = securePreferences.node( nodeName );
            prefNode.removeNode();
            try
            {
                prefNode.flush();
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
            return;
        }
        refCount--;
        globalNodeRefCounts.put( nodeName, refCount );
    }

    public void setCredentials( URI location, String username, String password )
    {
        if ( username == null && password == null )
        {
            return;
        }

        synchronized ( globalNodeRefCounts )
        {
            getLogger().debug( "Setting credentials for URI='" + location + "', username=" + username );

            String nodeName = getNodeName( location );
            ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();

            boolean isNewNode = !securePreferences.nodeExists( nodeName );
            // if ( !isNewNode )
            // {
            // ISecurePreferences prefNode = securePreferences.node( nodeName );
            // prefNode.removeNode();
            // try
            // {
            // prefNode.flush();
            // }
            // catch ( IOException e )
            // {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }
            // isNewNode = !securePreferences.nodeExists( nodeName );
            // }
            getLogger().debug( "isNewNode=" + isNewNode );

            ISecurePreferences prefNode = securePreferences.node( nodeName );

            try
            {
                if ( !isNewNode )
                {
                    if ( !username.equals( prefNode.get( IRepository.PROP_USERNAME, username ) )
                        || !password.equals( prefNode.get( IRepository.PROP_PASSWORD, password ) ) )
                    {
                        getLogger().info( "Redefining access credentials for repository node " + nodeName );
                        throw new RuntimeException( "Cannot redefine credentials for URI=" + location + ", nodeName=" + nodeName );
                    }
                }
                else
                {
                    globalNodeRefCounts.put( nodeName, 0 );
                    prefNode.put( IRepository.PROP_USERNAME, username, false );
                    prefNode.put( IRepository.PROP_PASSWORD, password, false );
                }
                incrementNodeRefCount( nodeName );
                nodeNamesToCleanup.add( nodeName );
            }
            catch ( StorageException e )
            {
                throw new RuntimeException( e );
            }
        }
    }

    public static String getNodeNameForUnitTests( URI location )
    {
        return getNodeName( location );
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

    private P2Logger logger = null;

    private P2Logger getLogger()
    {
        if ( logger != null )
        {
            return logger;
        }

        logger = new P2Logger()
        {
            public void info( String message )
            {
                System.out.println( message );
            }

            public void debug( String message )
            {
                System.out.println( message );
            }
        };
        return logger;
    }
    public void setLogger( P2Logger logger )
    {
        this.logger = logger;
    }

    public static Map<String, Integer> getGlobalNodeRefCountsForUnitTests()
    {
        Map<String, Integer> clone = new LinkedHashMap<String, Integer>();
        clone.putAll( globalNodeRefCounts );
        return clone;
    }

    public List<String> getNodeNamesToCleanupForUnitTests()
    {
        List<String> clone = new ArrayList<String>();
        clone.addAll( nodeNamesToCleanup );
        return clone;
    }
}
