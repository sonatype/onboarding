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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import org.sonatype.nexus.test.PlexusTestCaseSupport;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.storage.local.fs.FileContentLocator;


public class FilteredStorageItemTest
    extends PlexusTestCaseSupport
{

    private File createTestFile()
        throws IOException
    {
        File testFile = new File( "target/FilteredStorageItemTest.txt" );
        FileWriter writer = null;

        try
        {
            writer = new FileWriter( testFile );
            IOUtil.copy( "A nice interpolated value: ${baseurl} the key was baseurl\n".getBytes(), writer, "UTF-8" );
        }
        finally
        {
            IOUtil.close( writer );
        }

        return testFile;
    }

    @Test
    public void testInterpolation()
        throws IOException
    {

        HashMap<String, String> properties = new HashMap<String, String>();
        String baseUrl = "http://localhost:8081/nexus";
        properties.put( "baseurl", baseUrl );

        ContentLocator contentLocator =
            new InterpolatedContentLocator( new FileContentLocator( this.createTestFile(), "plain/text" ), properties );

        InputStream in = null;

        try
        {
            in = contentLocator.getContent();
            String result = IOUtil.toString( in );

            Assert.assertTrue( result.contains( baseUrl ) );
            Assert.assertFalse( result.contains( "$" ) );

        }
        finally
        {
            IOUtil.close( in );
        }
    }
}
