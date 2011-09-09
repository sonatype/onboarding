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
package com.sonatype.nexus.plugin.p2.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;

import com.sonatype.nexus.p2.metadata.Artifacts;
import com.sonatype.nexus.p2.metadata.ArtifactsMerge;
import com.sonatype.nexus.p2.metadata.Content;
import com.sonatype.nexus.p2.metadata.P2MetadataMergeException;

public class MetadataMergeTest
{
    @Test
    public void mergeArtifactsMetadata()
        throws Exception
    {
        ArrayList<Artifacts> repos = loadArtifactsMetadata( new String[] {
            "metadata/merge/artifact1.xml",
            "metadata/merge/artifact2.xml" } );

        // sanity check
        Assert.assertEquals( 2, repos.get( 0 ).getArtifacts().size() );
        Assert.assertEquals( 1, repos.get( 1 ).getArtifacts().size() );

        ArtifactsMerge m = new ArtifactsMerge();

        Artifacts merged = m.mergeArtifactsMetadata( "test", repos );

        Assert.assertEquals( 3, merged.getArtifacts().size() );
        Assert.assertEquals( 5, merged.getMappings().size() );
    }

    // @Test
    public void incompatibleRepositoryProperties()
        throws Exception
    {
        ArrayList<Artifacts> repos = loadArtifactsMetadata( new String[] {
            "metadata/merge/artifact1.xml",
            "metadata/merge/artifact2props.xml" } );

        ArtifactsMerge m = new ArtifactsMerge();

        try
        {
            m.mergeArtifactsMetadata( "test", repos );
            Assert.fail( "RepositoryMetadataMergeException expected" );
        }
        catch ( P2MetadataMergeException e )
        {
            // expected
        }
    }

    @Test
    public void incompatibleMappingsRules()
        throws Exception
    {
        ArrayList<Artifacts> repos = loadArtifactsMetadata( new String[] {
            "metadata/merge/artifact1.xml",
            "metadata/merge/artifact2mappins.xml" } );

        ArtifactsMerge m = new ArtifactsMerge();

        try
        {
            m.mergeArtifactsMetadata( "test", repos );
            Assert.fail( "P2MetadataMergeException expected" );
        }
        catch ( P2MetadataMergeException e )
        {
            if ( !e.getMessage().startsWith( "Incompatible artifact repository mapping rules: filter=" ) )
            {
                throw e;
            }
        }
    }

    @Test
    public void mergeContentMetadata()
        throws Exception
    {
        ArrayList<Content> repos = loadContentMetadata( new String[] {
            "metadata/merge/content1.xml",
            "metadata/merge/content2.xml" } );

        ArtifactsMerge m = new ArtifactsMerge();

        Content merged = m.mergeContentMetadata( "test", repos );

        // repo1: bundle, featureJar, featureGroup, jre, jreConfig
        // repo2: feature2Jar, feature2Group (jre and jreConfig ignored)
        Assert.assertEquals( 7, merged.getUnits().size() );
    }

    private ArrayList<Artifacts> loadArtifactsMetadata( String[] files )
        throws IOException,
            XmlPullParserException
    {
        ArrayList<Artifacts> repos = new ArrayList<Artifacts>();
        for ( String file : files )
        {
            repos.add( new Artifacts( loadXpp3Dom( file ) ) );
        }
        return repos;
    }

    private ArrayList<Content> loadContentMetadata( String[] files )
        throws IOException,
            XmlPullParserException
    {
        ArrayList<Content> repos = new ArrayList<Content>();
        for ( String file : files )
        {
            repos.add( new Content( loadXpp3Dom( file ) ) );
        }
        return repos;
    }

    private Xpp3Dom loadXpp3Dom( String filepath )
        throws IOException,
            XmlPullParserException
    {
        FileInputStream is = new FileInputStream( new File( "src/test/resources", filepath ) );
        try
        {
            return Xpp3DomBuilder.build( new XmlStreamReader( is ) );
        }
        finally
        {
            IOUtil.close( is );
        }
    }
}
