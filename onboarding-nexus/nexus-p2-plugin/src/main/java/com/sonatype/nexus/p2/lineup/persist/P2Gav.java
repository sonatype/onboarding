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
package com.sonatype.nexus.p2.lineup.persist;

import org.codehaus.plexus.util.StringUtils;

import com.sonatype.s2.p2lineup.model.IP2Lineup;

public class P2Gav
{
    private String id;

    private String groupId;

    private String version;

    public P2Gav( IP2Lineup lineup )
    {
        this( lineup.getGroupId(), lineup.getId(), lineup.getVersion() );
    }

    public P2Gav( String groupId, String id, String version )
    {   
        this.id = id;
        this.groupId = groupId;
        this.version = version;
    }

    public P2Gav( String gav )
        throws InvalidP2GavException
    {
        gav = this.removeTrailingSlash( gav );
        
        String[] parts = gav.split( "/" );
        if ( parts.length < 3 )
        {
            throw new InvalidP2GavException( "P2 Gav '" + gav + "' is invalid, must be in the format of group/artifact/version" );
        }
        
        this.version = parts[parts.length-1];
        this.id = parts[parts.length-2];
        StringBuffer groupBuffer = new StringBuffer();
        for ( int ii = 0; ii < parts.length-2; ii++ )
        {
            if( parts[ii].length() > 0)
            {
                groupBuffer.append( parts[ii] );
                
                if( ii != parts.length-3)
                {
                    groupBuffer.append( "." );
                }
            }
        }
        this.groupId = groupBuffer.toString();
        
        // simple m2 like gav (don't want to use the m2 stuff as that is getting removed from the core, and P2 should
        // not reference it
        
        if( StringUtils.isEmpty( this.groupId ))
        {
            throw new InvalidP2GavException( "P2 Gav '" + gav + "' is invalid, must be in the format of group/artifact/version" );
        }
    }

    private String removeTrailingSlash( String string )
    {
        if ( string.endsWith( "/" ) )
        {
            return this.removeTrailingSlash( string.substring( 0, string.length() - 1 ) );
        }
        return string;

    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }
    
    
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( groupId == null ) ? 0 : groupId.hashCode() );
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        result = prime * result + ( ( version == null ) ? 0 : version.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        P2Gav other = (P2Gav) obj;
        if ( groupId == null )
        {
            if ( other.groupId != null )
                return false;
        }
        else if ( !groupId.equals( other.groupId ) )
            return false;
        if ( id == null )
        {
            if ( other.id != null )
                return false;
        }
        else if ( !id.equals( other.id ) )
            return false;
        if ( version == null )
        {
            if ( other.version != null )
                return false;
        }
        else if ( !version.equals( other.version ) )
            return false;
        return true;
    }

    public String toString()
    {
        return groupId + ":" + id +":" + version;
    }
    
    public String toPathString()
    {
        String tmpGroupId = (this.groupId != null) ? this.groupId : ""; 
        return tmpGroupId.replace( ".", "/" ) + "/" + this.id +"/" + this.version;
    }

}
