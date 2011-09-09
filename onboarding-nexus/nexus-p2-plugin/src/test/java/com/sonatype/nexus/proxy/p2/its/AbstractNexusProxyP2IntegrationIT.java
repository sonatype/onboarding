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
package com.sonatype.nexus.proxy.p2.its;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.junit.After;
import org.junit.Before;
import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.sonatype.nexus.test.utils.TestProperties;

public abstract class AbstractNexusProxyP2IntegrationIT
    extends AbstractNexusIntegrationTest
{
    @Override
    protected void customizeContainerConfiguration( ContainerConfiguration configuration )
    {
        super.customizeContainerConfiguration( configuration );
        configuration.setClassPathScanning( PlexusConstants.SCANNING_ON );
        
        // to force creating the proxies/lineups with no onboarding plugin
        System.setProperty( "p2.lineups.create", "true" );
    }

    protected static ServletServer server;

    protected static final String localStorageDir;
    static
    {
        localStorageDir = TestProperties.getString( "proxy.repo.base.dir" );
    }

    protected AbstractNexusProxyP2IntegrationIT()
    {
        super();
    }

    protected AbstractNexusProxyP2IntegrationIT( String testRepositoryId )
    {
        super( testRepositoryId );
    }

    @Override
    protected void copyConfigFiles()
        throws IOException
    {
        super.copyConfigFiles();
    }

    @Before
    public void startProxy()
        throws Exception
    {
        if ( server == null )
        {
            server = (ServletServer) lookup( ServletServer.ROLE );
            server.start();
        }
    }

    @After
    public void stopProxy()
        throws Exception
    {
        if ( server != null )
        {
            server = (ServletServer) lookup( ServletServer.ROLE );
            server.stop();
            server = null;
        }
    }

    private int forkedProcessTimeoutInSeconds = 900;

    private File getEquinoxLauncher( String p2location )
        throws Exception
    {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( p2location );
        ds.setIncludes( new String[] { "plugins/org.eclipse.equinox.launcher_*.jar" } );
        ds.scan();
        String[] includedFiles = ds.getIncludedFiles();
        if ( includedFiles == null || includedFiles.length != 1 )
        {
            throw new Exception( "Can't locate org.eclipse.equinox.launcher bundle in " + p2location );
        }
        return new File( p2location, includedFiles[0] );
    }

    protected void installUsingP2( String repositoryURL, String installIU, String destination )
        throws Exception
    {
        installUsingP2( repositoryURL, installIU, destination, null, null, null );
    }

    protected void installUsingP2( String repositoryURL, String installIU, String destination, String p2Os,
                                   String p2Ws, String p2Arch, String... extraArgs )
        throws Exception
    {
        FileUtils.deleteDirectory( destination );

        String p2location = getP2RuntimeLocation().getCanonicalPath();
        cleanP2Runtime( p2location );

        Commandline cli = new Commandline();

        cli.setWorkingDirectory( p2location );

        String executable = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
        if ( File.separatorChar == '\\' )
        {
            executable = executable + ".exe";
        }
        cli.setExecutable( executable );

        if ( extraArgs != null )
        {
            cli.addArguments( extraArgs );
        }

        cli.addArguments( new String[] { "-Declipse.p2.data.area=" + destination + "/p2" } );
        cli.addArguments( new String[] { "-Dorg.eclipse.ecf.provider.filetransfer.retrieve.readTimeout=30000" } );

        cli.addArguments( new String[] { "-jar", getEquinoxLauncher( p2location ).getAbsolutePath(), } );

        cli.addArguments( new String[] { "-nosplash", "-application", "org.eclipse.equinox.p2.director",
            "-metadataRepository", repositoryURL, "-artifactRepository", repositoryURL, "-installIU", installIU,
            "-destination", destination, "-profile", getTestId(), "-profileProperties",
            "org.eclipse.update.install.features=true", "-bundlepool", destination, "-roaming", "-debug",
            "-consolelog", } );

        if ( p2Os != null )
        {
            cli.addArguments( new String[] { "-p2.os", p2Os } );
        }

        if ( p2Ws != null )
        {
            cli.addArguments( new String[] { "-p2.ws", p2Ws } );
        }

        if ( p2Arch != null )
        {
            cli.addArguments( new String[] { "-p2.arch", p2Arch } );
        }

        log.info( "Command line:\n\t" + cli.toString() );

        final StringBuffer buf = new StringBuffer();

        StreamConsumer out = new StreamConsumer()
        {
            public void consumeLine( String line )
            {
                System.out.println( line );
                buf.append( "[OUT] " ).append( line ).append( "\n" );
            }
        };

        StreamConsumer err = new StreamConsumer()
        {
            public void consumeLine( String line )
            {
                System.err.println( line );
                buf.append( "[ERR] " ).append( line ).append( "\n" );
            }
        };

        int result = CommandLineUtils.executeCommandLine( cli, out, err, forkedProcessTimeoutInSeconds );
        if ( result != 0 )
        {
            throw new P2ITException( result, buf );
        }
    }

    private void cleanP2Runtime( String p2location )
        throws IOException
    {
        FileUtils.deleteDirectory( p2location + "/p2" ); // clean p2 runtime cache
        DirectoryScanner scanner = new DirectoryScanner();
        File configuration = new File( p2location, "configuration" );
        scanner.setBasedir( configuration );
        scanner.scan();
        for ( String path : scanner.getIncludedFiles() )
        {
            if ( path != null && path.trim().length() > 0 && !"config.ini".equals( path ) )
            {
                new File( configuration, path ).delete();
            }
        }
        for ( String path : scanner.getIncludedDirectories() )
        {
            if ( path != null && path.trim().length() > 0 )
            {
                FileUtils.deleteDirectory( new File( configuration, path ) );
            }
        }
    }

    protected File getP2RuntimeLocation()
        throws IOException
    {
        File dst = getOverridableFile( "p2" );
        return dst;
    }

    @Override
    public void oncePerClassSetUp()
        throws Exception
    {
        startProxy();

        super.oncePerClassSetUp();
    }

    protected void replaceInFile( String filename, String target, String replacement )
        throws IOException
    {
        String content = FileUtils.fileRead( filename );
        content = content.replace( target, replacement );
        FileUtils.fileWrite( filename, content );
    }

    @Override
    protected void copyTestResources()
        throws IOException
    {
        super.copyTestResources();

        File source = new File( TestProperties.getString( "test.resources.source.folder" ), "proxyRepo" );
        if ( !source.exists() )
        {
            return;
        }

        FileTestingUtils.interpolationDirectoryCopy( source, new File( localStorageDir ), TestProperties.getAll() );

    }

}
