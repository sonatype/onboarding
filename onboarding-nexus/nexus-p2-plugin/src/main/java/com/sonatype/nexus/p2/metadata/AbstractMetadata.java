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

import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public abstract class AbstractMetadata
{
    protected final Xpp3Dom dom;

    protected AbstractMetadata( Xpp3Dom dom )
    {
        this.dom = dom;
    }

    protected AbstractMetadata( AbstractMetadata other )
    {
        this.dom = new Xpp3Dom( other.dom );
    }

    public Xpp3Dom getDom()
    {
        return dom;
    }

    public static void removeChild( Xpp3Dom dom, String name )
    {
        Xpp3Dom[] children = dom.getChildren();
        for ( int i = 0; i < children.length; )
        {
            if ( name.equals( children[i].getName() ) )
            {
                dom.removeChild( i );
                children = dom.getChildren();
            }
            else
            {
                i++;
            }
        }
    }

    public void removeProperty( String name )
    {
        Xpp3Dom properties = dom.getChild( "properties" );

        if ( properties != null )
        {
            Xpp3Dom[] property = properties.getChildren( "property" );

            for ( int i = 0; i < property.length; i++ )
            {
                if ( name.equals( property[i].getAttribute( "name" ) ) )
                {
                    properties.removeChild( i );
                }
            }
        }
    }

    public LinkedHashMap<String, String> getProperties()
    {
        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
        
        Xpp3Dom propertiesDom = dom.getChild( "properties" );
        
        if ( propertiesDom != null )
        {
            for ( Xpp3Dom propertyDom : propertiesDom.getChildren( "property" ) )
            {
                result.put( propertyDom.getAttribute( "name" ), propertyDom.getAttribute( "value" ) );
            }
        }

        return result;
    }

    public void setProperties( LinkedHashMap<String, String> properties )
    {
        removeChild( dom, "properties" );
        
        Xpp3Dom propertiesDom = new Xpp3Dom( "properties" );
        
        for ( Map.Entry<String, String> property : properties.entrySet() )
        {
            Xpp3Dom propertyDom = new Xpp3Dom( "property" );
            propertyDom.setAttribute( "name", property.getKey() );
            propertyDom.setAttribute( "value", property.getValue() );
            propertiesDom.addChild( propertyDom );
        }

        propertiesDom.setAttribute( "size", Integer.toString( properties.size() ) );
        dom.addChild( propertiesDom );
    }

}
