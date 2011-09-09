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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * NOTE: ideally we should use a Reader, so se don't need to deal with char set issues. (this class assumes UTF-8), however tests with a reader (and wrapping it in an input stream) slow.
 *
 */
public class InterpolatingInputStream
    extends FilterInputStream
{

    /** Character marking the beginning of a token. */
    private String beginToken;

    /** Length of begin token. */
    private int beginTokenLength;

    /** Character marking the end of a token. */
    private String endToken;

    /** Length of end token. */
    private int endTokenLength;

    /** Index into previous data */
    private int previousIndex = -1;

    /** replacement text from a token */
    private String replaceData = null;

    /** Index into replacement data */
    private int replaceIndex = -1;

    /** Hashtable to hold the replacee-replacer pairs (String to String). */
    private final Map<?, ?> variables;

    /** Default begin token. */
    private static String DEFAULT_BEGIN_TOKEN = "${";

    /** Default end token. */
    private static String DEFAULT_END_TOKEN = "}";

    public InterpolatingInputStream( InputStream in, Map<?, ?> variables, String beginToken, String endToken )
    {
        super( in );

        this.variables = variables;
        this.beginToken = beginToken;
        this.endToken = endToken;

        beginTokenLength = beginToken.length();
        endTokenLength = endToken.length();
    }

    public InterpolatingInputStream( InputStream in, Map<?, ?> variables )
    {
        this( in, variables, DEFAULT_BEGIN_TOKEN, DEFAULT_END_TOKEN );
    }

    
    public int read( byte buf[],
                     int off,
                     int len )
        throws IOException
    {
        for ( int i = 0; i < len; i++ )
        {
            int ch = read();
            if ( ch == -1 )
            {
                if ( i == 0 )
                {
                    return -1;
                }
                else
                {
                    return i;
                }
            }
            buf[off + i] = (byte) ch;
        }
        return len;
    }
    
    @Override
    public int read()
        throws IOException
    {
        if ( replaceIndex != -1 && replaceIndex < replaceData.length() )
        {
            int ch = replaceData.charAt( replaceIndex++ );
            if ( replaceIndex >= replaceData.length() )
            {
                replaceIndex = -1;
            }
            return ch;
        }

        int ch = -1;
        if ( previousIndex != -1 && previousIndex < endTokenLength )
        {
            ch = endToken.charAt( previousIndex++ );
        }
        else
        {
            ch = in.read();
        }

        if ( ch == beginToken.charAt( 0 ) )
        {
            StringBuffer key = new StringBuffer();

            int beginTokenMatchPos = 1;

            do
            {
                if ( previousIndex != -1 && previousIndex < endTokenLength )
                {
                    ch = endToken.charAt( previousIndex++ );
                }
                else
                {
                    ch = in.read();
                }
                if ( ch != -1 )
                {
                    key.append( (char) ch );

                    if ( ( beginTokenMatchPos < beginTokenLength )
                        && ( ch != beginToken.charAt( beginTokenMatchPos++ ) ) )
                    {
                        ch = -1; // not really EOF but to trigger code below
                        break;
                    }
                }
                else
                {
                    break;
                }
            }
            while ( ch != endToken.charAt( 0 ) );

            // now test endToken
            if ( ch != -1 && endTokenLength > 1 )
            {
                int endTokenMatchPos = 1;

                do
                {
                    if ( previousIndex != -1 && previousIndex < endTokenLength )
                    {
                        ch = endToken.charAt( previousIndex++ );
                    }
                    else
                    {
                        ch = in.read();
                    }

                    if ( ch != -1 )
                    {
                        key.append( (char) ch );

                        if ( ch != endToken.charAt( endTokenMatchPos++ ) )
                        {
                            ch = -1; // not really EOF but to trigger code below
                            break;
                        }

                    }
                    else
                    {
                        break;
                    }
                }
                while ( endTokenMatchPos < endTokenLength );
            }

            // There is nothing left to read so we have the situation where the begin/end token
            // are in fact the same and as there is nothing left to read we have got ourselves
            // end of a token boundary so let it pass through.
            if ( ch == -1 )
            {
                replaceData = key.toString();
                replaceIndex = 0;
                return beginToken.charAt( 0 );
            }

            String variableKey = key.substring( beginTokenLength - 1, key.length() - endTokenLength );

            Object o = variables.get( variableKey );
            if ( o != null )
            {
                String value = o.toString();
                if ( value.length() != 0 )
                {
                    replaceData = value;
                    replaceIndex = 0;
                }
                return read();
            }
            else
            {
                previousIndex = 0;
                replaceData = key.substring( 0, key.length() - endTokenLength );
                replaceIndex = 0;
                return beginToken.charAt( 0 );
            }
        }

        return ch;
    }
}
