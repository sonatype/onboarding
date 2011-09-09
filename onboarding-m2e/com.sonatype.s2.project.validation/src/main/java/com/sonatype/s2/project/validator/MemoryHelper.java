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
package com.sonatype.s2.project.validator;

import java.lang.management.ManagementFactory;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MemoryHelper
{

    private final Logger log = LoggerFactory.getLogger( MemoryHelper.class );

    public static final MemoryHelper INSTANCE = new MemoryHelper();

    private long maxMemory = -1;

    private MemoryHelper()
    {
        // hide constructor
    }

    /**
     * Parses a memory specification like "768M" or "1024k" as used for the JVM parameter "-Xmx".
     * 
     * @param mem The memory specification to parse, must not be {@code null}.
     * @return The number of bytes expressed by the memory specification.
     * @throws NumberFormatException If the memory specification could not be parsed.
     */
    public long parseMemSpec( String mem )
        throws NumberFormatException
    {
        long bytes;

        mem = mem.trim();

        if ( mem.endsWith( "m" ) || mem.endsWith( "M" ) )
        {
            bytes = Long.parseLong( mem.substring( 0, mem.length() - 1 ) );
            bytes *= 1024 * 1024;
        }
        else if ( mem.endsWith( "k" ) || mem.endsWith( "K" ) )
        {
            bytes = Long.parseLong( mem.substring( 0, mem.length() - 1 ) );
            bytes *= 1024;
        }
        else
        {
            bytes = Long.parseLong( mem );
        }

        return bytes;
    }

    /**
     * Gets the maximum amount of heap memory available to the JVM.
     * 
     * @return The maximum heap size in bytes.
     */
    public long getMaxMemory()
    {
        if ( maxMemory < 0 )
        {
            String xmx = getXmxValue();
            if ( xmx.length() > 0 )
            {
                try
                {
                    maxMemory = parseMemSpec( xmx );
                    log.debug( "Maximum heap memory as reported by -Xmx{}: {}", xmx, maxMemory );
                }
                catch ( Exception e )
                {
                    log.debug( "Failed to parse -Xmx argument: " + e.getMessage(), e );
                    // fallback to other strategies below
                }
            }

            if ( maxMemory < 0 )
            {
                long max1 = Runtime.getRuntime().maxMemory();
                long max2 = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
                log.debug( "Maximum heap memory as reported by runtime and management: {} vs {}", max1, max2 );

                /*
                 * At least IBM JVM 1.5.0-SR2 reports a wrong value via Runtime (the total system RAM?) so we prefer the
                 * MBean if available.
                 */
                long max = ( max2 > 0 ) ? max2 : max1;

                /*
                 * Sun JVMs seem to reserve 0.77 % of the mem specified by -Xmx, so we try to adjust
                 */
                max = (long) ( max * 100 / 99.23 );

                maxMemory = max;
            }
        }

        return maxMemory;
    }

    private String getXmxValue()
    {
        List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for ( String arg : args )
        {
            if ( arg.startsWith( "-Xmx" ) )
            {
                return arg.substring( 4 );
            }
        }
        return "";
    }

}
