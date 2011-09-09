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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.internal.update.SourceTreeRemoveOperation;
import com.sonatype.s2.project.ui.materialization.Images;
import com.sonatype.s2.project.ui.materialization.Messages;

@SuppressWarnings( "restriction" )
public class SourceTreeNode
    extends CodebaseViewNode
{
    private final static Map<String, String> statusMessages = new HashMap<String, String>();

    private final static Map<String, Image> statusImages = new HashMap<String, Image>();

    static
    {
        statusMessages.put( IWorkspaceSourceTree.STATUS_ADDED, Messages.sourceTreeNode_status_added );
        statusMessages.put( IWorkspaceSourceTree.STATUS_CHANGED, Messages.sourceTreeNode_status_changed );
        statusMessages.put( IWorkspaceSourceTree.STATUS_NOT_SUPPORTED, Messages.sourceTreeNode_status_notSupported );
        statusMessages.put( IWorkspaceSourceTree.STATUS_REMOVED, Messages.sourceTreeNode_status_removed );
        statusMessages.put( IWorkspaceSourceTree.STATUS_UPTODATE, Messages.sourceTreeNode_status_upToDate );

        statusImages.put( IWorkspaceSourceTree.STATUS_ADDED,
                          Images.getOverlayImage( "mse-sourcetree.png", "overlay_added.gif", IDecoration.BOTTOM_RIGHT ) ); //$NON-NLS-1$ //$NON-NLS-2$
        statusImages.put( IWorkspaceSourceTree.STATUS_CHANGED,
                          Images.getOverlayImage( "mse-sourcetree.png", "overlay_delta.gif", IDecoration.BOTTOM_RIGHT ) ); //$NON-NLS-1$ //$NON-NLS-2$
        statusImages.put( IWorkspaceSourceTree.STATUS_NOT_SUPPORTED,
                          Images.getOverlayImage( "mse-sourcetree.png", "overlay_error.gif", IDecoration.BOTTOM_RIGHT ) ); //$NON-NLS-1$ //$NON-NLS-2$
        statusImages.put( IWorkspaceSourceTree.STATUS_REMOVED,
                          Images.getOverlayImage( "mse-sourcetree.png", "overlay_removed.gif", IDecoration.BOTTOM_RIGHT ) ); //$NON-NLS-1$ //$NON-NLS-2$
        statusImages.put( IWorkspaceSourceTree.STATUS_UPTODATE, Images.SOURCE_TREE );
    }

    private String status;

    private String statusText;

    private Image image;

    private IWorkspaceSourceTree workspaceSourceTree;

    public SourceTreeNode( IWorkspaceCodebase workspaceCodebase, IWorkspaceSourceTree workspaceSourceTree )
    {
        super( workspaceCodebase, workspaceSourceTree.getName(), workspaceSourceTree.getScmUrl(), Images.SOURCE_TREE );

        this.workspaceSourceTree = workspaceSourceTree;
        this.status = workspaceSourceTree.getStatus();
        this.statusText = workspaceSourceTree.getStatusMessage();

        if ( this.statusText == null )
        {
            this.statusText = statusMessages.get( status );
        }
        this.image = statusImages.get( status );
    }

    @Override
	public String getTooltip() {
    	String tip = null; 
    	if (getStatus() != null ) {
    		tip = "Status: " + getStatus();
        	if (workspaceSourceTree.getStatusHelp() != null) {
        		tip = tip + "\nDescription: " + workspaceSourceTree.getStatusHelp();
        	}
    	}
    	return tip;
	}

	@Override
    public String getStatus()
    {
        return statusText;
    }

    @Override
    public Image getImage(boolean showStatus)
    {
        return image == null || !showStatus ? super.getImage(showStatus) : image;
    }

    @Override
    public boolean isUpdateAvailable()
    {
        return IWorkspaceSourceTree.STATUS_ADDED.equals( status )
            || IWorkspaceSourceTree.STATUS_CHANGED.equals( status )
            || IWorkspaceSourceTree.STATUS_REMOVED.equals( status );
    }
    
    @Override
    public boolean isUpdateUnsupported() 
    {
        return IWorkspaceSourceTree.STATUS_NOT_SUPPORTED.equals( status );
    }

    public IWorkspaceSourceTree getWorkspaceSourceTree()
    {
        return workspaceSourceTree;
    }

    @Override
    public void update()
    {
        final IWorkspaceSourceTree sourceTree = getWorkspaceSourceTree();
        if ( IWorkspaceSourceTree.STATUS_ADDED.equals( status ) )
        {
            new SourceTreeImportJob( getWorkspaceCodebase(), sourceTree ).schedule();
        }
        else if ( IWorkspaceSourceTree.STATUS_CHANGED.equals( status ) )
        {
            new SourceTreeUpdateJob( getWorkspaceCodebase(), sourceTree ).schedule();
        }
        else if ( IWorkspaceSourceTree.STATUS_REMOVED.equals( status ) )
        {
            // REMOVED (past tense), means the source tree was removed both locally and from remove codebase descriptor
            new AbstractCodebaseUpdateJob( getWorkspaceCodebase(), Messages.sourceTreeNode_job_updateCodebase )
            {
                @Override
                protected IStatus doRun( IProgressMonitor monitor )
                    throws CoreException, InterruptedException
                {
                    new SourceTreeRemoveOperation( getWorkspaceCodebase() ).run( monitor );
                    return Status.OK_STATUS;
                }
            }.schedule();
        }
    }
}
