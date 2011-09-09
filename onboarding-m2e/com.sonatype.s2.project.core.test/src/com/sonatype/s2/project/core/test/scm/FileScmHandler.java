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
package com.sonatype.s2.project.core.test.scm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.scm.MavenProjectScmInfo;
import org.eclipse.m2e.scm.spi.ScmHandler;

public class FileScmHandler
    extends ScmHandler
{

    private static final String SCMURL_PREFIX = "scm:testfile:";

    @Override
    public void checkoutProject( MavenProjectScmInfo info, File location, IProgressMonitor monitor )
        throws CoreException, InterruptedException
    {
        String folderUrl = info.getFolderUrl();
        if ( !folderUrl.startsWith( SCMURL_PREFIX ) )
        {
            throw new IllegalArgumentException( "Unsupported SCM URL " + folderUrl );
        }

        File src = toFile( folderUrl );

        try
        {
            Properties config = getConfig( src );

            if ( !validateAccess( config, info ) )
            {
                throw new IOException( "Invalid username/password" );
            }

            DirectoryScanner ds = new DirectoryScanner();
            ds.setExcludes( DirectoryScanner.DEFAULTEXCLUDES );
            ds.setBasedir( src );
            ds.scan();

            for ( String relPath : ds.getIncludedFiles() )
            {
                File srcFile = new File( src, relPath );
                File dstFile = new File( location, relPath );
                FileUtils.copyFile( srcFile, dstFile );
            }

            FileTeamProvider.writeRelpaths( location, ds.getIncludedFiles() );
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, "com.sonatype.s2.project.core.test",
                                                 "Could not copy project", e ) );
        }
    }

    public static File toFile( String folderUrl )
    {
        File src = new File( folderUrl.substring( SCMURL_PREFIX.length() ) );
        return src;
    }

    private Properties getConfig( File basedir )
        throws IOException
    {
        Properties props = new Properties();

        File configFile = new File( basedir, "testfile.properties" );
        if ( configFile.isFile() )
        {
            FileInputStream fis = new FileInputStream( configFile );
            try
            {
                props.load( fis );
            }
            finally
            {
                fis.close();
            }
        }

        return props;
    }

    private boolean validateAccess( Properties config, MavenProjectScmInfo info )
    {
        if ( Boolean.parseBoolean( config.getProperty( "authentication" ) ) )
        {
            if ( info.getUsername() == null || info.getUsername().length() <= 0
                || !config.getProperty( info.getUsername(), "" ).equals( info.getPassword() ) )
            {
                return false;
            }
        }

        return true;
    }

}
