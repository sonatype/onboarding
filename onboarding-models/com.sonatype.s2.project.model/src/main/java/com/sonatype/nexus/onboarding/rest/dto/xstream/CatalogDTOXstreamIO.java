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
package com.sonatype.nexus.onboarding.rest.dto.xstream;

import java.io.InputStream;

import com.thoughtworks.xstream.XStream;

public class CatalogDTOXstreamIO
{
    private static XStream newInitializedXstream()
    {
        XStream xs = new XStream();
        XStreamUtil.initializeXStream( xs );
        // use the models classloader
        xs.setClassLoader( CatalogDTOXstreamIO.class.getClassLoader() );
        return xs;
    }

    public static <T> T deserialize( InputStream is, Class<T> clazz )
    {
        XStream xs = newInitializedXstream();
        return clazz.cast( xs.fromXML( is ) );
    }
}
