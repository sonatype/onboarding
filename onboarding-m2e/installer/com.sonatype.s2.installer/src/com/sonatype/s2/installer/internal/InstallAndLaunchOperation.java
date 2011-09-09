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
package com.sonatype.s2.installer.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.maven.ide.eclipse.io.UrlFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.nexus.NexusFacade;
import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupHelper;
import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Reader;

public class InstallAndLaunchOperation
    implements IRunnableWithProgress
{
    private Logger log = LoggerFactory.getLogger( InstallAndLaunchOperation.class );

    private final String lineupUrl;

    private final String pmdUrl;

    private final String workspacePath;

    private final String installationPath;

    private final String nexusUrl;

    private final boolean shared;

    public InstallAndLaunchOperation( String nexusUrl, String lineupUrl, String pmdUrl, String workspacePath,
                                      String installationPath, boolean shared )
    {
        this.nexusUrl = nexusUrl;
        this.lineupUrl = lineupUrl;
        this.pmdUrl = pmdUrl;
        this.workspacePath = workspacePath;
        this.installationPath = installationPath;
        this.shared = shared;
    }

    private void launchProduct()
        throws CoreException, IOException
    {
        IPath installLocation = new Path( installationPath );
        String toRun = installLocation.append( "eclipse" ).toString();

        log.debug( "toRun={}", toRun );
        toRun = toRun.trim();

        log.debug( "workspacePath={}", workspacePath );
        String workspacePathParam = workspacePath.trim();

        String cmd[] =
            new String[] { toRun, "-application", "com.sonatype.s2.project.materializer", "-data", workspacePathParam,
                "-materializer.descriptorURL", pmdUrl, NexusFacade.ARG_NEXUS_BASE_URL, nexusUrl };
        String sCmd = "";
        for ( String arg : cmd )
        {
            sCmd += arg + " ";
        }
        log.info( "Materialization command: {}", sCmd );
        Runtime.getRuntime().exec( cmd, null, installLocation.toFile() );
    }

    private void installP2Lineup( IProgressMonitor monitor )
        throws URISyntaxException, IOException, XmlPullParserException, CoreException
    {
        log.info( "Installing lineup: {}", lineupUrl );
        IAuthData authData = AuthFacade.getAuthService().select( lineupUrl );
        if ( authData != null )
        {
            P2AuthHelper.setCredentials( URIUtil.fromString( lineupUrl ), authData.getUsername(),
                                         authData.getPassword() );
        }

        IP2Lineup p2Lineup = loadP2Lineup( lineupUrl );
        String p2LineupIUId = P2LineupHelper.getMasterInstallableUnitId( p2Lineup );
        Version p2LineupIUVersion = Version.parseVersion( p2Lineup.getVersion() );
        P2Installer p2Installer =
            new P2Installer( lineupUrl, p2LineupIUId + "/" + p2LineupIUVersion, installationPath, shared );
        p2Installer.run( monitor );
    }

    private IP2Lineup loadP2Lineup( String p2LineupURLString )
        throws URISyntaxException, IOException, XmlPullParserException
    {
        if ( !p2LineupURLString.endsWith( "/" ) )
        {
            p2LineupURLString += "/";
        }
        URI p2LineupUri = new URL( new URL( p2LineupURLString ), "p2lineup.xml" ).toURI();
        InputStream is =
            new UrlFetcher().openStream( p2LineupUri, new NullProgressMonitor(), AuthFacade.getAuthService(),
                                         S2IOFacade.getProxyService() );
        try
        {
            return new P2LineupXpp3Reader().read( is, false /* strict */);
        }
        finally
        {
            IOUtil.close( is );
        }
    }

    private void createAndValidatePath( String pathString )
        throws IOException
    {
        File path = new File( pathString );
        if ( path.exists() )
        {
            File testFile = new File( path, "test" + System.currentTimeMillis() );
            if ( testFile.mkdirs() )
            {
                testFile.delete();
            }
            else
            {
                throw new IOException( "Cannot access path '" + pathString + "'." );
            }
            return;
        }

        if ( !path.mkdirs() )
        {
            throw new IOException( "Cannot create path '" + pathString + "'." );
        }
    }

    public void run( IProgressMonitor monitor )
        throws InvocationTargetException, InterruptedException
    {
        try
        {
            createAndValidatePath( installationPath );
            createAndValidatePath( workspacePath );

            installP2Lineup( monitor );

            setJVM();

            launchProduct();
        }
        catch ( RuntimeException e )
        {
            throw new InvocationTargetException( e, e.getMessage() );
        }
        catch ( URISyntaxException e )
        {
            throw new InvocationTargetException( e, e.getMessage() );
        }
        catch ( IOException e )
        {
            throw new InvocationTargetException( e, e.getMessage() );
        }
        catch ( XmlPullParserException e )
        {
            throw new InvocationTargetException( e, e.getMessage() );
        }
        catch ( CoreException e )
        {
            throw new InvocationTargetException( e, e.getMessage() );
        }
    }

    private void setJVM()
        throws IOException
    {
        String javaHome = System.getProperty( "java.home" );
        log.debug( "java.home={}", javaHome );
        if ( javaHome == null || javaHome.trim().length() == 0 )
        {
            // Probably impossible...
            log.warn( "Cannot set the jvm for the installed eclipse: java.home is empty" );
            return;
        }

        File jvmPath = new File( javaHome, "bin" );
        log.debug( "Setting jvm path to {}", jvmPath );
        if ( !jvmPath.exists() )
        {
            // Probably impossible...
            log.warn( "Cannot set the jvm for the installed eclipse: Path does not exist: {}", jvmPath );
            return;
        }

        File eclipseIniFile = new File( installationPath, "eclipse.ini" );
        if ( !eclipseIniFile.exists() )
        {
            // Probably impossible...
            log.warn( "Cannot set the jvm for the installed eclipse: File does not exist: {}", eclipseIniFile );
            return;
        }
        String eclipseIniContent = FileUtils.fileRead( eclipseIniFile );
        String lineSeparator = System.getProperty( "line.separator" );
        eclipseIniContent = "-vm" + lineSeparator + jvmPath.getAbsolutePath() + lineSeparator + eclipseIniContent;
        log.debug( "eclipse.ini new content:\n{}", eclipseIniContent );
        FileUtils.fileWrite( eclipseIniFile.getAbsolutePath(), eclipseIniContent );
    }
}
