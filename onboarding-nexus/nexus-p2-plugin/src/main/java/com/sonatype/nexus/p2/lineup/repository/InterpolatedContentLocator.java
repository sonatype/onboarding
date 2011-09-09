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
package com.sonatype.nexus.p2.lineup.repository;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.sonatype.nexus.proxy.item.ContentLocator;

/**
 * Interpolates the original content while streaming.
 * 
 * @author bdemers
 */
public class InterpolatedContentLocator
    implements ContentLocator
{

    private Map<String, String> interpolatedKeyValues;

    private ContentLocator contentLocator;

    public InterpolatedContentLocator( ContentLocator contentLocator, Map<String, String> interpolatedKeyValues )
    {
        this.contentLocator = contentLocator;
        this.interpolatedKeyValues = interpolatedKeyValues;
    }

    public InputStream getContent()
        throws IOException
    {
        // buffer and interpolate the stream.
        return new InterpolatingInputStream( new BufferedInputStream( this.contentLocator.getContent() ),
                                             this.interpolatedKeyValues );
        // return new ReaderInputStream(
        // new InterpolationFilterReader(
        // new BufferedReader(
        // new InputStreamReader(
        // contentLocator.getContent() ) ),
        // this.interpolatedKeyValues ) );
    }

    public String getMimeType()
    {
        return this.contentLocator.getMimeType();
    }

    public boolean isReusable()
    {
        // we are reusable, since getContent() always creates new input stream
        // so, our reusability actually depends on the wrapper locator.
        // if it is not reusable, we cannot execute getContet() multiple times either
        return this.contentLocator.isReusable();
    }

}
