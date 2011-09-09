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
package com.sonatype.s2.project.core;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.spice.interactive.interpolation.Interpolator;

import com.sonatype.s2.project.common.S2ProjectCommon;
import com.sonatype.s2.project.core.internal.ModulesAddRemoveJob;
import com.sonatype.s2.project.core.internal.S2CodebaseRegistry;
import com.sonatype.s2.project.core.internal.S2ProjectCatalogRegistry;
import com.sonatype.s2.project.core.internal.S2ProjectPlugin;
import com.sonatype.s2.project.core.internal.WorkspaceCodebase;
import com.sonatype.s2.project.core.internal.WorkspaceSourceTree;
import com.sonatype.s2.project.core.internal.update.SourceTreeImportOperation;
import com.sonatype.s2.project.model.IResourceLocation;
import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.IS2ProjectCatalogEntry;
import com.sonatype.s2.project.prefs.IPreferenceManager;
import com.sonatype.s2.project.prefs.internal.PreferenceManager;

public class S2ProjectCore
{
    private static final Logger log = LoggerFactory.getLogger( S2ProjectCore.class );

    /**
     * do not make this field final! it is being reset in com.sonatype.s2.project.tests.common.Util.resetProjectCore for
     * reasons that are beyond me
     */
    private static S2ProjectCore instance = new S2ProjectCore();

    private S2ProjectCatalogRegistry projectCatalogRegistry;

    private S2CodebaseRegistry codebaseRegistry;

    private IPreferenceManager prefManager = new PreferenceManager();

    private final List<IS2CodebaseChangeEventListener> dynaListeners = new ArrayList<IS2CodebaseChangeEventListener>();

    private final ModulesAddRemoveJob modulesUpdateJob = new ModulesAddRemoveJob();

    public static S2ProjectCore getInstance()
    {
        return instance;
    }

    public void materialize( IS2Project descriptor, boolean preferences, IProgressMonitor monitor )
        throws CoreException, InterruptedException
    {
        log.debug( "Materializing project {}", descriptor.getName() );

        SubMonitor progress = SubMonitor.convert( monitor, "Materializing project " + descriptor.getName(), 100 );

        WorkspaceCodebase codebase = S2CodebaseRegistry.createCodebase( descriptor );

        long start = System.currentTimeMillis();

        // validate( s2project, monitor, true );

        configureMavenSettings( descriptor, progress );
        progress.setWorkRemaining( 95 );

        if ( preferences && descriptor.getEclipsePreferencesLocation() != null
            && descriptor.getEclipsePreferencesLocation().getUrl() != null )
        {
            String url = descriptor.getEclipsePreferencesLocation().getUrl();
            getPrefManager().importPreferences( url, progress.newChild( 5, SubMonitor.SUPPRESS_NONE ) );
        }
        progress.setWorkRemaining( 90 );

        List<IS2Module> modules = descriptor.getModules();

        SubMonitor subProgress =
            SubMonitor.convert( progress.newChild( 50, SubMonitor.SUPPRESS_NONE ), modules.size() * 100 );

        for ( final IS2Module module : modules )
        {
            if ( module.getScmLocation() != null && module.getScmLocation().getUrl() != null )
            {
                WorkspaceSourceTree sourceTree =
                    S2CodebaseRegistry.createSourceTree( module, SourceTreeImportOperation.getModuleLocation( module ) );
                SourceTreeImportOperation op = new SourceTreeImportOperation( sourceTree );
                op.run( subProgress );
                codebase.addSourceTree( sourceTree );
            }
        }

        replaceWorkspaceCodebase( null, codebase );

        log.debug( "Materialized project {} in {} ms", descriptor.getName(), System.currentTimeMillis() - start );
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     */
    public synchronized S2CodebaseRegistry getCodebaseRegistry()
    {
        if ( codebaseRegistry == null )
        {
            codebaseRegistry = new S2CodebaseRegistry( getStateLocation() );
        }

        return codebaseRegistry;
    }

    protected void notifyCodebaseChangeListeners( IWorkspaceCodebase oldCodebase, IWorkspaceCodebase codebase )
    {
        S2CodebaseChangeEvent event = new S2CodebaseChangeEvent( oldCodebase, codebase );

        log.debug( "Notifying codebase change listeners" );
        for ( IS2CodebaseChangeEventListener listener : getCodebaseChangeListeners() )
        {
            listener.codebaseChanged( event );
        }
    }

    private static final char[] INVALID_CHARACTERS_FOR_WINDOWS_FILE_NAME = { ':', '*', '?', '"', '<', '>', '|', '\\',
        '/' };

    private String string2LegalFileName( String s )
    {
        if ( s == null )
        {
            return "";
        }
        for ( char illegalChar : INVALID_CHARACTERS_FOR_WINDOWS_FILE_NAME )
        {
            s = s.replace( illegalChar, '_' );
        }
        return s;
    }

    protected void configureMavenSettings( IS2Project s2project, SubMonitor progress )
        throws CoreException
    {
        MavenPlugin mavenPlugin = MavenPlugin.getDefault();
        IMavenConfiguration mavenConfiguration = mavenPlugin.getMavenConfiguration();

        if ( s2project.getMavenSettingsLocation() != null )
        {
            File state = new File( getStateLocation(), "projects/" + string2LegalFileName( s2project.getName() ) );
            state.mkdirs();

            log.debug( "Downloading Maven settings from {} to {}", s2project.getMavenSettingsLocation().getUrl(), state );

            byte[] settings =
                loadMavenSettings( s2project.getMavenSettingsLocation(),
                                   progress.newChild( 5, SubMonitor.SUPPRESS_NONE ) );

            File settingsFile = new File( state, "settings.xml" );

            try
            {
                OutputStream os = new BufferedOutputStream( new FileOutputStream( settingsFile ) );
                try
                {
                    IOUtil.copy( settings, os );
                }
                finally
                {
                    IOUtil.close( os );
                }
            }
            catch ( IOException e )
            {
                throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                     "Could not write project settings.xml", e ) );
            }

            if ( mavenConfiguration.getUserSettingsFile() != null
                && mavenConfiguration.getUserSettingsFile().trim().length() > 0 )
            {
                log.info( "Non-default user settings.xml {}", mavenConfiguration.getUserSettingsFile() );
            }

            interpolateMavenSettings( settingsFile );
            mavenConfiguration.setUserSettingsFile( settingsFile.getAbsolutePath() );
        }

