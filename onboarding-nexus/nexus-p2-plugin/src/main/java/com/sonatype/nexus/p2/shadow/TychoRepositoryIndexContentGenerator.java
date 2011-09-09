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
package com.sonatype.nexus.p2.shadow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.search.Query;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.SearchType;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringInputStream;
import org.codehaus.tycho.ProjectType;
import org.sonatype.nexus.index.IndexerManager;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.ByteArrayContentLocator;
import org.sonatype.nexus.proxy.item.ContentGenerator;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.tycho.p2.facade.internal.DefaultTychoRepositoryIndex;
import org.sonatype.tycho.p2.facade.internal.GAV;
import org.sonatype.tycho.p2.facade.internal.RepositoryReader;

@Component( role = ContentGenerator.class, hint = TychoRepositoryIndexContentGenerator.ROLE_HINT )
public class TychoRepositoryIndexContentGenerator
    implements ContentGenerator
{

    public static final String ROLE_HINT = "TychoIndexContentGenerator";

    @Requirement
    private IndexerManager indexerManager;

    public ContentLocator generateContent( Repository repository, String path, StorageFileItem item )
        throws IllegalOperationException, ItemNotFoundException, StorageException
    {
        try
        {
            DefaultTychoRepositoryIndex index = new DefaultTychoRepositoryIndex( new RepositoryReader()
            {

                public InputStream getContents( GAV arg0, String arg1, String arg2 )
                    throws IOException
                {
                    return new StringInputStream( "" );
                }

                public InputStream getContents( String arg0 )
                    throws IOException
                {
                    return new StringInputStream( "" );
                }
            } );

            for ( String packaging : ProjectType.PROJECT_TYPES )
            {
                Query pq = indexerManager.constructQuery( MAVEN.PACKAGING, packaging, SearchType.EXACT );

                IteratorSearchResponse hits =
                    indexerManager.searchQueryIterator( pq, repository.getId(), null, null, null, false, null );

                for ( ArtifactInfo info : hits )
                {
                    index.addProject( info.groupId, info.artifactId, info.version );
                }
            }

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            index.write( buf );
            byte[] bytes = buf.toByteArray();

            item.setLength( bytes.length );

            return new ByteArrayContentLocator( bytes, "text/plain" );
        }
        catch ( NoSuchRepositoryException e )
        {
            // can't really happen, can it?
            throw new StorageException( e );
        }
        catch ( IOException e )
        {
            throw new StorageException( "Could not construct Tycho repository index", e );
        }

    }

    public String getGeneratorId()
    {
        return ROLE_HINT;
    }

}
