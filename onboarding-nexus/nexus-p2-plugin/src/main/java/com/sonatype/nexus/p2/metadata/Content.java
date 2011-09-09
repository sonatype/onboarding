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
package com.sonatype.nexus.p2.metadata;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class Content
    extends AbstractMetadata
{
    public Content( Xpp3Dom dom )
    {
        super( dom );
    }

    public Content( String name )
    {
        super( new Xpp3Dom( "repository" ) );
        setRepositoryAttributes( name );
    }

    public void setRepositoryAttributes( String name )
    {
        getDom().setAttribute( "name", name );
        getDom().setAttribute( "type", "org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository" );
        getDom().setAttribute( "version", "1" );
    }

    public static class Unit
        extends AbstractMetadata
    {

        protected Unit( Xpp3Dom dom )
        {
            super( dom );
        }

        public Unit( Unit other )
        {
            super( other );
        }

        public String getId()
        {
            return dom.getAttribute( "id" );
        }

        public String getVersion()
        {
            return dom.getAttribute( "version" );
        }
    }

    public void removeReferences()
    {
        Xpp3Dom[] children = dom.getChildren();

        for ( int i = 0; i < children.length; i++ )
        {
            if ( "references".equals( children[i].getName() ) )
            {
                dom.removeChild( i );
            }
        }
    }

    public List<Unit> getUnits()
    {
        Xpp3Dom unitsDom = dom.getChild( "units" );

        return getUnits( unitsDom );
    }

    public static List<Unit> getUnits( Xpp3Dom unitsDom )
    {
        List<Unit> result = new ArrayList<Unit>();

        if ( unitsDom != null )
        {
            for ( Xpp3Dom unitDom : unitsDom.getChildren( "unit" ) )
            {
                result.add( new Unit( unitDom ) );
            }
        }

        return result;
    }

    public void setUnits( List<Unit> units )
    {
        removeChild( dom, "units" );
        Xpp3Dom unitsDom = new Xpp3Dom( "units" );

        for ( Unit unit : units )
        {
            unitsDom.addChild( unit.getDom() );
        }
        unitsDom.setAttribute( "size", Integer.toString( units.size() ) );

        dom.addChild( unitsDom );
    }

}
