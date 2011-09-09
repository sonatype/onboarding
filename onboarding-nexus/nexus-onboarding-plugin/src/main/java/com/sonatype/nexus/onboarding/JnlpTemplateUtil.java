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
package com.sonatype.nexus.onboarding;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.restlet.data.MediaType;

public class JnlpTemplateUtil
{
    public static final String JNLP_MIME_TYPE = MediaType.APPLICATION_JNLP.getName();
    
    public static String processJnlpTemplate( String templatePath, Map<String, String> interpolationProperties ) throws IOException
    {
        InputStream templateInputStream =
            new BufferedInputStream( JnlpTemplateUtil.class.getClassLoader().getResourceAsStream( templatePath ) );
        return processJnlpTemplate( templateInputStream, interpolationProperties );
    }

    public static String processJnlpTemplate( InputStream templateInputStream,
                                              Map<String, String> interpolationProperties )
        throws IOException
    {
        try
        {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();

            InputStreamReader reader = new InputStreamReader( templateInputStream );
            try
            {
                InterpolationFilterReader interpolationFilterReader =
                    new InterpolationFilterReader( reader, interpolationProperties );
                IOUtil.copy( interpolationFilterReader, new OutputStreamWriter( buf, reader.getEncoding() ) );
                
                return buf.toString( reader.getEncoding() );
            }
            finally
            {
                IOUtil.close( reader );
            }

        }
        finally
        {
            IOUtil.close( templateInputStream );
        }
    }
}
