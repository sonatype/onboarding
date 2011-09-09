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
package com.sonatype.s2.project.ui.codebase.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction.MavenPathStorageEditorInput;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IFileEditorInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.common.S2ProjectCommon;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.S2ProjectFacade;
import com.sonatype.s2.project.prefs.PreferenceGroup;
import com.sonatype.s2.project.ui.codebase.Activator;
import com.sonatype.s2.project.ui.codebase.CodebaseImage;
import com.sonatype.s2.project.ui.codebase.Messages;

public class WorkspaceCodebaseEditorInput
    extends AbstractCodebaseEditorInput
    implements ICodebaseEditorInput, IFileEditorInput
{
    private static Logger log = LoggerFactory.getLogger( WorkspaceCodebaseEditorInput.class );

    private Image image;

    private boolean imageUpdated;

    private boolean preferencesUpdated;

    private IFile pmdFile;

    public WorkspaceCodebaseEditorInput( IFile pmdFile )
        throws CoreException
    {
        this.pmdFile = pmdFile;
        loadProject();
    }

    public WorkspaceCodebaseEditorInput( IFile pmdFile, IS2Project project )
    {
        super( project );
        this.pmdFile = pmdFile;
    }

    public Image getCodebaseImage()
    {
        if ( image == null )
        {
            image = CodebaseImage.getWorkspaceImage( pmdFile.getRawLocation().removeLastSegments( 1 ) );
        }
        return image;
    }

    public void setCodebaseImage( Image image )
    {
        this.image = image;
        imageUpdated = true;
    }

    public MavenPathStorageEditorInput getMavenSettings()
    {
        return null;
    }

    public void doSave( IProgressMonitor monitor )
        throws CoreException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try
        {
            S2ProjectFacade.writeProject( getProject(), buffer );

            ByteArrayInputStream is = new ByteArrayInputStream( buffer.toByteArray() );
            if ( pmdFile.exists() )
            {
                pmdFile.setContents( is, true, true, monitor );
            }
            else
            {
                pmdFile.create( is, true, monitor );
            }

            IPath path = pmdFile.getLocation().removeLastSegments( 1 );

            if ( imageUpdated && image != null )
            {
                CodebaseImage.saveWorkspaceImage( path, image, monitor );
                imageUpdated = false;
            }

            if ( preferencesUpdated )
            {
                IFile preferencesFile =
                    ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(
                                                                                 path.append( IS2Project.PROJECT_PREFERENCES_FILENAME ) );

                Collection<PreferenceGroup> groups = getEclipsePreferenceGroups();
                if ( groups == null )
                {
                    if ( preferencesFile.exists() )
                    {
                        preferencesFile.delete( true, monitor );
                    }
                }
                else
                {
                    ByteArrayOutputStream buffer2 = new ByteArrayOutputStream();

                    S2ProjectCore.getInstance().getPrefManager().exportPreferences( buffer2,
                                                                                    getEclipsePreferenceGroups(),
                                                                                    monitor );

                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( buffer2.toByteArray() );
                    if ( preferencesFile.exists() )
                    {
                        preferencesFile.setContents( byteArrayInputStream, false, true, monitor );
                    }
                    else
                    {
                        preferencesFile.create( byteArrayInputStream, true, monitor );
                    }
                }
                preferencesUpdated = false;
            }
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                                                 Messages.errors_errorSavingCodebaseDescriptor, e ) );
        }
    }

    @Override
    public boolean equals( Object o )
    {
        if ( o == this )
        {
            return true;
        }

        if ( !( o instanceof WorkspaceCodebaseEditorInput ) )
        {
            return false;
        }

        return super.equals( pmdFile.equals( ( (WorkspaceCodebaseEditorInput) o ).pmdFile ) );
    }

    private void loadProject()
        throws CoreException
    {
        InputStream is = pmdFile.getContents( true );
        try
        {
            setProject( S2ProjectCommon.loadProject( is, false ) );
        }
        catch ( IOException e )
        {
            log.error( e.getMessage(), e );
            throw new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                                                 Messages.errors_errorOpeningCodebaseDescriptor, e ) );
        }
        finally
        {
            try
            {
                is.close();
            }
            catch ( IOException e )
            {
                log.error( e.getMessage(), e );
            }
        }
    }

    public IFile getFile()
    {
        return pmdFile;
    }

    public IStorage getStorage()
        throws CoreException
    {
        return pmdFile;
    }

    @Override
    public String getTitle()
    {
        return pmdFile.getName();
    }

    @Override
    public void setEclipsePreferenceGroups( Collection<PreferenceGroup> preferenceGroups )
    {
        super.setEclipsePreferenceGroups( preferenceGroups );
        preferencesUpdated = true;
    }
}
