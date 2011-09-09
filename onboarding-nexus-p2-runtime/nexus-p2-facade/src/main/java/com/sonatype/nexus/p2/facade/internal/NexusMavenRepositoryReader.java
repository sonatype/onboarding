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
package com.sonatype.nexus.p2.facade.internal;

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.artifact.IllegalArtifactCoordinateException;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.ArtifactStoreHelper;
import org.sonatype.nexus.proxy.maven.ArtifactStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.tycho.p2.facade.internal.GAV;
import org.sonatype.tycho.p2.facade.internal.RepositoryReader;

public class NexusMavenRepositoryReader
    implements RepositoryReader
{
    private final MavenRepository mavenRepository;

    public NexusMavenRepositoryReader( MavenRepository mavenRepository )
    {
        this.mavenRepository = mavenRepository;
    }

    public InputStream getContents( GAV gav, String classifier, String extension )
        throws IOException
    {
        //TODO Use RepositoryLayoutHelper.getRelativePath() instead
        StringBuilder path = new StringBuilder( gav.getGroupId().replaceAll( "\\.", "/" ) );
        path.append( '/' ).append( gav.getArtifactId() ).append( '/' ).append( gav.getVersion() );
        path.append( '/' ).append( gav.getArtifactId() ).append( '-' ).append( gav.getVersion() );
        path.append( '-' ).append( classifier ).append( '.' ).append( extension );

        try
        {
            ArtifactStoreRequest gavRequest =
                new ArtifactStoreRequest( mavenRepository, path.toString(), false /* localOnly */);
            ArtifactStoreHelper helper = mavenRepository.getArtifactStoreHelper();
            StorageFileItem fileItem = helper.retrieveArtifact( gavRequest );

            return fileItem.getInputStream();
        }
        catch ( IllegalArtifactCoordinateException e )
        {
            throw newIOException( "Could not retrieve artifact " + path + " from repository id "
                + mavenRepository.getId(), e );
        }
        catch ( AccessDeniedException e )
        {
            throw newIOException( "Could not retrieve artifact " + path + " from repository id "
                + mavenRepository.getId(), e );
        }
        catch ( IllegalOperationException e )
        {
            throw newIOException( "Could not retrieve artifact " + path + " from repository id "
                + mavenRepository.getId(), e );
        }
        catch ( ItemNotFoundException e )
        {
            throw newIOException( "Could not retrieve artifact " + path + " from repository id "
                + mavenRepository.getId(), e );
        }
    }

    private IOException newIOException( String message, Exception cause )
    {
        IOException e = new IOException( message );
        e.initCause( cause );
        return e;
    }

    public InputStream getContents( String remoteRelativePath )
        throws IOException
    {
        ResourceStoreRequest storeRequest = new ResourceStoreRequest( remoteRelativePath );
        try
        {
            StorageFileItem storageItem = (StorageFileItem) mavenRepository.retrieveItem( storeRequest );
            return storageItem.getInputStream();
        }
        catch ( Exception e )
        {
            throw newIOException( "Could not retrieve item: " + remoteRelativePath, e );
        }
    }
}
