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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.sonatype.nexus.test.PlexusTestCaseSupport;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.storage.local.fs.FileContentLocator;


public class FilteredStorageItemPerformanceTest
    extends PlexusTestCaseSupport
{

    private File createTestFile()
        throws IOException
    {
        File testFile = new File( "target/FilteredStorageItemPerformanceTest.txt" );

        if ( testFile.exists() && testFile.length() > 0 )
        {
            return testFile;
        }

        FileWriter writer = null;

        try
        {
            writer = new FileWriter( testFile );

            int mb = 30;
            int count = 20000 * mb; // which is about correct, we are not looking for the exact size here.

            for ( int ii = 1; ii < count; ii++ )
            {
                if ( 10 % ii == 0 )
                {
                    IOUtil.copy(
                                 "A nice interpolated value: ${baseurl} the key was baseurl, the value is http://localhost:8080/nexus \n".getBytes(),
                                 writer );
                }
                else
                {
                    IOUtil.copy( "Some interesting Text, in a interesting file.   \n".getBytes(), writer );
                }
            }
        }
        finally
        {
            IOUtil.close( writer );
        }

        return testFile;
    }

    private void doTest( ContentLocator locator )
        throws IOException
    {
        File testFile = this.createTestFile();

        System.out.println( "File Size: ~" + testFile.length() / 1000000 + "mb" );

        int numberOfIterations = 10;
        Map<Long, Long> data = new HashMap<Long, Long>( numberOfIterations );

        long startTime = 0;
        long endTime = 0;

        InputStream in = null;
        OutputStream out = null;

        try
        {
            for ( int iteration = 0; iteration < numberOfIterations; iteration++ )
            {
                System.gc();
                out = new FileOutputStream( new File( "target/" + locator.getClass().getName() ) );
                in = locator.getContent();

                startTime = System.currentTimeMillis();
                IOUtil.copy( in, out );
                endTime = System.currentTimeMillis();
                data.put( endTime - startTime, Runtime.getRuntime().freeMemory() );
            }
        }
        finally
        {
            IOUtil.close( in );
            IOUtil.close( out );
        }

        System.out.println( "Data:" );
        long totalTime = 0;
        long totalMemory = 0;
        for ( Entry<Long, Long> entry : data.entrySet() )
        {
            totalTime += entry.getKey();
            totalMemory += entry.getValue();
            System.out.println( entry.getKey() + "\t" + entry.getValue() );
        }

        System.out.println( "\nAverage: " + (double) totalTime / (double) ( data.size() ) + "ms\t"
            + (double) totalMemory / (double) ( data.size() ) / 1000000d + " MB" );
    }

    @Test
    public void testAndTimeStorageFilteredFileItem()
        throws IOException
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put( "baseurl", "http://localhost:8081/nexus" );

        System.out.println( "\nFiltered Storage File Item" );
        this.doTest( new InterpolatedContentLocator( new FileContentLocator( this.createTestFile(), "plain/text" ), properties ) );
    }

    @Test
    public void testAndTimeStorageFileItem()
        throws IOException
    {
        System.out.println( "\nDefault Storage File Item" );

        this.doTest( new FileContentLocator( this.createTestFile(), "plain/text" ) );
    }
}
