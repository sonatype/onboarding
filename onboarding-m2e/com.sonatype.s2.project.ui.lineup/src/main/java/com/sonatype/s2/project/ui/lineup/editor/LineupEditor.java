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
package com.sonatype.s2.project.ui.lineup.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.ui.common.editor.AbstractFileEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.nexus.NexusFacade;
import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Reader;
import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Writer;
import com.sonatype.s2.project.ui.lineup.Activator;
import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;

public class LineupEditor
    extends AbstractFileEditor
{
    private static final String EXTENSION_ID = "com.sonatype.s2.project.ui.lineup.editor.LineupEditorAction"; //$NON-NLS-1$

    private Logger log = LoggerFactory.getLogger( LineupEditor.class );

    private NexusLineupPublishingInfo info;

    private LineupEditorPage lineupEditorPage;

    @Override
    protected void addPages()
    {
        try
        {
            lineupEditorPage = new LineupEditorPage( this );
            addPage( lineupEditorPage );
        }
        catch ( PartInitException e )
        {
            StatusManager.getManager().handle( e, Activator.PLUGIN_ID );
        }
    }

    @Override
    public void doSave( IProgressMonitor monitor )
    {
        setBusy( true );

        try
        {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try
            {
                new P2LineupXpp3Writer().write( buffer, info.getLineup() );
            }
            catch ( IOException e )
            {
                throw new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                                                     Messages.lineupEditor_errorSaving, e ) );
            }

            ByteArrayInputStream is = new ByteArrayInputStream( buffer.toByteArray() );
            IFile file = ( (IFileEditorInput) getEditorInput() ).getFile();
            if ( file.exists() )
            {
                file.setContents( is, true, true, monitor );
            }
            else
            {
                file.create( is, true, monitor );
            }
            clearDirty();
            lineupEditorPage.updateTitle();
            updatePages();
        }
        catch ( CoreException e )
        {
            StatusManager.getManager().handle( e.getStatus(), StatusManager.BLOCK | StatusManager.LOG );
        }
        finally
        {
            setBusy( false );
            monitor.done();
        }
    }

    @Override
    protected void createPages()
    {
        super.createPages();
        if ( getPageCount() == 1 && getContainer() instanceof CTabFolder )
        {
            ( (CTabFolder) getContainer() ).setTabHeight( 0 );
        }
    }

    @Override
    public void init( IEditorSite site, IEditorInput input )
        throws PartInitException
    {
        if ( input instanceof IFileEditorInput )
        {
            super.init( site, input );

            Exception e = null;

            try
            {
                InputStream is = ( (IFileEditorInput) input ).getFile().getContents();
                try
                {
                    info = new NexusLineupPublishingInfo( new P2LineupXpp3Reader().read( is ) );
                    info.setServerUrl( NexusFacade.getMainNexusServerURL() );
                    loadActionExtensions( EXTENSION_ID, Activator.PLUGIN_ID );
                    updatePages();
                }
                finally
                {
                    is.close();
                }
            }
            catch ( IOException ioException )
            {
                e = ioException;
            }
            catch ( XmlPullParserException xmlPullParserException )
            {
                e = xmlPullParserException;
            }
            catch ( CoreException coreException )
            {
                e = coreException;
            }
            if ( e != null )
            {
                String message = NLS.bind( Messages.lineupEditor_errorOpeningEditor, e.getMessage() );
                log.error( message, e );
                throw new PartInitException( message, e );
            }
        }
        else
        {
            throw new PartInitException( Messages.lineupEditor_wrongInput );
        }
    }

    public NexusLineupPublishingInfo getLineupInfo()
    {
        return info;
    }

    public IStatus validateLineup( IProgressMonitor monitor )
    {
        IStatus status = lineupEditorPage.validateLineup( monitor );
        Display.getDefault().asyncExec( new Runnable()
        {
            public void run()
            {
                updatePages();
            }
        } );
        return status;
    }

    public static IStatus openEditor( IEditorInput input )
    {
        IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        try
        {
            workbenchPage.openEditor( input, LineupEditor.class.getName() );
        }
        catch ( PartInitException e )
        {
            return e.getStatus();
        }

        return Status.OK_STATUS;
    }
}
