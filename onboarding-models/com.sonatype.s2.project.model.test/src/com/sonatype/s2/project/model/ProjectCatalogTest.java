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
package com.sonatype.s2.project.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import com.sonatype.s2.project.model.IS2ProjectCatalog;
import com.sonatype.s2.project.model.IS2ProjectCatalogEntry;
import com.sonatype.s2.project.model.catalog.ProjectCatalog;
import com.sonatype.s2.project.model.catalog.io.xpp3.S2ProjectCatalogXpp3Reader;
import com.sonatype.s2.project.model.catalog.io.xpp3.S2ProjectCatalogXpp3Writer;

public class ProjectCatalogTest
    extends TestCase
{
    private IS2ProjectCatalog loadCatalog( String filename )
        throws Exception
    {
        FileInputStream is = new FileInputStream( new File( filename ) );
        try
        {
            return new S2ProjectCatalogXpp3Reader().read( is );
        }
        finally
        {
            IOUtil.close( is );
        }
    }

    public void testReadCatalog()
        throws Exception
    {
        IS2ProjectCatalog catalog = loadCatalog( "resources/catalog.xml" );

        assertEquals( "Blah", catalog.getName() );

        List<IS2ProjectCatalogEntry> entries = catalog.getEntries();
        assertEquals( 1, entries.size() );
        assertEquals( "Foo", entries.get( 0 ).getName() );
        assertEquals( "./descriptor.xml", entries.get( 0 ).getDescriptorUrl() );
    }

    public void testCatalogVersion_1_0_4()
        throws Exception
    {
        IS2ProjectCatalog catalog = loadCatalog( "resources/catalogVersion_1.0.4.xml" );
        String xml;
        OutputStream os = new ByteArrayOutputStream();
        try
        {
            new S2ProjectCatalogXpp3Writer().write( os, (ProjectCatalog) catalog );
            xml= os.toString();
        }
        finally
        {
            IOUtil.close( os );
        }
        int count = StringUtils.countMatches( xml, "security-realm" );
        Assert.assertEquals( "Found 'security-realm' in XML:\n" + xml, 0, count );
    }
}
