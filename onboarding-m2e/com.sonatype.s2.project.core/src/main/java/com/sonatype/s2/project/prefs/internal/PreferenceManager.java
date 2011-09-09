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
package com.sonatype.s2.project.prefs.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.internal.S2ProjectPlugin;
import com.sonatype.s2.project.prefs.IPreferenceManager;
import com.sonatype.s2.project.prefs.PreferenceGroup;

public class PreferenceManager
    implements IPreferenceManager
{
    private final Logger log = LoggerFactory.getLogger( PreferenceManager.class );

    private static final String ECLIPSE_PREFERENCES = "s2.epf";

    private static final String MANIFEST_KEY_PREFIX = "SonatypeStudio-";

    private static final String MANIFEST_WORKSPACE_PREFS = MANIFEST_KEY_PREFIX + "WorkspacePreferences";

    public Set<PreferenceGroup> getPreferenceGroups()
        throws CoreException
    {
        log.debug( "Getting available preference groups" );

        IEclipsePreferences rootNode = Platform.getPreferencesService().getRootNode();

        Set<PreferenceGroup> preferenceGroups = EnumSet.noneOf( PreferenceGroup.class );

        for ( PreferenceGroup preferenceGroup : PreferenceGroup.values() )
        {
            log.debug( "Checking for availability of preference group {}", preferenceGroup );

            IPreferenceGroup group = getGroup( preferenceGroup );
            try
            {
                if ( group.isAvailable( rootNode ) )
                {
                    preferenceGroups.add( preferenceGroup );
                }
            }
            catch ( BackingStoreException e )
            {
                log.warn( "Failed to check availability of preferences for " + preferenceGroup, e );
            }
        }

        log.debug( "Available preference groups {}", preferenceGroups );

        return preferenceGroups;
    }

    public Set<PreferenceGroup> getPreferenceGroups( String preferencesUrl, IProgressMonitor monitor )
        throws CoreException
    {
        log.debug( "Getting preference groups from {}", preferencesUrl );

        SubMonitor progress = SubMonitor.convert( monitor, "Reading available preferences from " + preferencesUrl, 1 );

        Set<PreferenceGroup> preferenceGroups = null;

        try
        {
            InputStream is = S2IOFacade.openStream( preferencesUrl, progress.newChild( 1 ) );

            try
            {
                JarInputStream jis = new JarInputStream( is, false );
                try
                {
                    preferenceGroups = getPreferenceGroups( jis );
                }
                finally
                {
                    jis.close();
                }
            }
            finally
            {
                is.close();
            }
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                 "Failed to retrieve preferences", e ) );
        }
        catch ( URISyntaxException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                 "Invalid download location for preferences", e ) );
        }

        progress.worked( 1 );

        return preferenceGroups == null ? new LinkedHashSet<PreferenceGroup>() : preferenceGroups;
    }

    public Set<PreferenceGroup> getPreferenceGroups( JarInputStream jis )
    {
        return getPreferenceGroups( jis.getManifest() );
    }

    private Set<PreferenceGroup> getPreferenceGroups( Manifest manifest )
    {
        Set<PreferenceGroup> preferenceGroups = new LinkedHashSet<PreferenceGroup>();

        String value = ( manifest != null ) ? manifest.getMainAttributes().getValue( MANIFEST_WORKSPACE_PREFS ) : null;
        if ( value != null )
        {
            for ( String s : value.trim().split( "[ ,]+" ) )
            {
                preferenceGroups.add( PreferenceGroup.valueOf( s ) );
            }
        }

        return preferenceGroups;
    }

    public byte[] getEclipsePreferences( JarInputStream jis )
        throws IOException
    {
        while ( true )
        {
            ZipEntry ze = jis.getNextEntry();
            if ( ze == null )
            {
                break;
            }
            if ( ECLIPSE_PREFERENCES.equals( ze.getName() ) )
            {
                ByteArrayOutputStream os = new ByteArrayOutputStream();

                try
                {
                    byte[] buffer = new byte[0x1000];
                    for ( int n = 0; ( n = jis.read( buffer ) ) > -1; os.write( buffer, 0, n ) )
                        ;
                    return os.toByteArray();
                }
                finally
                {
                    IOUtil.close( os );
                }
            }
        }
        return null;
    }

    public void exportPreferences( File file, Collection<PreferenceGroup> preferences, IProgressMonitor monitor )
        throws CoreException
    {
        try
        {
            file.getParentFile().mkdirs();
            FileOutputStream output = new FileOutputStream( file );
            try
            {
                exportPreferences( output, Collections.singletonMap( "", preferences ), monitor );
            }
            finally
            {
                output.close();
            }
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                 "Failed to export preferences to " + file, e ) );
        }
    }

    public void exportPreferences( OutputStream output, Collection<PreferenceGroup> preferences,
                                   IProgressMonitor monitor )
        throws CoreException
    {
        exportPreferences( output, Collections.singletonMap( "", preferences ), monitor );
    }

    public void exportPreferences( OutputStream output, Map<String, Collection<PreferenceGroup>> preferences,
                                   IProgressMonitor monitor )
        throws CoreException
    {
        log.debug( "Exporting preference groups {}", preferences );

        SubMonitor progress = SubMonitor.convert( monitor, "Exporting preferences", preferences.size() + 1 );

        IPreferencesService prefService = Platform.getPreferencesService();

        IEclipsePreferences rootNode = prefService.getRootNode();

        File preferencesDirectory = getPreferencesDirectory();
        log.debug( "Reading external preference files from directory {}", preferencesDirectory );

        Collection<IPreferenceFilter> filters = new ArrayList<IPreferenceFilter>();

        Collection<String> filenames = new HashSet<String>();

        Manifest mf = new Manifest();
        mf.getMainAttributes().put( Attributes.Name.MANIFEST_VERSION, "1.0" );

        for ( String projectName : preferences.keySet() )
        {
            Collection<? extends PreferenceGroup> preferenceGroups = preferences.get( projectName );
            if ( preferenceGroups == null )
            {
                preferenceGroups = EnumSet.allOf( PreferenceGroup.class );
            }

            SubMonitor subProgress = SubMonitor.convert( progress.newChild( 1 ), preferenceGroups.size() );

            if ( projectName == null || projectName.length() <= 0 )
            {
                String value = preferenceGroups.toString();
                value = value.substring( 1, value.length() - 1 );
                mf.getMainAttributes().putValue( MANIFEST_WORKSPACE_PREFS, value );
            }

            for ( PreferenceGroup preferenceGroup : preferenceGroups )
            {
                log.debug( "Exporting preference group {}", preferenceGroup );

                IPreferenceGroup group = getGroup( preferenceGroup );

                Collections.addAll( filenames, group.getFiles( preferencesDirectory ) );

                try
                {
                    filters.add( group.getFilter( projectName, rootNode ) );
                }
                catch ( BackingStoreException e )
                {
                    throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                         "Failed to export preferences for " + preferenceGroup, e ) );
                }

                subProgress.worked( 1 );
            }
        }

        try
        {
            SubMonitor subProgress = SubMonitor.convert( progress.newChild( 1 ), filenames.size() + 1 );

            JarOutputStream jos = new JarOutputStream( output, mf );

            log.debug( "Exporting preference nodes" );

            jos.putNextEntry( new ZipEntry( ECLIPSE_PREFERENCES ) );
            prefService.exportPreferences( rootNode, filters.toArray( new IPreferenceFilter[filters.size()] ), jos );

            subProgress.worked( 1 );

            for ( String filename : filenames )
            {
                File file = new File( preferencesDirectory, filename );
                if ( file.isFile() )
                {
                    log.debug( "Exporting external preference file {}", filename );

                    jos.putNextEntry( new ZipEntry( filename.replace( '\\', '/' ) ) );
                    FileInputStream fis = new FileInputStream( file );
                    try
                    {
                        copy( fis, jos );
                    }
                    finally
                    {
                        fis.close();
                    }
                }
                else
                {
                    log.debug( "Skipped non-existent external preference file {}", filename );
                }

                subProgress.worked( 1 );
            }

            jos.finish();
            jos.flush();
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                 "Failed to export preferences", e ) );
        }
    }

    private IPreferenceGroup getGroup( PreferenceGroup preferenceGroup )
    {
        switch ( preferenceGroup )
        {
            case PROXY:
                return new ProxyPreferenceGroup();
            case JDT:
                return new JdtPreferenceGroup();
            case M2E:
                return new M2EclipsePreferenceGroup();
        }

        log.debug( "Unknown preference group {}", preferenceGroup );
        return new EmptyPreferenceGroup();
    }

    private File getPreferencesDirectory()
    {
        File dir = S2ProjectPlugin.getDefault().getStateLocation().toFile().getParentFile();
        return dir.getAbsoluteFile();
    }

    private void copy( InputStream is, OutputStream os )
        throws IOException
    {
        for ( byte[] buffer = new byte[1024 * 4];; )
        {
            int n = is.read( buffer );
            if ( n < 0 )
            {
                return;
            }
            os.write( buffer, 0, n );
        }
    }

    public void deployPreferences( File preferencesFile, String deploymentUrl, IProgressMonitor monitor )
        throws CoreException
    {
        log.debug( "Deploying exported preferences from {} to {}", preferencesFile, deploymentUrl );

        SubMonitor progress = SubMonitor.convert( monitor, "Deploying preferences to " + deploymentUrl, 1 );

        try
        {
            S2IOFacade.putFile( preferencesFile, deploymentUrl, progress.newChild( 1, SubMonitor.SUPPRESS_ALL_LABELS ) );
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                 "Failed to deploy preferences", e ) );
        }
        catch ( URISyntaxException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                 "Invalid deployment location for preferences", e ) );
        }
    }

    public void importPreferences( String preferencesUrl, IProgressMonitor monitor )
        throws CoreException
    {
        log.debug( "Importing preferences from {}", preferencesUrl );

        SubMonitor progress = SubMonitor.convert( monitor, "Importing preferences from " + preferencesUrl, 2 );

        try
        {
            InputStream is =
                S2IOFacade.openStream( preferencesUrl, progress.newChild( 1, SubMonitor.SUPPRESS_ALL_LABELS ) );

            File preferencesDirectory = getPreferencesDirectory();
            log.debug( "Writing external preference files to directory {}", preferencesDirectory );

            ZipInputStream zis = new ZipInputStream( is );
            try
            {
                Set<PreferenceGroup> preferenceGroups = null;
                Set<String> importedExternalFiles = new LinkedHashSet<String>();
                while ( true )
                {
                    ZipEntry zipEntry = zis.getNextEntry();
                    if ( zipEntry == null )
                    {
                        break;
                    }

                    log.debug( "Processing export archive entry {}", zipEntry.getName() );

                    if ( zipEntry.getName().startsWith( "META-INF/" ) )
                    {
                        if ( zipEntry.getName().equals( "META-INF/MANIFEST.MF" ) )
                        {
                            Manifest manifest = new Manifest( zis );
                            preferenceGroups = getPreferenceGroups( manifest );
                        }
                        continue;
                    }

                    if ( ECLIPSE_PREFERENCES.equals( zipEntry.getName() ) )
                    {
                        importEclipsePreferences( zis );
                    }
                    else if ( !zipEntry.isDirectory() )
                    {
                        log.debug( "Importing external preference file {}", zipEntry.getName() );

                        File file = new File( preferencesDirectory, zipEntry.getName() );
                        file.getParentFile().mkdirs();
                        FileOutputStream fos = new FileOutputStream( file );
                        try
                        {
                            copy( zis, fos );
                        }
                        finally
                        {
                            fos.close();
                        }
                        importedExternalFiles.add( zipEntry.getName() );
                    }
                }

                if ( preferenceGroups != null )
                {
                    for ( PreferenceGroup preferenceGroup : preferenceGroups )
                    {
                        IPreferenceGroup group = getGroup( preferenceGroup );
                        for ( String file : importedExternalFiles )
                        {
                            group.notifyFileImported( file );
                        }
                    }
                }
            }
            finally
            {
                zis.close();
            }
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                 "Failed to retrieve preferences", e ) );
        }
        catch ( URISyntaxException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                 "Invalid download location for preferences", e ) );
        }

        progress.worked( 1 );
    }

    private void importEclipsePreferences( InputStream is )
        throws IOException, CoreException
    {
        log.debug( "Importing preference nodes" );

        IPreferencesService prefService = Platform.getPreferencesService();

        IEclipsePreferences rootNode = prefService.getRootNode();

        for ( PreferenceGroup preferenceGroup : PreferenceGroup.values() )
        {
            log.debug( "Resetting preference nodes for preference group {}", preferenceGroup );

            IPreferenceGroup group = getGroup( preferenceGroup );
            try
            {
                group.resetPreferences( rootNode );
            }
            catch ( BackingStoreException e )
            {
                throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                     "Failed to reset preferences for " + preferenceGroup, e ) );
            }
        }

        BufferedInputStream bis = new BufferedInputStream( is )
        {
            @Override
            public void close()
                throws IOException
            {
                // leave stream open
            }

        };
        bis.mark( 1 );
        if ( bis.read() < 0 )
        {
            /*
             * NOTE: If no preferences at all were exported, the preference service generates an empty file. The import
             * of this file via the preference service fails as the file is missing the crucial version format key.
             */
            return;
        }
        bis.reset();

        log.debug( "Reading preference nodes to import" );

        IEclipsePreferences importNode = prefService.readPreferences( bis );

        log.debug( "Applying preference nodes to import" );

        /*
         * NOTE: applyPreferences(IExportedPreferences) removes keys not present in the imported node, that's why we use
         * the overload with the filter which does not exhibit this behavior and leaves non-imported keys unchanged.
         */
        IPreferenceFilter filter = new IPreferenceFilter()
        {

            public String[] getScopes()
            {
                return new String[] { InstanceScope.SCOPE, ConfigurationScope.SCOPE };
            }

            @SuppressWarnings( "unchecked" )
            public Map getMapping( String scope )
            {
                return null;
            }

        };
        prefService.applyPreferences( importNode, new IPreferenceFilter[] { filter } );
    }

}
