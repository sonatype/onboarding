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
package com.sonatype.m2e.cvs.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSMessages;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolder;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * Copied from org.eclipse.team.internal.ccvs.core.filesystem.CVSURI with bugfix from 317606 applied
 */
public class CVSURI
{

    private static final String SCHEME = "cvs"; //$NON-NLS-1$

    private final ICVSRepositoryLocation repository;

    private final IPath path;

    private final CVSTag tag;

    private final String revision;

    /**
     * Convert the given URI to a CVSURI. There are two supported formats: the original opaque format and a newer
     * hierarchical format.
     * <ul>
     * <li>cvs://[:]method:user[:password]@host:[port]/root/path#project/path[,tagName]</li>
     * <li>cvs://_method_user[_password]~host_[port]!root!path/project/path[?<version,branch,date,revision>=tagName]</li>
     * </ul>
     * 
     * @param uri the URI
     * @return a CVS URI
     */
    public static CVSURI fromUri( URI uri )
    {
        try
        {
            ICVSRepositoryLocation repository = getRepository( uri );
            if ( repository != null )
            {
                IPath path = new Path( null, uri.getPath() );
                CVSTag tag = getTag( uri );
                String revision = getRevision( uri );
                return new CVSURI( repository, path, tag, revision );
            }
            else
            {
                repository = getOldRepository( uri );
                IPath path = getOldPath( uri );
                CVSTag tag = getOldTag( uri );
                return new CVSURI( repository, path, tag );
            }
        }
        catch ( CVSException e )
        {
            CVSProviderPlugin.log( e );
            throw new IllegalArgumentException( NLS.bind( CVSMessages.CVSURI_InvalidURI, new String[] { uri.toString(),
                e.getMessage() } ) );
        }
    }

    private static CVSTag getTag( URI uri )
    {
        String query = uri.getQuery();
        if ( query == null )
            return null;
        StringTokenizer tokens = new StringTokenizer( query, "," ); //$NON-NLS-1$
        while ( tokens.hasMoreTokens() )
        {
            String token = tokens.nextToken();
            int index = token.indexOf( '=' );
            if ( index != -1 )
            {
                String type = token.substring( 0, index );
                String value = token.substring( index + 1 );
                if ( value.length() > 0 )
                {
                    int tagType = getTagType( type );
                    if ( tagType != -1 )
                        return new CVSTag( value, tagType );
                }
            }
        }
        return null;
    }

    private static String getRevision( URI uri )
    {
        String query = uri.getQuery();
        if ( query == null )
            return null;
        StringTokenizer tokens = new StringTokenizer( query, "," ); //$NON-NLS-1$
        while ( tokens.hasMoreTokens() )
        {
            String token = tokens.nextToken();
            int index = token.indexOf( '=' );
            if ( index != -1 )
            {
                String type = token.substring( 0, index );
                String value = token.substring( index + 1 );
                if ( type.equals( "revision" ) && isValidRevision( value ) ) { //$NON-NLS-1$
                    return value;
                }
            }
        }
        return null;
    }

    private static boolean isValidRevision( String value )
    {
        return value.matches( "\\d+\\.\\d+(?:\\.\\d+)*" ); //$NON-NLS-1$
    }

    private static int getTagType( String type )
    {
        if ( type.equalsIgnoreCase( "version" ) ) //$NON-NLS-1$
            return CVSTag.VERSION;
        if ( type.equalsIgnoreCase( "branch" ) ) //$NON-NLS-1$
            return CVSTag.BRANCH;
        if ( type.equalsIgnoreCase( "date" ) ) //$NON-NLS-1$
            return CVSTag.DATE;
        return -1;
    }

    private static ICVSRepositoryLocation getRepository( URI uri )
        throws CVSException
    {
        String authority = uri.getAuthority();
        if ( authority.indexOf( '/' ) != -1 )
            return null;
        if ( authority.indexOf( '!' ) == -1 )
            return null;
        authority = decodeAuthority( authority );
        return CVSRepositoryLocation.fromString( authority );
    }

    private static CVSTag getOldTag( URI uri )
    {
        String f = uri.getFragment();
        int i = f.indexOf( ',' );
        if ( i == -1 )
        {
            return CVSTag.DEFAULT;
        }

        return CVSTag.DEFAULT;// just use HEAD for now (name, CVSTag.BRANCH);
    }

    private static IPath getOldPath( URI uri )
    {
        String path = uri.getFragment();
        int i = path.indexOf( ',' );
        if ( i != -1 )
        {
            path = path.substring( 0, i );
        }
        return new Path( path );
    }

    private static ICVSRepositoryLocation getOldRepository( URI uri )
        throws CVSException
    {
        String ssp = uri.getSchemeSpecificPart();
        if ( ssp.startsWith( "//" ) ) //$NON-NLS-1$
            ssp = ssp.substring( 2 );
        if ( !ssp.startsWith( ":" ) ) { //$NON-NLS-1$
            ssp = ":" + ssp; //$NON-NLS-1$
        }
        return CVSRepositoryLocation.fromString( ssp );
    }

