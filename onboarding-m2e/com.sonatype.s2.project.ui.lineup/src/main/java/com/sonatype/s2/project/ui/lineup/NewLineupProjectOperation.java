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
package com.sonatype.s2.project.ui.lineup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.FileEditorInput;

import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Writer;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.internal.AbstractNewProjectOperation;
import com.sonatype.s2.project.ui.lineup.editor.LineupEditor;

public class NewLineupProjectOperation
    extends AbstractNewProjectOperation
{
    private final IP2Lineup lineup;

    public NewLineupProjectOperation( String projectName, IP2Lineup lineup )
    {
        super( projectName );
        this.lineup = lineup;
    }

    @Override
    public IProject createProject( IProgressMonitor monitor )
        throws CoreException
    {
        return createProject( monitor, false );
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

            IFile lineupFile = s2.getFile( IP2Lineup.LINEUP_FILENAME );

            ByteArrayOutputStream buf = new ByteArrayOutputStream();

            new P2LineupXpp3Writer().write( buf, (P2Lineup) lineup );

            setContents( lineupFile, buf.toByteArray(), monitor );
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                                                 "Could not create lineup descriptor", e ) );
        }

        if ( open )
        {
            IFile file = project.getFolder( IS2Project.PROJECT_DESCRIPTOR_PATH ).getFile( IP2Lineup.LINEUP_FILENAME );

            openEditor( file );
        }

        return project;
    }

    private static void openEditor( final IFile file )
        throws CoreException
    {
        Display.getDefault().syncExec( new Runnable()
        {
            public void run()
            {
                LineupEditor.openEditor( new FileEditorInput( file ) );
            }
        } );
    }

    protected void setContents( IFile file, byte[] contents, IProgressMonitor monitor )
        throws CoreException
    {
        ByteArrayInputStream is = new ByteArrayInputStream( contents );
        if ( file.exists() )
        {
            file.setContents( is, IResource.FORCE, monitor );
        }
        else
        {
            file.create( is, true, monitor );
        }
    }
}
