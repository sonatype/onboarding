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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;

import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.team.ITeamProvider;
import com.sonatype.s2.project.core.team.TeamOperationResult;

public class FileTeamProvider
    implements ITeamProvider
{

    private static final QualifiedName QN_SHARED = new QualifiedName( FileTeamProvider.class.getName(), "shared" );

    public static Set<String> readRelpaths( File location )
        throws IOException
    {
        Set<String> relpath = new LinkedHashSet<String>();

        BufferedReader r = new BufferedReader( new FileReader( new File( location, ".relpath" ) ) );
        try
        {
            String str;
            while ( ( str = r.readLine() ) != null )
            {
                relpath.add( str );
            }
        }
        finally
        {
            IOUtil.close( r );
        }

        return relpath;
    }

    public static void writeRelpaths( File location, String[] relpaths )
        throws IOException
    {
        BufferedWriter w = new BufferedWriter( new FileWriter( new File( location, ".relpath" ) ) );
        try
        {
            for ( String relPath : relpaths )
            {
                w.write( relPath + "\n" );
            }
        }
        finally
        {
            IOUtil.close( w );
        }
    }

    public TeamOperationResult getUpdateStatus( IWorkspaceSourceTree sourceTree, IProgressMonitor monitor )
        throws CoreException
    {
        File src = FileScmHandler.toFile( sourceTree.getScmUrl() );
        File dst = new File( sourceTree.getLocation() );

        DirectoryScanner ds = new DirectoryScanner();
        ds.setExcludes( DirectoryScanner.DEFAULTEXCLUDES );
        ds.setBasedir( src );
        ds.scan();

        try
        {
            Set<String> relpaths = readRelpaths( dst );

            for ( String relpath : ds.getIncludedFiles() )
            {
                File srcFile = new File( src, relpath );
                File dstFile = new File( dst, relpath );
                if ( !FileUtils.contentEquals( srcFile, dstFile ) )
                {
                    return TeamOperationResult.RESULT_CHANGED;
                }
                relpaths.remove( relpath );
            }

            if ( !relpaths.isEmpty() )
            {
                // removed from source code repository
                return TeamOperationResult.RESULT_CHANGED;
            }
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, getClass().getName(), e.getMessage(), e ) );
        }

        return TeamOperationResult.RESULT_UPTODATE;
    }

    protected boolean isUptodate( Set<String> relpath, File srcFile, File dstFile )
        throws IOException
    {
        return FileUtils.contentEquals( srcFile, dstFile );
    }

    public TeamOperationResult updateFromRepository( IWorkspaceSourceTree sourceTree, IProgressMonitor monitor )
        throws CoreException
    {
        File src = FileScmHandler.toFile( sourceTree.getScmUrl() );
        File dst = new File( sourceTree.getLocation() );

        DirectoryScanner ds = new DirectoryScanner();
        ds.setExcludes( DirectoryScanner.DEFAULTEXCLUDES );
        ds.setBasedir( src );
        ds.scan();

        try
        {
            Set<String> relpaths = readRelpaths( dst );
            for ( String relpath : ds.getIncludedFiles() )
            {
                relpaths.remove( relpath );
                File srcFile = new File( src, relpath );
                File dstFile = new File( dst, relpath );
                if ( !FileUtils.contentEquals( srcFile, dstFile ) )
                {
                    FileUtils.copyFile( srcFile, dstFile );
                }
            }

            for ( String relpath : relpaths )
            {
                File file = new File( dst, relpath );
                if ( !file.delete() )
                {
                    throw new IOException( "Could not delete file " + file.getAbsolutePath() );
                }
            }

            writeRelpaths( dst, ds.getIncludedFiles() );
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, "com.sonatype.s2.project.core.test",
                                                 "Could not update source tree", e ) );
        }

        // refresh
        IPath root = Path.fromOSString( sourceTree.getLocation() );
        for ( IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects() )
        {
            if ( root.isPrefixOf( project.getLocation() ) )
            {
                project.refreshLocal( IResource.DEPTH_INFINITE, monitor );
            }
        }

        return TeamOperationResult.RESULT_UPTODATE;
    }

    public void afterProjectsImport( List<IMavenProjectImportResult> projectImportResults, File location,
                                     IProgressMonitor monitor )
        throws CoreException
    {
        for ( IMavenProjectImportResult result : projectImportResults )
        {
            IProject project = result.getProject();
            if ( project != null )
            {
                project.setPersistentProperty( QN_SHARED, "true" );
            }
        }
    }

    public static void assertTeamProviderEnabled( IProject project )
        throws CoreException
    {
        if ( !Boolean.parseBoolean( project.getPersistentProperty( QN_SHARED ) ) )
        {
            Assert.fail( "Team provider is not enabled for project " + project );
        }
    }
}
