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
package com.sonatype.s2.project.integration.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.team.core.RepositoryProvider;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.util.tracker.ServiceTracker;

import com.sonatype.s2.project.core.IS2ProjectCatalogRegistry;
import com.sonatype.s2.project.core.IS2ProjectCatalogRegistryEntry;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.internal.S2ProjectCatalogRegistry;
import com.sonatype.s2.project.core.internal.S2ProjectPlugin;
import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.IS2ProjectCatalog;
import com.sonatype.s2.project.model.IS2ProjectCatalogEntry;
import com.sonatype.s2.project.validation.api.IS2ProjectValidationStatus;
import com.sonatype.s2.project.validation.api.UnauthorizedStatus;
import com.sonatype.s2.project.validator.ScmAccessValidator;
import com.sonatype.s2.ssh.SshHandler;
import com.sonatype.s2.ssh.SshHandlerManager;

public abstract class AbstractMavenProjectMaterializationTest
    extends com.sonatype.s2.project.core.test.AbstractMavenProjectMaterializationTest
{
    private List<HttpServer> httpServers;

    private File basedir = new File( "" ).getAbsoluteFile();

    private ServiceTracker proxyServiceTracker;

    private File prefBackupFile;

    private File oldSshDir;

    private SshHandler sshHandler;

    protected File getBasedir()
    {
        return basedir;
    }

    protected String getBaseUrl()
    {
        String url = "file://" + getBasedir().toURI().getPath();
        return url;
    }

    protected String getBaseUri()
    {
        String url = "file://" + getBasedir().toURI().getRawPath();
        return url;
    }

    protected HttpServer newHttpServer()
    {
        if ( httpServers == null )
        {
            httpServers = new ArrayList<HttpServer>();
        }

        HttpServer httpServer = new HttpServer();
        httpServer.addResources( "/", "resources", "xml", "properties" );
        httpServers.add( httpServer );

        return httpServer;
    }

    protected HttpServer startHttpServer()
        throws Exception
    {
        HttpServer httpServer = newHttpServer();
        httpServer.start();
        return httpServer;
    }

    protected IProxyService getProxyService()
    {
        return ( proxyServiceTracker != null ) ? (IProxyService) proxyServiceTracker.getService() : null;
    }

    protected void setNonProxiedHosts( String... hosts )
        throws Exception
    {
        IProxyService proxyService = getProxyService();
        if ( proxyService != null )
        {
            proxyService.setNonProxiedHosts( hosts );
        }
    }

    protected void setProxy( String host, int port, boolean ssl, String username, String password )
        throws Exception
    {
        IProxyService proxyService = getProxyService();
        if ( proxyService != null )
        {
            IProxyData data =
                new ProxyData( ssl ? IProxyData.HTTPS_PROXY_TYPE : IProxyData.HTTP_PROXY_TYPE, host, port, username,
                               password );
            proxyService.setProxyData( new IProxyData[] { data } );
        }
    }

    protected S2ProjectCore getProjectCore()
    {
        return S2ProjectCore.getInstance();
    }

    protected IS2ProjectCatalogRegistry getCatalogRegistry()
    {
        return getProjectCore().getProjectCatalogRegistry();
    }

    protected void addRealmAndURL( String realmId, String url, AuthenticationType type, AnonymousAccessType anonType )
    {
        IAuthRealm realm = AuthFacade.getAuthRegistry().addRealm( realmId, realmId, realmId, type, monitor );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( url, realm.getId(), anonType, monitor );
    }

    protected void addRealmAndURL( String realmId, String url, String username, String password )
    {
        addRealmAndURL( realmId, url, AuthenticationType.USERNAME_PASSWORD, AnonymousAccessType.ALLOWED );
        AuthFacade.getAuthService().save( url, username, password );
    }

    protected void addRealmAndURL( String realmId, String url, File certificate, String passphrase )
    {
        addRealmAndURL( realmId, url, AuthenticationType.CERTIFICATE, AnonymousAccessType.ALLOWED );
        AuthFacade.getAuthService().save( url, certificate, passphrase );
    }

    protected IS2Project[] materializeProjects( String catalogUrl, String... projectNames )
        throws Exception
    {
        return materializeProjects( catalogUrl, true, projectNames );
    }

    protected IS2Project[] materializeProjects( String catalogUrl, boolean preferences, String... projectNames )
        throws Exception
    {
        addCatalog( catalogUrl );

        IS2ProjectCatalogRegistryEntry registryEntry = getRegistryEntry( catalogUrl );
        assertNotNull( registryEntry );
        assertTrue( "Catalog " + registryEntry.getUrl() + " not loaded: " + registryEntry.getStatus(),
                    registryEntry.isLoaded() );

        Collection<IS2Project> projects = new ArrayList<IS2Project>();

        for ( String projectName : projectNames )
        {
            IS2ProjectCatalogEntry catalogEntry = getCatalogEntry( registryEntry.getCatalog(), projectName );
            assertNotNull( catalogEntry );

            IS2Project project = getProjectCore().loadProject( catalogEntry, monitor );
            projects.add( project );
            getProjectCore().materialize( project, preferences, new ConsoleProgressMonitor() );
        }

        return projects.toArray( new IS2Project[projects.size()] );
    }

    protected IS2Project materializeProject( String catalogUrl, String projectName )
        throws Exception
    {
        return materializeProject( catalogUrl, true, projectName );
    }

    protected IS2Project materializeProject( String catalogUrl, boolean preferences, String projectName )
        throws Exception
    {
        return materializeProjects( catalogUrl, preferences, projectName )[0];
    }

    protected IS2ProjectValidationStatus validateScmAccess( String projectUrl )
        throws Exception
    {
        IS2Project project = getProjectCore().loadProject( projectUrl, monitor );
        ScmAccessValidator validator = new ScmAccessValidator();
        return validator.validate( project, new ConsoleProgressMonitor() );
    }

    protected IS2Project validateScmAccessAndMaterialize( String projectUrl )
        throws Exception
    {
        IS2Project codebase = getProjectCore().loadProject( projectUrl, new ConsoleProgressMonitor() );

        ScmAccessValidator validator = new ScmAccessValidator();
        IStatus validationStatus = validator.validate( codebase, new ConsoleProgressMonitor() );
        assertTrue( validationStatus.toString(), validationStatus.isOK() );

        getProjectCore().materialize( codebase, true /* preferences */, new ConsoleProgressMonitor() );
        return codebase;
    }

    protected IS2ProjectCatalogRegistryEntry getRegistryEntry( String catalogUrl )
        throws Exception
    {
        IS2ProjectCatalogRegistry registry = getCatalogRegistry();

        for ( IS2ProjectCatalogRegistryEntry entry : registry.getCatalogEntries( new NullProgressMonitor() ) )
        {
            if ( catalogUrl.equals( entry.getUrl() ) )
            {
                return entry;
            }
        }

        return null;
    }

    protected IS2ProjectCatalogEntry getCatalogEntry( IS2ProjectCatalog catalog, String projectName )
    {
        for ( IS2ProjectCatalogEntry catalogEntry : catalog.getEntries() )
        {
            if ( projectName.equals( catalogEntry.getName() ) )
            {
                return catalogEntry;
            }
        }
        return null;
    }

    protected IS2ProjectCatalogRegistryEntry addCatalog( String url )
        throws Exception
    {
        return addCatalogs( url )[0];
    }

    protected IS2ProjectCatalogRegistryEntry[] addCatalogs( String... urls )
        throws Exception
    {
        final List<IS2ProjectCatalogRegistryEntry> entries = new CopyOnWriteArrayList<IS2ProjectCatalogRegistryEntry>();

        IS2ProjectCatalogRegistry registry = getCatalogRegistry();
        for ( String url : urls )
        {
            entries.add( ((S2ProjectCatalogRegistry) registry).addCatalog( url, monitor ) );
        }

        return entries.toArray( new IS2ProjectCatalogRegistryEntry[entries.size()] );
    }

    protected void assertCatalogsLoaded( String... urls )
        throws Exception
    {
        Collection<String> set = new HashSet<String>();
        for ( String url : urls )
        {
            set.add( url );
        }

        IS2ProjectCatalogRegistry registry = getCatalogRegistry();

        for ( IS2ProjectCatalogRegistryEntry entry : registry.getCatalogEntries( new NullProgressMonitor() ) )
        {
            if ( set.contains( entry.getUrl() ) )
            {
                assertTrue( "Catalog " + entry.getUrl() + " not loaded: " + entry.getStatus(), entry.isLoaded() );
            }
        }
    }

    protected void assertMavenProjects( int count )
    {
        MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
        assertEquals( count, projectManager.getProjects().length );
    }

    protected void assertMavenProject( String groupId, String artifactId, String version )
    {
        MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
        assertNotNull( projectManager.getMavenProject( groupId, artifactId, version ) );
    }

    protected void assertWorkspaceProjects( int count )
    {
        assertEquals( count, getWorkspaceProjects().length );
    }

    protected void assertWorkspaceProject( String projectName )
    {
        IProject project = getWorkspaceProject( projectName );
        assertNotNull( project );
        assertTrue( project.isAccessible() );
    }

    protected void assertWorkspaceProjectShared( String projectName )
        throws Exception
    {
        IProject project = getWorkspaceProject( projectName );
        assertNotNull( project );
        assertTrue( "Expected accessible project:" + projectName, project.isAccessible() );
        assertTrue( "Expected shared project:" + projectName, RepositoryProvider.isShared( project ) );
        RepositoryProvider provider = RepositoryProvider.getProvider( project );
        assertNotNull( provider );
    }

    protected IProject getWorkspaceProject( String projectName )
    {
        // NOTE: IWorkspaceRoot.getProject(String) creates new/missing projects which is not desired here
        IProject[] projects = workspace.getRoot().getProjects();
        for ( IProject project : projects )
        {
            if ( project.getName().equals( projectName ) )
            {
                return project;
            }
        }
        return null;
    }

    protected IProject[] getWorkspaceProjects()
    {
        return workspace.getRoot().getProjects();
    }

    protected void assertErrorStatus( IStatus status, String errorMessageSubstring )
    {
        assertFalse( status.toString(), status.isOK() );
        assertEquals( status.toString(), IStatus.ERROR, status.getSeverity() );
        assertTrue( status.toString(), status.toString().contains( errorMessageSubstring ) );
    }

    protected boolean isUnauthorizedStatus( IStatus status )
    {
        if ( status instanceof UnauthorizedStatus )
        {
            return true;
        }
        if ( status.isMultiStatus() )
        {
            for ( IStatus child : status.getChildren() )
            {
                if ( isUnauthorizedStatus( child ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void setUp()
        throws Exception
    {
        System.out.println( "TEST-SETUP: " + getName() );

        super.setUp();

        SshHandlerManager sshManager = SshHandlerManager.getInstance();
        sshHandler = new SimpleSshHandler();
        sshManager.addSshHandler( sshHandler );
        oldSshDir = sshManager.getSshDirectory();

        clearCatalogRegistry();

        proxyServiceTracker =
            new ServiceTracker( S2ProjectPlugin.getDefault().getBundle().getBundleContext(),
                                IProxyService.class.getName(), null );
        proxyServiceTracker.open();

        resetProxies();

        backupPreferences();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            stopHttpServers();

            AuthFacade.getAuthRegistry().clear();

            clearCatalogRegistry();

            resetProxies();

            resetPreferences();

            if ( proxyServiceTracker != null )
            {
                proxyServiceTracker.close();
                proxyServiceTracker = null;
            }

            SshHandlerManager sshManager = SshHandlerManager.getInstance();
            sshManager.setSshDirectory( oldSshDir );
            sshManager.removeSshHandler( sshHandler );
            sshHandler = null;
        }
        finally
        {
            super.tearDown();
        }
    }

    protected void stopHttpServers()
        throws Exception
    {
        if ( httpServers != null )
        {
            for ( HttpServer httpServer : httpServers )
            {
                httpServer.stop();
            }
            httpServers = null;
        }
    }

    protected void clearCatalogRegistry()
        throws Exception
    {
        Collection<String> catalogUrls = new HashSet<String>();
        for ( IS2ProjectCatalogRegistryEntry catalogEntry : getCatalogRegistry().getCatalogEntries( monitor ) )
        {
            catalogUrls.add( catalogEntry.getUrl() );
        }
        for ( String catalogUrl : catalogUrls )
        {
            getCatalogRegistry().removeCatalog( catalogUrl );
        }
    }

    protected void resetProxies()
        throws Exception
    {
        IProxyService proxyService = getProxyService();
        if ( proxyService != null )
        {
            proxyService.setSystemProxiesEnabled( false );
            proxyService.setNonProxiedHosts( new String[0] );
            proxyService.setProxyData( new IProxyData[] { new ProxyData( IProxyData.HTTP_PROXY_TYPE ),
                new ProxyData( IProxyData.HTTPS_PROXY_TYPE ) } );
        }
    }

    protected void backupPreferences()
        throws Exception
    {
        prefBackupFile = File.createTempFile( "s2pref", ".bak" );
        prefBackupFile.deleteOnExit();

        IPreferencesService prefService = Platform.getPreferencesService();

        FileOutputStream os = new FileOutputStream( prefBackupFile );
        try
        {
            String[] excludes = { '/' + ConfigurationScope.SCOPE };
            prefService.exportPreferences( prefService.getRootNode(), os, excludes );
        }
        finally
        {
            os.close();
        }
    }

    protected void resetPreferences()
        throws Exception
    {
        if ( prefBackupFile != null && prefBackupFile.isFile() )
        {
            IPreferencesService prefService = Platform.getPreferencesService();

            IPreferenceFilter filter = new IPreferenceFilter()
            {

                public String[] getScopes()
                {
                    return new String[] { InstanceScope.SCOPE };
                }

                @SuppressWarnings( "unchecked" )
                public Map getMapping( String scope )
                {
                    return null;
                }

            };

            FileInputStream is = new FileInputStream( prefBackupFile );
            // clear anything but the settings preferences for m2e or the indexer jobs will kill us
            IPreferenceNodeVisitor visitor = new IPreferenceNodeVisitor()
            {
                private static final String INSTANCE_SCOPE = '/' + InstanceScope.SCOPE;

                public boolean visit( IEclipsePreferences node )
                    throws BackingStoreException
                {
                    String absPath = node.absolutePath();
                    if ( INSTANCE_SCOPE.startsWith( absPath ) )
                    {
                        return true;
                    }
                    else if ( !absPath.startsWith( INSTANCE_SCOPE )
                        || absPath.startsWith( INSTANCE_SCOPE + "/org.eclipse.core" )
                        || absPath.startsWith( INSTANCE_SCOPE + "/org.eclipse.team.svn.core" ) )
                    {
                        return false;
                    }
                    else if ( absPath.equals( INSTANCE_SCOPE + "/org.maven.ide.eclipse" ) )
                    {
                        for ( String key : node.keys() )
                        {
                            if ( !key.endsWith( "SettingsFile" ) )
                            {
                                node.remove( key );
                            }
                        }
                        return true;
                    }

                    node.removeNode();

                    return false;
                }

            };
            prefService.getRootNode().accept( visitor );
            IEclipsePreferences prefs = prefService.readPreferences( is );
            prefService.applyPreferences( prefs, new IPreferenceFilter[] { filter } );
        }
    }

    protected String getPreference( String node, String key )
    {
        IPreferencesService prefService = Platform.getPreferencesService();
        return prefService.getRootNode().node( '/' + InstanceScope.SCOPE + '/' + node ).get( key, null );
    }

    protected File getPreferencesLocation( String bundleName )
    {
        File dir = S2ProjectPlugin.getDefault().getStateLocation().toFile().getParentFile();
        return new File( dir, bundleName );
    }

}
