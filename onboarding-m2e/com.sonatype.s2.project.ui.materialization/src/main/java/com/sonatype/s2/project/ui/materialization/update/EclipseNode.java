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
package com.sonatype.s2.project.ui.materialization.update;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.ide.IDEUpdater;
import com.sonatype.s2.project.core.ide.IIDEUpdater;
import com.sonatype.s2.project.ui.materialization.Images;
import com.sonatype.s2.project.ui.materialization.Messages;

public class EclipseNode
    extends CodebaseViewNode
{
    private final static Map<String, String> statusMessages = new HashMap<String, String>();

    private final static Map<String, Image> statusImages = new HashMap<String, Image>();

    static
    {
        statusMessages.put( IIDEUpdater.NOT_LINEUP_MANAGED, Messages.eclipseNode_status_notLineupManaged );
        statusMessages.put( IIDEUpdater.NOT_UP_TO_DATE, Messages.eclipseNode_status_notUpToDate );
        statusMessages.put( IIDEUpdater.UNKNOWN, Messages.eclipseNode_status_unknown );
        statusMessages.put( IIDEUpdater.UP_TO_DATE, Messages.eclipseNode_status_upToDate );
        statusMessages.put( IIDEUpdater.ERROR, Messages.eclipseNode_status_error );

        statusImages.put( IIDEUpdater.NOT_LINEUP_MANAGED, Images.ECLIPSE );
        statusImages.put( IIDEUpdater.NOT_UP_TO_DATE,
                          Images.getOverlayImage( "eclipse.gif", "overlay_delta.gif", IDecoration.BOTTOM_RIGHT ) ); //$NON-NLS-1$ //$NON-NLS-2$
        statusImages.put( IIDEUpdater.UNKNOWN,
                          Images.getOverlayImage( "eclipse.gif", "overlay_question.gif", IDecoration.BOTTOM_RIGHT ) ); //$NON-NLS-1$ //$NON-NLS-2$
        statusImages.put( IIDEUpdater.UP_TO_DATE, Images.ECLIPSE );
        statusImages.put( IIDEUpdater.ERROR,
                          Images.getOverlayImage( "eclipse.gif", "overlay_error.gif", IDecoration.BOTTOM_RIGHT ) ); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String status;

    private Image image;

    public EclipseNode( IWorkspaceCodebase workspaceCodebase )
    {
        super( workspaceCodebase, Messages.eclipseNode_title, null, Images.ECLIPSE );
        status = getWorkspaceCodebase().getIsP2LineupUpToDate();
        image = statusImages.get( status );
    }

    @Override
    public String getStatus()
    {
        return statusMessages.get( status );
    }

    @Override
    public Image getImage(boolean showStatus)
    {
        return image == null || !showStatus ? super.getImage(showStatus) : image;
    }

    @Override
    public boolean isUpdateAvailable()
    {
        return IIDEUpdater.NOT_UP_TO_DATE.equals( status ) || IIDEUpdater.NOT_LINEUP_MANAGED.equals( status );
    }

    @Override
    public void update()
    {
        Job job = new Job( Messages.eclipseNode_updateJob )
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                IIDEUpdater updater = IDEUpdater.getUpdater();
                return updater.performUpdate( getWorkspaceCodebase().getPending().getP2LineupLocation(), monitor );
            }
        };
        job.setUser( true );
        job.schedule();
    }
}
