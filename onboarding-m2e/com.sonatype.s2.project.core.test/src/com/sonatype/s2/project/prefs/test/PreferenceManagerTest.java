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
package com.sonatype.s2.project.prefs.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.archetype.ArchetypeManager;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.sonatype.s2.project.core.internal.S2ProjectPlugin;
import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.prefs.IPreferenceManager;
import com.sonatype.s2.project.prefs.PreferenceGroup;
import com.sonatype.s2.project.prefs.internal.M2EclipsePreferenceGroup;
import com.sonatype.s2.project.prefs.internal.PreferenceManager;

public class PreferenceManagerTest
    extends TestCase
{

    private static final String NET = "org.eclipse.core.net";

    private static final String JDT_CORE = "org.eclipse.jdt.core";

    private static final String JDT_UI = "org.eclipse.jdt.ui";

    private static final String M2E = M2EclipsePreferenceGroup.M2E_ID;

    private static final String M2E_XML = M2EclipsePreferenceGroup.M2E_XML_ID;

    private IPreferenceManager prefMan;

    private File prefFile;

    private Preferences netNode;

    @Override
    protected void setUp()
        throws Exception
    {
    	// Sanity check
        MavenPlugin.getDefault().getMavenConfiguration().getUserSettingsFile();
        
        super.setUp();

        prefFile = File.createTempFile( "s2pref", ".epf" );

        IPreferencesService prefService = Platform.getPreferencesService();

        netNode = prefService.getRootNode().node( '/' + ConfigurationScope.SCOPE + "/org.eclipse.core.net" );

        FileOutputStream os = new FileOutputStream( prefFile );
        try
        {
            prefService.exportPreferences( prefService.getRootNode(), os, null );
            os.flush();
        }
        finally
        {
            IOUtil.close( os );
        }

        clearPrefs();

        prefMan = new PreferenceManager();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            prefMan = null;

            if ( prefFile != null && prefFile.isFile() )
            {
                try
                {
                    clearPrefs();

                    IPreferenceFilter filter = new IPreferenceFilter()
                    {
                        public String[] getScopes()
                        {
                            return new String[] { ConfigurationScope.SCOPE, InstanceScope.SCOPE, DefaultScope.SCOPE };
                        }

                        @SuppressWarnings( "unchecked" )
                        public Map getMapping( String scope )
                        {
                            return null;
                        }

                    };

                    IPreferencesService prefService = Platform.getPreferencesService();
                    FileInputStream is = new FileInputStream( prefFile );
                    try
                    {
                        IEclipsePreferences importNode = prefService.readPreferences( is );
                        prefService.applyPreferences( importNode, new IPreferenceFilter[] { filter } );
                    }
                    finally
                    {
                        IOUtil.close( is );
                    }
                }
                finally
                {
                    prefFile.delete();
                    prefFile = null;
                }
            }

            if ( netNode != null )
            {
                /*
                 * NOTE: This is a sanity check to make sure some nodes (e.g. the proxy prefs) are not removed during
                 * testing as this would cause subtle but severe defects to other plugins.
                 */
                netNode.keys();
                netNode = null;
            }
            // Sanity check
            MavenPlugin.getDefault().getMavenConfiguration().getUserSettingsFile();
        }
        finally
        {
            super.tearDown();
        }
    }

    private IEclipsePreferences getRoot()
    {
        return Platform.getPreferencesService().getRootNode();
    }

    private IEclipsePreferences getConfiguration()
    {
        return (IEclipsePreferences) getRoot().node( '/' + ConfigurationScope.SCOPE );
    }

    private IEclipsePreferences getInstance()
    {
        return (IEclipsePreferences) getRoot().node( '/' + InstanceScope.SCOPE );
    }

    private IEclipsePreferences getDefault()
    {
        return (IEclipsePreferences) getRoot().node( '/' + DefaultScope.SCOPE );
    }

    private File getPrefDir( String bundleName )
    {
        return new File( S2ProjectPlugin.getDefault().getStateLocation().toFile().getParentFile(), bundleName );
    }

    private void clearPrefs()
        throws Exception
    {
        IPreferencesService prefService = Platform.getPreferencesService();
        prefService.getRootNode().accept( new IPreferenceNodeVisitor()
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
                    || absPath.startsWith( INSTANCE_SCOPE + "/org.eclipse.core" ) )
                {
                    return false;
                }

                node.removeNode();

                return false;
            }
        } );
    }

    private File exportPrefs( PreferenceGroup... groups )
        throws Exception
    {
        File tempFile = File.createTempFile( "s2pref", ".spa" );
        tempFile.deleteOnExit();

        FileOutputStream os = new FileOutputStream( tempFile );
        try
        {
            prefMan.exportPreferences( os, Arrays.asList( groups ), null );
        }
        finally
        {
            os.close();
        }

        return tempFile;
    }

    private void importPrefs( File file )
        throws Exception
    {
        prefMan.importPreferences( file.toURI().toString(), null );
    }

    private void write( File file, String text )
        throws Exception
    {
        FileOutputStream fos = new FileOutputStream( file );
        try
        {
            fos.write( text.getBytes( "UTF-8" ) );
        }
        finally
        {
            fos.close();
        }
    }

    private String read( File file )
        throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        FileInputStream fis = new FileInputStream( file );
        try
        {
            byte[] buffer = new byte[1024];
            while ( true )
            {
                int n = fis.read( buffer );
                if ( n < 0 )
                {
                    break;
                }
                baos.write( buffer, 0, n );
            }
        }
        finally
        {
            fis.close();
        }

        return baos.toString( "UTF-8" );
    }

    public void testGetPreferenceGroups()
        throws Exception
    {
        Collection<PreferenceGroup> groups = prefMan.getPreferenceGroups();
        assertTrue( groups.size() >= 1 );
        assertTrue( groups.contains( PreferenceGroup.JDT ) );

        getConfiguration().node( NET );
        getInstance().node( JDT_CORE );
        getInstance().node( M2E );

        groups = prefMan.getPreferenceGroups();
        assertEquals( 3, groups.size() );
        assertTrue( groups.contains( PreferenceGroup.JDT ) );
        assertTrue( groups.contains( PreferenceGroup.M2E ) );
        assertTrue( groups.contains( PreferenceGroup.PROXY ) );
    }

    public void testImportDoesNotRemoveNonExportedKeys()
        throws Exception
    {
        getInstance().node( M2E ).removeNode();

        Preferences prefs = getInstance().node( M2E );
        prefs.put( "eclipse.m2.offline", "test-TEST" );
        prefs.put( "eclipse.m2.globalSettingsFile", "not-exported" );
        prefs.put( "eclipse.m2.userSettingsFile", "not-exported" );

        File file = exportPrefs( PreferenceGroup.M2E );

        prefs.put( "eclipse.m2.offline", "failed" );
        prefs.put( "eclipse.m2.globalSettingsFile", "test-TEST" );
        prefs.put( "eclipse.m2.userSettingsFile", "test-TEST" );

        importPrefs( file );

        prefs = getInstance().node( M2E );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.offline", null ) );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.globalSettingsFile", null ) );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.userSettingsFile", null ) );
    }

    public void testImportResetsDefaultValues()
        throws Exception
    {
        getInstance().node( M2E ).removeNode();
        getDefault().node( M2E ).put( "eclipse.m2.offline", "test-TEST" );

        File file = exportPrefs( PreferenceGroup.M2E );

        getInstance().node( M2E ).put( "eclipse.m2.offline", "failed" );
        getInstance().node( M2E + "/subnode" ).put( "test-key", "failed" );

        importPrefs( file );

        assertEquals( null, getInstance().node( M2E ).get( "eclipse.m2.offline", null ) );
        assertEquals( null, getInstance().node( M2E + "/subnode" ).get( "test-key", null ) );
    }

    public void testImportDoesNotChokeOnEmptyPreferences()
        throws Exception
    {
        getInstance().node( M2E ).removeNode();

        File file = exportPrefs( PreferenceGroup.M2E );

        importPrefs( file );
    }

    public void testExportImportM2E()
        throws Exception
    {
        ArchetypeManager archetypeManager = MavenPlugin.getDefault().getArchetypeManager();
        Collection<ArchetypeCatalogFactory> archetypeCatalogFactories = archetypeManager.getArchetypeCatalogs();

        File archetypeCatalog = new File( getPrefDir( M2E ), MavenPlugin.PREFS_ARCHETYPES );
        File nonExported = new File( getPrefDir( M2E ), "dialog_settings.xml" );

        getInstance().node( M2E ).removeNode();
        getInstance().node( M2E_XML ).removeNode();
        String catalogsContent =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><archetypeCatalogs><catalog type=\"remote\" location=\"file:///x\" description=\"\"/></archetypeCatalogs>";
        write( archetypeCatalog, catalogsContent );
        write( nonExported, "test" );

        Preferences prefs = getInstance().node( M2E );
        prefs.put( "eclipse.m2.offline", "test-TEST" );
        prefs.put( "eclipse.m2.debugOutput", "test-TEST" );
        prefs.put( "eclipse.m2.downloadSources", "test-TEST" );
        prefs.put( "eclipse.m2.downloadJavadocs", "test-TEST" );
        prefs.put( "eclipse.m2.globalSettingsFile", "test-TEST" );
        prefs.put( "eclipse.m2.userSettingsFile", "test-TEST" );
        prefs.put( "eclipse.m2.goalOnImport", "test-TEST" );
        prefs.put( "eclipse.m2.goalOnUpdate", "test-TEST" );
        prefs.put( "eclipse.m2.runtimes", "test-TEST" );
        prefs.put( "eclipse.m2.defaultRuntime", "test-TEST" );
        prefs.put( "eclipse.m2.updateIndexes", "test-TEST" );
        prefs.put( "eclipse.m2.updateProjects", "test-TEST" );
        prefs.put( "eclipse.m2.jiraUsername", "test-TEST" );
        prefs.put( "eclipse.m2.jiraPassword", "test-TEST" );
        prefs.put( "eclipse.m2.hideFoldersOfNestedProjects", "test-TEST" );
        prefs.put( "eclipse.m2.showConsoleOnErr", "test-TEST" );
        prefs.put( "eclipse.m2.showConsoleOnOutput", "test-TEST" );
        prefs.put( "eclipse.m2.separateProjectsForModules", "test-TEST" );
        prefs.put( "eclipse.m2.fullIndex", "test-TEST" );

        prefs = getInstance().node( M2E_XML );
        prefs.put( "org.maven.ide.eclipse.editor.xml.templates", "test-TEST" );

        File file = exportPrefs( PreferenceGroup.M2E );

        getInstance().node( M2E ).removeNode();
        getInstance().node( M2E_XML ).removeNode();
        write( archetypeCatalog, "failed" );
        write( nonExported, "passed" );

        importPrefs( file );

        prefs = getInstance().node( M2E );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.offline", null ) );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.debugOutput", null ) );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.downloadSources", null ) );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.downloadJavadocs", null ) );
        assertEquals( null, prefs.get( "eclipse.m2.globalSettingsFile", null ) );
        assertEquals( null, prefs.get( "eclipse.m2.userSettingsFile", null ) );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.goalOnImport", null ) );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.goalOnUpdate", null ) );
        assertEquals( null, prefs.get( "eclipse.m2.runtimes", null ) );
        assertEquals( null, prefs.get( "eclipse.m2.defaultRuntime", null ) );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.updateIndexes", null ) );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.updateProjects", null ) );
        assertEquals( null, prefs.get( "eclipse.m2.jiraUsername", null ) );
        assertEquals( null, prefs.get( "eclipse.m2.jiraPassword", null ) );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.hideFoldersOfNestedProjects", null ) );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.showConsoleOnErr", null ) );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.showConsoleOnOutput", null ) );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.separateProjectsForModules", null ) );
        assertEquals( "test-TEST", prefs.get( "eclipse.m2.fullIndex", null ) );

        prefs = getInstance().node( M2E_XML );
        assertEquals( "test-TEST", prefs.get( "org.maven.ide.eclipse.editor.xml.templates", null ) );

        assertEquals( "passed", read( nonExported ) );
        assertEquals( catalogsContent, read( archetypeCatalog ) );

        assertEquals( archetypeCatalogFactories.size() + 1, archetypeManager.getArchetypeCatalogs().size() );
        assertNotNull( archetypeManager.getArchetypeCatalogFactory( "file:///x" ) );
        archetypeManager.removeArchetypeCatalogFactory( "file:///x" );

        archetypeCatalog.delete();
        nonExported.delete();
    }

    public void testExportImportJDT()
        throws Exception
    {
        getInstance().node( JDT_CORE ).removeNode();

        Preferences prefs = getInstance().node( JDT_CORE );
        prefs.put( "org.eclipse.jdt.core.classpathVariable.JRE_SRC", "test-TEST" );
        prefs.put( "org.eclipse.jdt.core.formatter.insert_space_after_binary_operator", "test-TEST" );
        prefs.put( "org.eclipse.jdt.core.compiler.codegen.targetPlatform", "test-TEST" );
        prefs.put( "org.eclipse.jdt.core.compiler.source", "test-TEST" );

        prefs = getInstance().node( JDT_UI );
        prefs.put( "org.eclipse.jdt.ui.formatterprofiles", "test-TEST" );

        File file = exportPrefs( PreferenceGroup.JDT );

        getInstance().node( JDT_CORE ).removeNode();
        getInstance().node( JDT_UI ).removeNode();

        importPrefs( file );

        prefs = getInstance().node( JDT_CORE );
        assertEquals( null, prefs.get( "org.eclipse.jdt.core.classpathVariable.JRE_SRC", null ) );
        assertEquals( "test-TEST",
                      prefs.get( "org.eclipse.jdt.core.formatter.insert_space_after_binary_operator", null ) );
        assertEquals( "test-TEST", prefs.get( "org.eclipse.jdt.core.compiler.codegen.targetPlatform", null ) );
        assertEquals( "test-TEST", prefs.get( "org.eclipse.jdt.core.compiler.source", null ) );

        prefs = getInstance().node( JDT_UI );
        assertEquals( "test-TEST", prefs.get( "org.eclipse.jdt.ui.formatterprofiles", null ) );
    }

    public void testExportImportProxy()
        throws Exception
    {
        getInstance().node( JDT_CORE ).removeNode();

        Preferences prefs = getConfiguration().node( NET );
        prefs.put( "systemProxiesEnabled", "true" );
        prefs.put( "nonProxiedHosts", "localhost|127.0.0.1|127.0.0.111" );

        File file = exportPrefs( PreferenceGroup.PROXY );

        prefs.put( "systemProxiesEnabled", "false" );
        prefs.put( "nonProxiedHosts", "none" );

        importPrefs( file );

        prefs = getConfiguration().node( NET );
        assertEquals( "true", prefs.get( "systemProxiesEnabled", null ) );
        assertEquals( "localhost|127.0.0.1|127.0.0.111", prefs.get( "nonProxiedHosts", null ) );
    }

    public void testDeployment()
        throws Exception
    {
        File tmpFile = File.createTempFile( "s2pref", ".prefs" );
        tmpFile.deleteOnExit();
        File tmpDir = File.createTempFile( "s2pref", "dir" );
        tmpDir.delete();

        HttpServer server = new HttpServer();
        server.addResources( "prefs", tmpDir.getAbsolutePath() );
        server.start();
        try
        {
            prefMan.deployPreferences( tmpFile, server.getHttpUrl() + "/prefs/test.pref", null );
        }
        finally
        {
            server.stop();
        }

        File prefFile = new File( tmpDir, "test.pref" );
        try
        {
            assertTrue( prefFile.isFile() );
        }
        finally
        {
            prefFile.delete();
            tmpDir.delete();
        }
    }

    public void testGetPreferencesAtUrl()
        throws Exception
    {
        getInstance().node( JDT_CORE );
        getInstance().node( M2E );

        File file = exportPrefs( PreferenceGroup.values() );

        Set<PreferenceGroup> groups = prefMan.getPreferenceGroups( file.toURI().toString(), null );
        assertEquals( PreferenceGroup.values().length, groups.size() );
        for ( PreferenceGroup group : PreferenceGroup.values() )
        {
            assertTrue( group.name(), groups.contains( group ) );
        }

        for ( PreferenceGroup group : PreferenceGroup.values() )
        {
            file = exportPrefs( group );
            groups = prefMan.getPreferenceGroups( file.toURI().toString(), null );
            assertEquals( new HashSet<PreferenceGroup>( Arrays.asList( group ) ), groups );
        }
    }

}
