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
package com.sonatype.m2e.subversive.internal;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.svn.ui.operation.ShowUpdateViewOperation;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.ui.materialization.ITeamProviderUI;

public class SubversiveTeamProviderUI
    implements ITeamProviderUI
{
    public void synchronize( IWorkspaceSourceTree tree )
    {
        IWorkbenchPage page = TeamUIPlugin.getActivePage();
        if ( page != null )
        {
            IWorkbenchPart targetPart = page.getActivePart();
            ShowUpdateViewOperation op = new ShowUpdateViewOperation( getResourceMapping( tree ), targetPart );
            op.run( new NullProgressMonitor() );
        }
    }

    private ResourceMapping[] getResourceMapping( IWorkspaceSourceTree tree )
    {
        ArrayList<ResourceMapping> mappings = new ArrayList<ResourceMapping>();
        for ( IProject project : SubversiveTeamProvider.getProjects( tree ) )
        {
            ResourceMapping mapping = Utils.getResourceMapping( project );
            if ( mapping != null )
            {
                mappings.add( mapping );
            }
        }
        return mappings.toArray( new ResourceMapping[ mappings.size() ]);
    }

}
