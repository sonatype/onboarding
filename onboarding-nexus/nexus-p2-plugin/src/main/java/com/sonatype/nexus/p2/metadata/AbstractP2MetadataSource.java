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
package com.sonatype.nexus.p2.metadata;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.MXSerializer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.RemoteAccessException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.ByteArrayContentLocator;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.local.LocalRepositoryStorage;

import com.sonatype.nexus.p2.P2Constants;

public abstract class AbstractP2MetadataSource<E extends Repository>
    implements P2MetadataSource<E>
{

    protected static final List<String> METADATA_PATHS = Arrays.asList( P2Constants.SITE_XML, P2Constants.CONTENT_JAR,
        P2Constants.CONTENT_XML, P2Constants.ARTIFACTS_JAR, P2Constants.ARTIFACTS_XML,
        P2Constants.COMPOSITE_CONTENT_XML, P2Constants.COMPOSITE_CONTENT_JAR, P2Constants.COMPOSITE_ARTIFACTS_XML,
        P2Constants.COMPOSITE_ARTIFACTS_JAR );

    @Requirement
    private Logger logger;

    protected LocalRepositoryStorage getLocalStorage( E repository )
    {
        return repository.getLocalStorage();
    }

    protected String getName( E repository )
    {
        return repository.getName();
    }

    protected StorageItem createMetadataItem( String path, Xpp3Dom metadata, String hack, Map<String, Object> context,
                                              E repository )
        throws IOException
    {
        logger.debug( "Repository " + repository.getId() + ": Creating metadata item " + path );
        DefaultStorageFileItem result = createMetadataItem( repository, path, metadata, hack, context );

        setItemAttributes( result, context, repository );

        logger.debug( "Repository " + repository.getId() + ": Created metadata item " + path );
        return doCacheItem( result, repository );
    }

    public static DefaultStorageFileItem createMetadataItem( Repository repository, String path, Xpp3Dom metadata,
                                                             String hack, Map<String, Object> context )
        throws IOException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        MXSerializer mx = new MXSerializer();
        mx.setProperty( "http://xmlpull.org/v1/doc/properties.html#serializer-indentation", "  " );
        mx.setProperty( "http://xmlpull.org/v1/doc/properties.html#serializer-line-separator", "\n" );
        String encoding = "UTF-8";
        mx.setOutput( buffer, encoding );
        mx.startDocument( encoding, null );
        if ( hack != null )
        {
            mx.processingInstruction( hack );
        }
        metadata.writeToSerializer( null, mx );
        mx.flush();

        byte[] bytes = buffer.toByteArray();

        ContentLocator content = new ByteArrayContentLocator( bytes, "text/xml" );
        DefaultStorageFileItem result =
            new DefaultStorageFileItem( repository, new ResourceStoreRequest( path ), true /* isReadable */,
                false /* isWritable */, content );
        result.getItemContext().putAll( context );
        result.setLength( bytes.length );
        return result;
    }

    protected void setItemAttributes( StorageFileItem item, Map<String, Object> context, E repository )
        throws StorageException
    {
        // this is a hook, do nothing by default
    }

    protected AbstractStorageItem doCacheItem( AbstractStorageItem item, E repository )
        throws StorageException
    {
        AbstractStorageItem result = null;

        try
        {
            getLocalStorage( repository ).storeItem( repository, item );

            result =
                getLocalStorage( repository ).retrieveItem( repository, new ResourceStoreRequest( item.getPath() ) );

            result.getItemContext().putAll( item.getItemContext() );
        }
        catch ( ItemNotFoundException ex )
        {
            // this is a nonsense, we just stored it!
            result = item;
        }
        catch ( UnsupportedStorageOperationException ex )
        {
            result = item;
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * @see
     * com.sonatype.nexus.p2.metadata.P2MetadataSource#doRetrieveItem(org.sonatype.nexus.proxy.item.RepositoryItemUid,
     * java.util.Map)
     */
    public StorageItem doRetrieveItem( ResourceStoreRequest request, E repository )
        throws StorageException, ItemNotFoundException
    {
        if ( !isP2MetadataItem( request.getRequestPath() ) )
        {
            // let real resource store retrieve the item
            return null;
        }

        final long start = System.currentTimeMillis();

        // because we are outside realm of nexus here, we need to handle locking ourselves...
        final RepositoryItemUid uid = repository.createUid( P2Constants.METADATA_LOCK_PATH );
        final RepositoryItemUidLock lock = uid.getLock();

        // start with read lock, no need to do a write lock until we find it necessary
        lock.lock( Action.read );
        try
        {
            if ( P2Constants.CONTENT_PATH.equals( request.getRequestPath() ) )
            {
                try
                {
                    AbstractStorageItem contentItem = doRetrieveLocalItem( request, repository );

                    if ( !isContentOld( contentItem, repository ) )
                    {
                        return contentItem;
                    }
                }
                catch ( ItemNotFoundException e )
                {
                    // fall through
                }

                // we need to get new file, so update the lock
                lock.lock( Action.delete );

                try
                {
                    StorageItem result = doRetrieveContentItem( request.getRequestContext(), repository );
                    doRetrieveArtifactsItem( request.getRequestContext(), repository );
                    return result;
                }
                catch ( RuntimeException e )
                {
                    return doRetrieveLocalOnTransferError( request, repository, e );
                }
                finally
                {
                    lock.unlock();
                }
            }
            else if ( P2Constants.ARTIFACTS_PATH.equals( request.getRequestPath() ) )
            {
                try
                {
                    AbstractStorageItem artifactsItem = doRetrieveLocalItem( request, repository );

                    if ( !isArtifactsOld( artifactsItem, repository ) )
                    {
                        return artifactsItem;
                    }
                }
                catch ( ItemNotFoundException e )
                {
                    // fall through
                }

                // we need to get new file, so update the lock
                lock.lock( Action.delete );

                try
                {
                    deleteItemSilently( repository, new ResourceStoreRequest( P2Constants.PRIVATE_ROOT ) );
                    doRetrieveContentItem( request.getRequestContext(), repository );
                    return doRetrieveArtifactsItem( request.getRequestContext(), repository );
                }
                catch ( RuntimeException e )
                {
                    return doRetrieveLocalOnTransferError( request, repository, e );

                }
                finally
                {
                    lock.unlock();
                }
            }

            // we explicitly do not serve any other metadata files
            throw new ItemNotFoundException( request, repository );
        }
        finally
        {
            lock.unlock();

            logger.debug( "Repository " + repository.getId() + ": retrieve item: " + request.getRequestPath()
                + ": took " + ( System.currentTimeMillis() - start ) + " ms." );
        }
    }

    /**
     * If the given RuntimeException turns out to be a P2 server error, try to retrieve the item locally, else rethrow
     * exception.
     */
    private StorageItem doRetrieveLocalOnTransferError( ResourceStoreRequest request, E repository, RuntimeException e )
        throws StorageException, ItemNotFoundException
    {
        Throwable cause = e.getCause();
        // hm, we don't have this class here?
        if ( cause.getClass().getName().equals( "org.eclipse.equinox.internal.provisional.p2.core.ProvisionException" ) )
        {
            if ( cause.getMessage().startsWith( "HTTP Server" ) || cause.getCause() instanceof ConnectException )
            {
                // P2.getRemoteRepositoryItem server error
                return doRetrieveLocalItem( request, repository );
            }
        }
        throw e;
    }

    public static boolean isP2MetadataItem( String path )
    {
        return METADATA_PATHS.contains( path );
    }

    private void deleteP2Metadata( Repository repository )
    {
        logger.debug( "Repository " + repository.getId() + ": Deleting p2 metadata items." );
        deleteItemSilently( repository, new ResourceStoreRequest( P2Constants.ARTIFACTS_JAR ) );
        deleteItemSilently( repository, new ResourceStoreRequest( P2Constants.ARTIFACTS_XML ) );
        deleteItemSilently( repository, new ResourceStoreRequest( P2Constants.CONTENT_JAR ) );
        deleteItemSilently( repository, new ResourceStoreRequest( P2Constants.CONTENT_XML ) );
        deleteItemSilently( repository, new ResourceStoreRequest( P2Constants.PRIVATE_ROOT ) );
        logger.debug( "Repository " + repository.getId() + ": Deleted p2 metadata items." );
    }

    private static void deleteItemSilently( Repository repository, ResourceStoreRequest request )
    {
        try
        {
            repository.getLocalStorage().deleteItem( repository, request );
        }
        catch ( Exception e )
        {
            // that's okay, darling, don't worry about this too much
        }
    }

    protected AbstractStorageItem doRetrieveLocalItem( ResourceStoreRequest request, E repository )
        throws StorageException, ItemNotFoundException
    {
        if ( getLocalStorage( repository ) != null )
        {
            AbstractStorageItem localItem = getLocalStorage( repository ).retrieveItem( repository, request );

            localItem.getItemContext().putAll( request.getRequestContext() );

            return localItem;
        }

        throw new ItemNotFoundException( request, repository );
    }

    protected Xpp3Dom parseItem( Repository repo, String jarName, String xmlName, Map<String, Object> context )
        throws StorageException, ItemNotFoundException
    {
        try
        {
            try
            {
                StorageItem item = doRetrieveRemoteItem( repo, jarName, context );

                return parseJarItem( item, xmlName.substring( 1 ) );
            }
            catch ( ItemNotFoundException e )
            {
                StorageItem item = doRetrieveRemoteItem( repo, xmlName, context );

                return parseXmlItem( item );
            }
        }
        catch ( RemoteAccessException e )
        {
            throw new StorageException( e );
        }
        catch ( IOException e )
        {
            throw new StorageException( e );
        }
        catch ( XmlPullParserException e )
        {
            throw new StorageException( e );
        }

    }

    protected Xpp3Dom parseXmlItem( StorageItem item )
        throws IOException, XmlPullParserException
    {
        InputStream is = ( (StorageFileItem) item ).getInputStream();

        try
        {
            return Xpp3DomBuilder.build( new XmlStreamReader( is ) );
        }
        finally
        {
            is.close();
        }
    }

    protected Xpp3Dom parseJarItem( StorageItem item, String jarPath )
        throws IOException, XmlPullParserException
    {
        File file = File.createTempFile( "file", "zip" );
        try
        {
            InputStream is = ( (StorageFileItem) item ).getInputStream();

            try
            {
                FileUtils.copyStreamToFile( new RawInputStreamFacade( is ), file );

                ZipFile z = new ZipFile( file );

                try
                {
                    ZipEntry ze = z.getEntry( jarPath );

                    if ( ze == null )
                    {
                        throw new StorageException( "Corrupted P2 metadata jar " + jarPath );
                    }

                    InputStream zis = z.getInputStream( ze );

                    return Xpp3DomBuilder.build( new XmlStreamReader( zis ) );
                }
                finally
                {
                    z.close();
                }
            }
            finally
            {
                is.close();
            }
        }
        finally
        {
            file.delete();
        }
    }

    protected StorageItem doRetrieveArtifactsItem( Map<String, Object> context, E repository )
        throws StorageException, ItemNotFoundException
    {
        Xpp3Dom dom = doRetrieveArtifactsDom( context, repository );
        try
        {
            logger.debug( "Repository " + repository.getId() + ": Deleting p2 artifacts metadata items." );
            deleteItemSilently( repository, new ResourceStoreRequest( P2Constants.ARTIFACTS_JAR ) );
            deleteItemSilently( repository, new ResourceStoreRequest( P2Constants.ARTIFACTS_XML ) );
            logger.debug( "Repository " + repository.getId() + ": Deleted p2 artifacts metadata items." );

            return createMetadataItem( P2Constants.ARTIFACTS_PATH, dom, P2Constants.XMLPI_ARTIFACTS, context,
                repository );
        }
        catch ( IOException e )
        {
            throw new StorageException( e );
        }
    }

    protected StorageItem doRetrieveContentItem( Map<String, Object> context, E repository )
        throws StorageException, ItemNotFoundException
    {
        Xpp3Dom dom = doRetrieveContentDom( context, repository );
        try
        {
            logger.debug( "Repository " + repository.getId() + ": Deleting p2 content metadata items." );
            deleteItemSilently( repository, new ResourceStoreRequest( P2Constants.CONTENT_JAR ) );
            deleteItemSilently( repository, new ResourceStoreRequest( P2Constants.CONTENT_XML ) );
            logger.debug( "Repository " + repository.getId() + ": Deleted p2 content metadata items." );

            return createMetadataItem( P2Constants.CONTENT_PATH, dom, P2Constants.XMLPI_CONTENT, context, repository );
        }
        catch ( IOException e )
        {
            throw new StorageException( e );
        }
    }

    protected abstract StorageItem doRetrieveRemoteItem( Repository repo, String path, Map<String, Object> context )
        throws ItemNotFoundException, RemoteAccessException, StorageException;

    protected abstract Xpp3Dom doRetrieveArtifactsDom( Map<String, Object> context, E repository )
        throws StorageException, ItemNotFoundException;

    protected abstract Xpp3Dom doRetrieveContentDom( Map<String, Object> context, E repository )
        throws StorageException, ItemNotFoundException;

    protected abstract boolean isArtifactsOld( AbstractStorageItem artifactsItem, E repository )
        throws StorageException;

    protected abstract boolean isContentOld( AbstractStorageItem contentItem, E repository )
        throws StorageException;

}