        log.debug( "Using Maven settings {}", mavenConfiguration.getUserSettingsFile() );
    }

    protected File getStateLocation()
    {
        return S2ProjectPlugin.getDefault().getStateLocation().toFile();
    }

    private List<IS2CodebaseChangeEventListener> getCodebaseChangeListeners()
    {
        ArrayList<IS2CodebaseChangeEventListener> listeners = new ArrayList<IS2CodebaseChangeEventListener>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint extensionPoint =
            registry.getExtensionPoint( "com.sonatype.s2.project.core.codebaseChangeEventListeners" );
        if ( extensionPoint != null )
        {
            for ( IExtension extension : extensionPoint.getExtensions() )
            {
                for ( IConfigurationElement element : extension.getConfigurationElements() )
                {
                    if ( "listener".equals( element.getName() ) )
                    {
                        try
                        {
                            listeners.add( (IS2CodebaseChangeEventListener) element.createExecutableExtension( "class" ) );
                        }
                        catch ( CoreException e )
                        {
                            log.debug( "Could not create codebase change listener", e );
                        }
                    }
                }
            }
        }
        synchronized ( dynaListeners )
        {
            listeners.addAll( dynaListeners );
        }

        return listeners;
    }

    public void addWorkspaceCodebaseChangeListener( IS2CodebaseChangeEventListener listener )
    {
        synchronized ( dynaListeners )
        {
            dynaListeners.add( listener );
        }
    }

    public void removeWorkspaceCodebaseChangeListener( IS2CodebaseChangeEventListener listener )
    {
        synchronized ( dynaListeners )
        {
            dynaListeners.remove( listener );
        }

    }

    private File getMavenSettingsDefaultValuesFile()
        throws CoreException
    {
        String userHome = System.getProperty( "user.home" );
        if ( userHome == null || userHome.trim().length() == 0 )
        {
            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                 "The user.home java property is null or empty." ) );
        }

        return new File( userHome, ".m2/settings-defaults.properties" );
    }

    private void interpolateMavenSettings( File settingsFile )
        throws CoreException
    {
        File valuesFile = getMavenSettingsDefaultValuesFile();
        if ( !valuesFile.exists() )
        {
            log.warn( "Maven settings default values file does not exist {}", valuesFile.getAbsolutePath() );
            return;
        }
        log.debug( "Found Maven settings default values file {}", valuesFile.getAbsolutePath() );

        Interpolator interpolator = new Interpolator( settingsFile, valuesFile );
        interpolator.replaceVariables();
    }

    public IS2Project loadProject( IS2ProjectCatalogEntry catalogEntry, IProgressMonitor monitor )
        throws CoreException
    {
        String urlStr = getProjectCatalogRegistry().getEffectiveDescriptorUrl( catalogEntry );

        return loadProject( urlStr, monitor );
    }

    public IS2Project loadProject( String urlStr, IProgressMonitor monitor )
        throws CoreException
    {
        log.debug( "Loading s2 project file: {}", urlStr );
        SubMonitor progress = SubMonitor.convert( monitor, "Loading project descriptor " + urlStr, 1 );
        String message = null;
        Exception cause = null;
        try
        {
            IS2Project project;

            InputStream is = S2IOFacade.openStream( urlStr, progress.newChild( 1 ) );
            try
            {
                project = S2ProjectCommon.loadProject( is, true /* validate */);
            }
            finally
            {
                IOUtil.close( is );
            }

            project.setDescriptorUrl( urlStr );

            log.debug( "Loaded s2 project: {}", project.getName() );
            return project;
        }
        catch ( IOException e )
        {
            message = "Error reading project descriptor " + urlStr;
            cause = e;
        }
        catch ( URISyntaxException e )
        {
            message = "Invalid URL " + urlStr;
            cause = e;
        }
        log.error( message, cause );
        throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID, message, cause ) );
    }

    public byte[] loadMavenSettings( IResourceLocation mavenSettingsLocation, IProgressMonitor monitor )
        throws CoreException
    {
        String urlStr = mavenSettingsLocation.getUrl();
        SubMonitor progress = SubMonitor.convert( monitor, "Loading Maven settings " + urlStr, 1 );
        String message = null;
        Exception cause = null;
        try
        {
            InputStream is = S2IOFacade.openStream( urlStr, progress.newChild( 1 ) );
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            try
            {
                byte[] buffer = new byte[0x1000];
                for ( int n = 0; ( n = is.read( buffer ) ) > -1; os.write( buffer, 0, n ) )
                    ;
                return os.toByteArray();
            }
            finally
            {
                IOUtil.close( is );
                IOUtil.close( os );
            }
        }
        catch ( IOException e )
        {
            message = "Error reading Maven settings " + urlStr;
            cause = e;
        }
        catch ( URISyntaxException e )
        {
            message = "Invalid URL " + urlStr;
            cause = e;
        }
        log.error( message, cause );
        throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID, message, cause ) );
    }

    public synchronized IS2ProjectCatalogRegistry getProjectCatalogRegistry()
    {
        if ( projectCatalogRegistry == null )
        {
            File stateLocation = getStateLocation();

            projectCatalogRegistry = new S2ProjectCatalogRegistry( stateLocation );
        }

        return projectCatalogRegistry;
    }

    public IPreferenceManager getPrefManager()
    {
        return prefManager;
    }

    /**
     * @return list of codebases, never null
     */
    public List<IWorkspaceCodebase> getWorkspaceCodebases()
    {
        return getCodebaseRegistry().getCodebases();
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     */
    public void replaceWorkspaceCodebase( IWorkspaceCodebase originalCodebase, IWorkspaceCodebase newCodebase )
        throws CoreException
    {
        WorkspaceCodebase codebase = (WorkspaceCodebase) newCodebase;

        IS2Project newS2Project = codebase.getS2Project();
        if ( newS2Project == null )
        {
            throw new IllegalArgumentException();
        }

        for ( Iterator<IWorkspaceSourceTree> treeIter = codebase.getSourceTrees().iterator(); treeIter.hasNext(); )
        {
            IWorkspaceSourceTree tree = treeIter.next();

            if ( IWorkspaceSourceTree.STATUS_UPTODATE.equals( tree.getStatus() ) )
            {
                ( (WorkspaceSourceTree) tree ).setStatus( null );
            }
            else if ( IWorkspaceSourceTree.STATUS_REMOVED.equals( tree.getStatus() ) )
            {
                treeIter.remove();
            }
        }

        S2CodebaseRegistry registry = getCodebaseRegistry();

        registry.replaceCodebase( (WorkspaceCodebase) originalCodebase, codebase, newS2Project  );
        registry.save();

        notifyCodebaseChangeListeners( originalCodebase, codebase );
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     */
    public ModulesAddRemoveJob getModulesAddRemoveJob()
    {
        return modulesUpdateJob;
    }
}
