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
package com.sonatype.nexus.onboarding.installer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;

import com.sonatype.nexus.onboarding.project.repository.OnboardingProjectRepository;
import com.sonatype.s2.project.model.IS2Project;

@Component( role = MSEInstallerManager.class )
public class DefaultMSEInstallerManager
    implements MSEInstallerManager
{
    public static final String JNLP_ARTIFACT_ID = "com.sonatype.s2.installer.jnlp";

    public static final String JNLP_CLASSIFIER_ID = "mse.installer.jnlp";

    public static final String INSTALLER_VERSION_PROPERTY = "mse.installer.compatible.versions";

    @Requirement
    private RepositoryRegistry repositoryRegistry;

    @Requirement
    private Logger logger;

    public synchronized Set<MSEInstallerInfo> getInstallers()
        throws NoSuchRepositoryException, StorageException, AccessDeniedException, ItemNotFoundException,
        IllegalOperationException
    {
        Repository installerRepository = getInstallerRepository();
        return loadInstallersFromRepository( installerRepository );
    }

    private Set<MSEInstallerInfo> loadInstallersFromRepository( Repository source )
        throws StorageException, AccessDeniedException, ItemNotFoundException, IllegalOperationException
    {
        ResourceStoreRequest storeRequest = new ResourceStoreRequest( "/artifacts.xml" );
        storeRequest.getRequestContext().put( AccessManager.REQUEST_AUTHORIZED, true );
        StorageFileItem artifactsXML = (StorageFileItem) source.retrieveItem( storeRequest );
        InputStream artifactsXMLStream = null;
        try
        {
            artifactsXMLStream = artifactsXML.getInputStream();
            return extractInstallersFromArtifacts( artifactsXMLStream, source );
        }
        catch ( IOException e )
        {
            throw new LocalStorageException( e );
        }
        finally
        {
            IOUtil.close( artifactsXMLStream );
        }
    }

    private Set<MSEInstallerInfo> extractInstallersFromArtifacts( InputStream is, Repository source )
        throws LocalStorageException
    {
        try
        {
            Xpp3Dom x = Xpp3DomBuilder.build( new XmlStreamReader( is ) );
            Xpp3Dom[] artifacts = x.getChildren( "artifacts" )[0].getChildren();
            Set<MSEInstallerInfo> installers = new HashSet<MSEInstallerInfo>();
            for ( int i = 0; i < artifacts.length; i++ )
            {
                if ( JNLP_ARTIFACT_ID.equals( artifacts[i].getAttribute( "id" ) )
                    && JNLP_CLASSIFIER_ID.equals( artifacts[i].getAttribute( "classifier" ) ) )
                {
                    MSEInstallerInfo newInstaller = createInstallerInfo( artifacts[i], source );
                    if ( newInstaller != null )
                    {
                        logger.debug( "Found MSE installer: " + newInstaller );
                        installers.add( newInstaller );
                    }
                }
            }
            return installers;
        }
        catch ( XmlPullParserException e )
        {
            throw new LocalStorageException( e );
        }
        catch ( IOException e )
        {
            throw new LocalStorageException( e );
        }
    }

    private MSEInstallerInfo createInstallerInfo( Xpp3Dom dom, Repository source )
    {
        final String VERSION = "version";
        final String PROPERTIES = "properties";
        final String PROPERTY = "property";
        final String NAME = "name";
        final String VALUE = "value";

        MSEInstallerInfo result = new MSEInstallerInfo( dom.getAttribute( VERSION ), source );
        Xpp3Dom properties = dom.getChild( PROPERTIES );
        if ( properties == null )
        {
            logger.warn( result + " does not have properties." );
            return null;
        }
        Xpp3Dom[] versionElt = properties.getChildren( PROPERTY );
        String supportedVersions = null;
        for ( int i = 0; i < versionElt.length; i++ )
        {
            if ( INSTALLER_VERSION_PROPERTY.equals( versionElt[i].getAttribute( NAME ) ) )
            {
                supportedVersions = versionElt[i].getAttribute( VALUE );
                break;
            }
        }
        if ( supportedVersions == null || supportedVersions.trim().length() == 0 )
        {
            logger.warn( result + " does not have a " + INSTALLER_VERSION_PROPERTY + " property." );
            return null;
        }
        StringTokenizer tokens = new StringTokenizer( supportedVersions, "," );
        Set<String> versions = new java.util.HashSet<String>( tokens.countTokens() );
        while ( tokens.hasMoreElements() )
        {
            versions.add( ( (String) tokens.nextElement() ).trim() );
        }
        result.setCanInstallVersions( versions );
        return result;
    }

    public MSEInstallerInfo resolveInstaller( String version )
        throws NoSuchRepositoryException, StorageException, AccessDeniedException, ItemNotFoundException,
        IllegalOperationException
    {
        MSEInstallerInfo selectedInstaller = null;
        logger.debug( "Looking up MSE installer for version: " + version );
        Set<MSEInstallerInfo> installers = getInstallers();
        for ( MSEInstallerInfo installer : installers )
        {
            if ( installer.canInstallVersion( version ) )
            {
                logger.debug( "Found matching MSE installer for version " + version + ": " + installer );
                if ( selectedInstaller == null
                    || selectedInstaller.getInstallerVersion().compareTo( installer.getInstallerVersion() ) < 0 )
                {
                    selectedInstaller = installer;
                }
            }
        }

        if ( selectedInstaller == null )
        {
            logger.debug( "Did not find installer for version: " + version );
        }
        else
        {
            logger.debug( "Selected matching MSE installer for version " + version + ": " + selectedInstaller );
        }
        return selectedInstaller;
    }

    public StorageFileItem getJnlpItem( MSEInstallerInfo installer )
        throws StorageException, AccessDeniedException, ItemNotFoundException, IllegalOperationException
    {
        Repository nexusRepository = installer.getNexusRepository();
        String jnlpItemPath = "/mse_installer/" + JNLP_ARTIFACT_ID + "_" + installer.getInstallerVersion() + ".jnlp";
        ResourceStoreRequest storeRequest = new ResourceStoreRequest( jnlpItemPath );
        storeRequest.getRequestContext().put( AccessManager.REQUEST_AUTHORIZED, true );
        return (StorageFileItem) nexusRepository.retrieveItem( storeRequest );
    }

    private Repository getInstallerRepository()
        throws NoSuchRepositoryException
    {
        OnboardingProjectRepository codebaseRepository = (OnboardingProjectRepository) repositoryRegistry.getRepository( IS2Project.PROJECT_REPOSITORY_ID );
        String installerRepositoryId =
            codebaseRepository.getExternalConfiguration( false /* forModification */).getMSEInstallersRepositoryId();
        logger.debug( "MSE installers repository id: " + installerRepositoryId );
        return repositoryRegistry.getRepository( installerRepositoryId );
    }
}
