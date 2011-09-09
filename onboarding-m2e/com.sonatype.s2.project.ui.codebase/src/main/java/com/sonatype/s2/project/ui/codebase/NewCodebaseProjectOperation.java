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
package com.sonatype.s2.project.ui.codebase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.FileEditorInput;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.S2ProjectFacade;
import com.sonatype.s2.project.model.descriptor.P2LineupLocation;
import com.sonatype.s2.project.ui.codebase.editor.CodebaseDescriptorEditor;
import com.sonatype.s2.project.ui.internal.AbstractNewProjectOperation;

@SuppressWarnings( "restriction" )
public class NewCodebaseProjectOperation
    extends AbstractNewProjectOperation
{
    private IS2Project codebase;

    /**
     * Codebase URL in a codebase repository. Can be null. If not null, must end with slash.
     */
    private final String codebaseBaseUrl;

    public NewCodebaseProjectOperation( String projectName, String groupId, String artifactId, String version,
                                        String lineupUrl )
    {
        super( projectName );
        this.codebaseBaseUrl = null;
        this.codebase = S2ProjectFacade.createProject( groupId, artifactId, version );
        codebase.setName( projectName );

        if ( lineupUrl != null )
        {
            P2LineupLocation lineupLocation = new P2LineupLocation();
            lineupLocation.setUrl( lineupUrl );
            codebase.setP2LineupLocation( lineupLocation );
        }
    }

    public NewCodebaseProjectOperation( String projectName, IS2Project project, String codebaseBaseUrl )
    {
        super( projectName != null ? projectName : project.getName() );
        this.codebase = project;
        this.codebaseBaseUrl = codebaseBaseUrl;
    }

    public IProject createProject( IProgressMonitor monitor )
        throws CoreException
    {
        return createProject( monitor, true );
    }

    public IProject createProject( IProgressMonitor monitor, boolean open )
        throws CoreException
    {
        IProject project = super.createProject( monitor );
        try
        {
            IFolder s2 = project.getFolder( IS2Project.PROJECT_DESCRIPTOR_PATH );

            if ( !s2.exists() )
            {
                s2.create( false, true, monitor );
            }

            IFile pmdFile = s2.getFile( IS2Project.PROJECT_DESCRIPTOR_FILENAME );

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            S2ProjectFacade.writeProject( codebase, buffer );
            InputStream is = new ByteArrayInputStream( buffer.toByteArray() );
            try
            {
                pmdFile.create( is, false /* force */, monitor );
            }
            finally
            {
                IOUtil.close( is );
            }

            if ( codebaseBaseUrl != null )
            {
                // copy other codebase files
                copyFile( codebaseBaseUrl, IS2Project.PROJECT_ICON_FILENAME, s2, monitor );
            }
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                                                 "Could not create project descriptor", e ) );
        }

        if ( open )
        {
            IFile pmdFile =
                project.getFolder( IS2Project.PROJECT_DESCRIPTOR_PATH ).getFile( IS2Project.PROJECT_DESCRIPTOR_FILENAME );

            openProjectEditor( pmdFile );
        }

        return project;
    }

    private static void openProjectEditor( final IFile pmdFile )
        throws CoreException
    {
        Display.getDefault().syncExec( new Runnable()
        {
            public void run()
            {
                CodebaseDescriptorEditor.openEditor( new FileEditorInput( pmdFile ) );
            }
        } );
    }
}
