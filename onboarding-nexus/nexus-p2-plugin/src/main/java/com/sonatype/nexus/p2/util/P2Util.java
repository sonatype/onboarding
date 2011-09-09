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
package com.sonatype.nexus.p2.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.sonatype.plugin.metadata.GAVCoordinate;

public class P2Util
{

    private static GAVCoordinate coordinate;

    public static GAVCoordinate getPluginCoordinates()
    {
        if ( coordinate == null )
        {
            Properties props = new Properties();

            InputStream is =
                P2Util.class.getResourceAsStream( "/META-INF/maven/com.sonatype.nexus.plugin/nexus-p2-plugin/pom.properties" );

            if ( is != null )
            {
                try
                {
                    props.load( is );
                }
                catch ( IOException e )
                {
                    throw new RuntimeException( e.getMessage(), e );
                }
            }

            coordinate =
                new GAVCoordinate( "com.sonatype.nexus.plugin", "nexus-p2-plugin", props.getProperty( "version" ) );
        }
        return coordinate;
    }

}
