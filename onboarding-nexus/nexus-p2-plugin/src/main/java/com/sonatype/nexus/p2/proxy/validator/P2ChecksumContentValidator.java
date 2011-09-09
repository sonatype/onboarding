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
package com.sonatype.nexus.p2.proxy.validator;

import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.attributes.inspectors.DigestCalculatingInspector;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.maven.AbstractChecksumContentValidator;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.RemoteHashResponse;
import org.sonatype.nexus.proxy.repository.ItemContentValidator;
import org.sonatype.nexus.proxy.repository.ProxyRepository;

import com.sonatype.nexus.p2.P2Constants;
import com.sonatype.nexus.p2.proxy.P2ProxyMetadataSource;
import com.sonatype.nexus.p2.proxy.P2ProxyRepository;
import com.sonatype.nexus.p2.proxy.mappings.ArtifactMapping;
import com.sonatype.nexus.p2.proxy.mappings.ArtifactPath;

/**
 * P2 checksum content validator.
 * 
 * @author velo
 */
@Component( role = ItemContentValidator.class, hint = "P2ChecksumContentValidator" )
public class P2ChecksumContentValidator
    extends AbstractChecksumContentValidator
    implements ItemContentValidator
{

    @Override
    protected ChecksumPolicy getChecksumPolicy( ProxyRepository proxy, AbstractStorageItem item )
        throws StorageException
    {
        if ( P2ProxyMetadataSource.isP2MetadataItem( item.getRepositoryItemUid().getPath() ) )
        {
            // the checksum is on metadata files
            return ChecksumPolicy.IGNORE;
        }

        if ( !proxy.getRepositoryKind().isFacetAvailable( P2ProxyRepository.class ) )
        {
            return ChecksumPolicy.IGNORE;
        }

        P2ProxyRepository p2repo = proxy.adaptToFacet( P2ProxyRepository.class );

        ChecksumPolicy checksumPolicy = p2repo.getChecksumPolicy();

        if ( checksumPolicy == null || !checksumPolicy.shouldCheckChecksum()
            || !( item instanceof DefaultStorageFileItem ) )
        {
            // there is either no need to validate or we can't validate the item content
            return ChecksumPolicy.IGNORE;
        }

        ResourceStoreRequest req = new ResourceStoreRequest( P2Constants.ARTIFACT_MAPPINGS_XML );
        req.setRequestLocalOnly( true );
        try
        {
            p2repo.retrieveItem( true, req );
        }
        catch ( Exception e )
        {
            // no way to calculate
            getLogger().debug( "Unable to find artifact-mapping.xml", e );
            return ChecksumPolicy.IGNORE;
        }

        return checksumPolicy;
    }

    @Override
    protected void cleanup( ProxyRepository proxy, RemoteHashResponse remoteHash, boolean contentValid )
        throws StorageException
    {
        // no know cleanup for p2 repos
    }

    @Override
    protected RemoteHashResponse retrieveRemoteHash( AbstractStorageItem item, ProxyRepository proxy, String baseUrl )
        throws StorageException
    {
        P2ProxyRepository p2repo = proxy.adaptToFacet( P2ProxyRepository.class );

        Map<String, ArtifactPath> paths;
        try
        {
            final ArtifactMapping artifactMapping = p2repo.getArtifactMappings().get( baseUrl );
            if ( artifactMapping == null )
            {
                getLogger().debug( "Unable to retrive remote has for " + item.getPath() );
                return null;
            }
            paths = artifactMapping.getArtifactsPath();
        }
        catch ( IllegalOperationException e )
        {
            getLogger().error( "Unable to open artifactsMapping.xml", e );
            return null;
        }
        String md5 = paths.get( item.getPath() ).getMd5();
        if ( md5 == null )
        {
            return null;
        }
        return new RemoteHashResponse( DigestCalculatingInspector.DIGEST_MD5_KEY, md5, null );
    }

}