    public CVSURI( ICVSRepositoryLocation repository, IPath path, CVSTag tag )
    {
        this( repository, path, tag, null );
    }

    public CVSURI( ICVSRepositoryLocation repository, IPath path, CVSTag tag, String revision )
    {
        this.repository = repository;
        this.path = path;
        this.tag = tag;
        if ( revision != null && !revision.equals( ResourceSyncInfo.ADDED_REVISION ) )
            this.revision = revision;
        else
            this.revision = null;
    }

    public CVSURI append( String name )
    {
        return new CVSURI( repository, path.append( name ), tag );
    }

    public CVSURI append( IPath childPath )
    {
        return new CVSURI( repository, path.append( childPath ), tag );
    }

    public String getLastSegment()
    {
        return path.lastSegment();
    }

    public URI toURI()
    {
        try
        {
            String authority = repository.getLocation( false );
            authority = ensureRegistryBasedAuthority( authority );
            String pathString = path.toString();
            if ( !pathString.startsWith( "/" ) ) { //$NON-NLS-1$
                pathString = "/" + pathString; //$NON-NLS-1$
            }
            String query = null;
            if ( tag != null && tag.getType() != CVSTag.HEAD )
            {
                query = getQueryType( tag ) + "=" + tag.getName(); //$NON-NLS-1$
            }
            if ( revision != null )
            {
                String string = "revision=" + revision; //$NON-NLS-1$
                if ( query == null )
                {
                    query = string;
                }
                else
                {
                    query = query + "," + string; //$NON-NLS-1$
                }
            }
            return new URI( SCHEME, authority, pathString, query, null );
        }
        catch ( URISyntaxException e )
        {
            CVSProviderPlugin.log( IStatus.ERROR,
                                   NLS.bind( "An error occurred while creating a URI for {0} {1}", repository, path ), e ); //$NON-NLS-1$
            throw new IllegalStateException( e.getMessage() );
        }
    }

    /*
     * Ensure that the authority will not be confused with a server based authority. To do this, we need to convert any
     * /, : and @ to another form.
     */
    private String ensureRegistryBasedAuthority( String authority )
    {
        // Encode / so the authority doesn't conflict with the path
        authority = encode( '/', '!', authority );
        // Encode @ to avoid URI interpreting the authority as a server based authority
        authority = encode( '@', '~', authority );
        // Encode : to avoid URI interpreting the authority as a server based authority
        authority = encode( ':', '_', authority );
        return authority;
    }

    private static String decodeAuthority( String authority )
    {
        authority = decode( '/', '!', authority );
        authority = decode( '@', '~', authority );
        authority = decode( ':', '_', authority );
        return authority;
    }

    private String encode( char charToEncode, char encoding, String string )
    {
        // First, escape any occurrences of the encoding character
        String result =
            string.replaceAll( new String( new char[] { encoding } ), new String( new char[] { encoding, encoding } ) );
        // Convert / to ! to avoid URI parsing part of the authority as the path
        return result.replace( charToEncode, encoding );
    }

    private static String decode( char encodedChar, char encoding, String string )
    {
        // Convert the encoded char back
        String reuslt = string.replace( encoding, encodedChar );
        // Convert any double occurrences of the encoded char back to the encoding
        return reuslt.replaceAll( new String( new char[] { encodedChar, encodedChar } ),
                                  new String( new char[] { encoding } ) );
    }

    private static String getQueryType( CVSTag tag )
    {
        switch ( tag.getType() )
        {
            case CVSTag.BRANCH:
                return "branch"; //$NON-NLS-1$
            case CVSTag.DATE:
                return "date"; //$NON-NLS-1$
        }
        return "version"; //$NON-NLS-1$
    }

    public boolean isRepositoryRoot()
    {
        return path.segmentCount() == 0;
    }

    public CVSURI removeLastSegment()
    {
        return new CVSURI( repository, path.removeLastSegments( 1 ), tag );
    }

    public ICVSRemoteFolder getParentFolder()
    {
        return removeLastSegment().toFolder();
    }

    public String getRepositoryName()
    {
        return repository.toString();
    }

    public CVSURI getProjectURI()
    {
        return new CVSURI( repository, path.uptoSegment( 1 ), tag );
    }

    public ICVSRemoteFolder toFolder()
    {
        return new RemoteFolder( null, repository, path.toString(), tag );
    }

    public ICVSRemoteFile toFile()
    {
        // TODO: What about keyword mode?
        return RemoteFile.create( path.toString(), repository, tag, revision );
    }

    public String toString()
    {
        return "[Path: " + this.path.toString() + " Tag: " + tag.getName() + " Repo: " + repository.getRootDirectory() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    public IPath getPath()
    {
        return path;
    }

    public IPath getProjectStrippedPath()
    {
        if ( path.segmentCount() > 1 )
            return path.removeFirstSegments( 1 );

        return path;
    }

    public ICVSRepositoryLocation getRepository()
    {
        return repository;
    }

    public CVSTag getTag()
    {
        return tag;
    }

    public String getRevision()
    {
        return revision;
    }
}
