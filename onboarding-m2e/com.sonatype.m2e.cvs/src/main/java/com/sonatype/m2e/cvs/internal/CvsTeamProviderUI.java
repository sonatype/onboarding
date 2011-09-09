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
package com.sonatype.m2e.cvs.internal;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.actions.CommitAction;
import org.eclipse.team.internal.ccvs.ui.mappings.WorkspaceModelParticipant;
import org.eclipse.team.internal.ccvs.ui.mappings.WorkspaceSubscriberContext;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.IWorkbenchPage;

import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.ui.materialization.ITeamProviderUI;

@SuppressWarnings( "restriction" )
public class CvsTeamProviderUI
    implements ITeamProviderUI
{
    /*
     * (non-Javadoc)
     * @see com.sonatype.s2.project.ui.materialization.ITeamProviderUI#synchronize(com.sonatype.s2.project.core.
     * IWorkspaceSourceTree)
     */
    public void synchronize( IWorkspaceSourceTree tree )
    {
        CvsTeamProvider.setPassword( tree );

        // Code is based on SyncAction
        IWorkbenchPage page = TeamUIPlugin.getActivePage();
        if ( page != null )
        {
            Shell shell = page.getActivePart().getSite().getShell();

            ResourceMapping[] mappings = getResourceMapping( tree );
            if ( mappings.length == 0 )
                return;
            SubscriberScopeManager manager =
                WorkspaceSubscriberContext.createWorkspaceScopeManager( mappings,
                                                                        true,
                                                                        CommitAction.isIncludeChangeSets( shell,
                                                                                                          CVSUIMessages.SyncAction_1 ) );
            WorkspaceSubscriberContext context =
                WorkspaceSubscriberContext.createContext( manager, ISynchronizationContext.THREE_WAY );
            WorkspaceModelParticipant participant = new WorkspaceModelParticipant( context );
            TeamUI.getSynchronizeManager().addSynchronizeParticipants( new ISynchronizeParticipant[] { participant } );
            participant.run( page.getActivePart() );
        }
    }

    private ResourceMapping[] getResourceMapping( IWorkspaceSourceTree tree )
    {
        ArrayList<ResourceMapping> mappings = new ArrayList<ResourceMapping>();
        for ( IProject project : CvsTeamProvider.getProjects( tree ) )
        {
            ResourceMapping mapping = Utils.getResourceMapping( project );
            if ( mapping != null )
            {
                mappings.add( mapping );
            }
        }
        return mappings.toArray( new ResourceMapping[mappings.size()] );
    }
}
