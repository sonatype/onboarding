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
package com.sonatype.nexus.p2.lineup.persist;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.access.NexusItemAuthorizer;
import org.sonatype.nexus.proxy.item.ByteArrayContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.walker.AbstractFileWalkerProcessor;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.Walker;
import org.sonatype.nexus.proxy.walker.WalkerContext;

import com.sonatype.nexus.p2.lineup.repository.P2LineupConstants;
import com.sonatype.nexus.p2.lineup.repository.P2LineupRepository;
import com.sonatype.nexus.p2.lineup.resolver.CannotResolveP2LineupException;
import com.sonatype.nexus.p2.lineup.resolver.P2LineupResolver;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Reader;
import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Writer;

/**
 * Default directory based storage of P2 Lineups. P2 gavs will be flattend out and contained within a single directory.
 * 
 * @author bdemers
 */
@Component( role = P2LineupManager.class )
public class DefaultP2LineupManager
    implements P2LineupManager
{
    private ReentrantLock lock = new ReentrantLock();
    
    @Requirement
    private Logger logger;

    @Requirement
    private P2LineupResolver lineupResolver;

    @Requirement
    private RepositoryRegistry repositoryRegistry;

    @Requirement
    private NexusItemAuthorizer nexusItemAuthorizer;

    @Requirement
    private Walker walker;

    public P2LineupRepository getDefaultP2LineupRepository()
        throws P2ConfigurationException
    {
        List<P2LineupRepository> existingRepositories =
            repositoryRegistry.getRepositoriesWithFacet( P2LineupRepository.class );
        if ( existingRepositories == null || existingRepositories.isEmpty() )
        {
            throw new P2ConfigurationException( "The P2 lineup repository does not exist." );
        }
        if ( existingRepositories.size() > 1 )
        {
            throw new P2ConfigurationException( "More than one P2 lineup repository." );
        }
        return existingRepositories.get( 0 );
    }

    public P2Lineup addLineup( P2Lineup lineup )
        throws P2LineupStorageException, CannotResolveP2LineupException, P2AccessDeniedException,
        P2ConfigurationException
    {

        P2Gav gav = new P2Gav( lineup );
        P2LineupRepository p2Repo = getDefaultP2LineupRepository();

        // check access, need to check before we try to resolve it
        this.validateAccess( p2Repo, gav, Action.create );

        // try to retrieve to check if exists
        if ( this.doesLineupExist( p2Repo, gav ) )
        {
            throw new P2LineupStorageException( "P2 Lineup: '" + gav + "' already exists." );
        }

        try
        {
            lineupResolver.resolveLineup( p2Repo, lineup, false/* validateOnly */);
        }
        catch ( CannotResolveP2LineupException e )
        {
            if ( e.isFatal() )
            {
                throw e;
            }
        }

        writeLineup( lineup, p2Repo );

        return lineup;
    }

    public P2Lineup updateLineup( P2Lineup lineup )
        throws NoSuchP2LineupException, P2LineupStorageException, CannotResolveP2LineupException,
        P2AccessDeniedException, P2ConfigurationException
    {

        P2Gav gav = new P2Gav( lineup );
        P2LineupRepository p2Repo = getDefaultP2LineupRepository();

        // check access, check before resolving
        this.validateAccess( p2Repo, gav, Action.update );

        // make sure it exists and we have access to it
        try
        {
            p2Repo.retrieveItem( new ResourceStoreRequest( getLineupFolderPath( gav ) ) );
        }
        catch ( ItemNotFoundException e )
        {
            throw new NoSuchP2LineupException( gav );
        }
        catch ( AccessDeniedException e )
        {
            // this should not happen if we get this far
            throw new P2AccessDeniedException( gav, Action.read );
        }
        catch ( Exception e )
        {
            throw new P2LineupStorageException( "Cannot retrieve P2 lineup: " + gav.toString() );
        }

        try
        {
            lineupResolver.resolveLineup( p2Repo, lineup, false/* validateOnly */);
        }
        catch ( CannotResolveP2LineupException e )
        {
            if ( e.isFatal() )
            {
                throw e;
            }
        }

        writeLineup( lineup, p2Repo );

        return lineup;
    }

    public void deleteLineup( P2Gav gav )
        throws NoSuchP2LineupException, P2LineupStorageException, P2AccessDeniedException, P2ConfigurationException
    {
        P2LineupRepository p2Repo = getDefaultP2LineupRepository();

        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Deleting P2 Linup to: " + gav.toPathString() );
        }

        try
        {
            p2Repo.deleteItem( new ResourceStoreRequest( getLineupFolderPath( gav ) ) );
        }
        catch ( AccessDeniedException e )
        {
            throw new P2AccessDeniedException( gav, Action.delete, e );
        }
        catch ( ItemNotFoundException e )
        {
            throw new NoSuchP2LineupException( gav, e );
        }
        catch ( Exception e )
        {
            throw new P2LineupStorageException( "Failed to delete P2 Lineup: " + gav.toString(), e );
        }
    }

    public P2Lineup getLineup( P2Gav gav )
        throws NoSuchP2LineupException, P2AccessDeniedException, P2ConfigurationException
    {
        P2LineupRepository p2Repo = getDefaultP2LineupRepository();

        // check access
        this.validateAccess( p2Repo, gav, Action.read );

        try
        {

            StorageItem storageItem = p2Repo.retrieveItem( new ResourceStoreRequest( getLineupRepositoryPath( gav ) ) );

            // read / throw on any exceptions
            return readLineup( storageItem );
        }
        catch ( IOException e )
        {
            throw new NoSuchP2LineupException( gav, e );
        }
        catch ( XmlPullParserException e )
        {
            throw new NoSuchP2LineupException( gav, e );
        }
        catch ( AccessDeniedException e )
        {
            throw new P2AccessDeniedException( gav, Action.read, e );
        }
        catch ( ItemNotFoundException e )
        {
            throw new NoSuchP2LineupException( gav );
        }
        catch ( IllegalOperationException e )
        {
            throw new P2ConfigurationException( "Could not read P2 lineup: " + gav.toString(), e );
        }
    }

    public Set<P2Lineup> getLineups()
        throws P2ConfigurationException
    {
        final P2LineupRepository p2Repo = this.getDefaultP2LineupRepository();

        ResourceStoreRequest storeRequest = new ResourceStoreRequest( "/", true );
        DefaultWalkerContext ctx = new DefaultWalkerContext( p2Repo, storeRequest );
        

        final Set<P2Lineup> result = new HashSet<P2Lineup>();

        ctx.getProcessors().add( new AbstractFileWalkerProcessor()
        {
            @Override
            protected void processFileItem( WalkerContext context, StorageFileItem fItem )
                throws Exception
            {
                // add the / to mimic the request
                if ( P2LineupConstants.LINEUP_DESCRIPTOR_XML.equals( "/" + fItem.getName() ) )
                {
                    try
                    {
                        P2Lineup lineup = readLineup( fItem );
                        if( checkAccess( p2Repo, getLineupRepositoryPath( lineup.getGroupId(), lineup.getId(), lineup.getVersion() ), Action.read ))
                        {
                            result.add( lineup );    
                        }
                    }
                    catch ( IOException e )
                    {
                        logger.error( "Error reading lineup from: " + fItem.getPath(), e );
                    }
                    catch ( XmlPullParserException e )
                    {
                        logger.error( "Error reading lineup from: " + fItem.getPath(), e );
                    }
                    catch ( P2ConfigurationException e )
                    {
                        logger.error( "Error reading lineup from: " + fItem.getPath(), e );
                    }
                }
            }
        } );

        walker.walk( ctx );

        return result;
    }

    public void validateLineup( P2Lineup lineup )
        throws CannotResolveP2LineupException, P2ConfigurationException, P2AccessDeniedException,
        P2LineupStorageException
    {
        boolean update = this.doesLineupExist( getDefaultP2LineupRepository(), new P2Gav( lineup ) );
        this.validateAccess( lineup, update );
        lineupResolver.resolveLineup( getDefaultP2LineupRepository(), lineup, true/* validateOnly */);
    }

    private boolean doesLineupExist( P2LineupRepository p2Repo, P2Gav gav )
        throws P2AccessDeniedException, P2LineupStorageException
    {
        try
        {
            p2Repo.retrieveItem( new ResourceStoreRequest( getLineupFolderPath( gav ) ) );
            return true;
        }
        catch ( ItemNotFoundException e )
        {
            return false;
        }
        catch ( AccessDeniedException e )
        {
            throw new P2AccessDeniedException( gav, Action.read );
        }
        catch ( Exception e )
        {
            throw new P2LineupStorageException( "Cannot retrieve P2 lineup: " + gav.toString() );
        }
    }

    private String getLineupFolderPath( P2Gav gav )
    {
        return getLineupFolderPath( gav.getGroupId(), gav.getId(), gav.getVersion() );
    }

    private String getLineupFolderPath( String groupId, String id, String version )
    {
        return "/" + groupId.replaceAll( "\\.", "/" ) + "/" + id + "/" + version;
    }

    private String getLineupRepositoryPath( P2Gav gav )
    {
        return getLineupFolderPath( gav ) + P2LineupConstants.LINEUP_DESCRIPTOR_XML;
    }

    private String getLineupRepositoryPath( String groupId, String id, String version )
    {
        return getLineupFolderPath( groupId, id, version ) + P2LineupConstants.LINEUP_DESCRIPTOR_XML;
    }

    private void writeLineup( P2Lineup lineup, P2LineupRepository p2Repo )
        throws P2LineupStorageException
    {
        // block other writes, not sure we need to do this now that we use the repository for storage
        lock.lock();

        try
        {
            String lineupFilePath =
                this.getLineupRepositoryPath( lineup.getGroupId(), lineup.getId(), lineup.getVersion() );

            if ( logger.isDebugEnabled() )
            {
                logger.debug( "Writing P2 Linup repository: " + lineupFilePath );
            }

            ResourceStoreRequest storeRequest = new ResourceStoreRequest( lineupFilePath );
            storeRequest.getRequestContext().put( FROM_LINEUP_API, "true" );

            // buffer in memory
            P2LineupXpp3Writer writer = new P2LineupXpp3Writer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter streamWriter = new OutputStreamWriter( baos );
            writer.write( streamWriter, lineup );

            // write to repository
            StorageFileItem storageFileItem =
                new DefaultStorageFileItem( p2Repo, storeRequest, true, true,
                                            new ByteArrayContentLocator( baos.toByteArray(), "application/xml" ) );
//            p2Repo.getLocalStorage().storeItem( p2Repo, storageFileItem );
            p2Repo.storeItem( false, storageFileItem );

        }
        catch ( IOException e )
        {
            // TODO Cleanup all the p2 "artifacts" from the lineup repo
            throw new P2LineupStorageException( "P2 Lineup: '" + new P2Gav( lineup ) + "' could not be stored.", e );
        }
        catch ( UnsupportedStorageOperationException e )
        {
            throw new P2LineupStorageException( "P2 Lineup: '" + new P2Gav( lineup ) + "' could not be stored.", e );
        }
        catch ( IllegalOperationException e )
        {
            throw new P2LineupStorageException( "P2 Lineup: '" + new P2Gav( lineup ) + "' could not be stored.", e );
        }
        finally
        {
            lock.unlock();
        }
    }

    private P2Lineup readLineup( StorageItem storageItem )
        throws IOException, XmlPullParserException, P2ConfigurationException
    {

        if ( !StorageFileItem.class.isInstance( storageItem ) )
        {
            throw new P2ConfigurationException( "Could not read P2 lineup: " + storageItem.getPath()
                + ", the repository does not think it is a file." );
        }

        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Reading P2 Linup from stream" );
        }

        InputStreamReader streamReader = null;
        try
        {
            streamReader = new InputStreamReader( ( (StorageFileItem) storageItem ).getInputStream() );
            P2LineupXpp3Reader reader = new P2LineupXpp3Reader();
            return reader.read( streamReader );
        }
        finally
        {
            IOUtil.close( streamReader );
        }
    }

    private void validateAccess( P2LineupRepository repository, P2Gav gav, Action action )
        throws P2AccessDeniedException
    {
        if ( !this.checkAccess( repository, "/" + gav.toPathString(), action ) )
        {
            throw new P2AccessDeniedException( gav, action );
        }
    }

    public void validateAccess( P2Lineup lineup, boolean update )
        throws P2ConfigurationException, P2AccessDeniedException
    {
        P2Gav p2Gav = new P2Gav( lineup );
        P2LineupRepository p2Repo = getDefaultP2LineupRepository();

        this.validateAccess( p2Repo, p2Gav, ( update ? Action.update : Action.create ) );
    }

    private boolean checkAccess( P2LineupRepository repository, String gavPath, Action action )
    {
        return nexusItemAuthorizer.authorizePath( repository, new ResourceStoreRequest( gavPath ), action );
    }
}
