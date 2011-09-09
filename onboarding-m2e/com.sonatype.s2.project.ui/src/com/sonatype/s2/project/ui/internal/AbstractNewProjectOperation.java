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
package com.sonatype.s2.project.ui.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.maven.ide.eclipse.io.NotFoundException;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO this class does not belong to this bundle!
 */
public abstract class AbstractNewProjectOperation
{
    private static final Logger logger = LoggerFactory.getLogger( AbstractNewProjectOperation.class );

    protected final String projectName;

    protected AbstractNewProjectOperation( String projectName )
    {
        this.projectName = projectName;

        if ( projectName == null )
        {
            throw new NullPointerException();
        }
    }

    /**
     * Copies a file from baseUrl to dest folder. Missing files are silently ignored. All other errors are rethrown as
     * CoreExceptions
     */
    protected void copyFile( String baseUrl, String filename, IFolder dest, IProgressMonitor monitor )
        throws CoreException
    {
        try
        {
            InputStream is = S2IOFacade.openStream( baseUrl + filename, monitor );
            try
            {
                IFile file = dest.getFile( filename );
                if ( file.exists() )
                {
                    file.setContents( is, IResource.FORCE, monitor );
                }
                else
                {
                    file.create( is, true, monitor );
                }
            }
            finally
            {
                is.close();
            }
        }
        catch ( CoreException e )
        {
            IStatus status = e.getStatus();
            if ( status.getException() instanceof NotFoundException )
            {
                logger.debug( "Could not copy project file " + filename + " from " + baseUrl, e );
            }
            else
            {
                throw new CoreException( status );
            }
        }
        catch ( NotFoundException e )
        {
            // expected and ignored
            logger.debug( "Could not copy project file " + filename + " from " + baseUrl, e );
        }
        catch ( FileNotFoundException e )
        {
            // expected and ignored
            logger.debug( "Could not copy project file " + filename + " from " + baseUrl, e );
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID, "Could not copy project file", e ) );
        }
        catch ( URISyntaxException e )
        {
            // this is a bug, there is no way the user or environment could have caused this
            logger.warn( "Could not copy project file " + filename + " from " + baseUrl, e );
        }
    }

    protected IProject createProject( IProgressMonitor monitor )
        throws CoreException
    {
        IProject project;
        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        project = workspace.getRoot().getProject( projectName );

        if ( !project.exists() )
        {
            IPath projectPath = project.getFullPath();
            IPath projectDir = workspace.getRoot().getLocation().append( projectPath );
            File projectDirAsFile = projectDir.toFile();
            // if projectDirAsFile exists and is a directory, delete it it
            if ( projectDirAsFile.exists() )
            {
                // if projectDirAsFile is a file (not a directory), the project creation will fail anyway
                if ( projectDirAsFile.isDirectory() )
                {
                    try
                    {
                        FileUtils.deleteDirectory( projectDirAsFile );
                    }
                    catch ( IOException e )
                    {
                        IStatus status = new Status( IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e );
                        throw new CoreException( status );
                    }
                }
            }

            project.create( monitor );
        }
        else
        {
            // TODO shall we somehow report? is it important?
        }

        project.open( monitor );
        return project;
    }

}
