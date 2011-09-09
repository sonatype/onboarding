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
package com.sonatype.s2.project.ui.materialization;

import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IS2CodebaseChangeEventListener;
import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.S2CodebaseChangeEvent;
import com.sonatype.s2.project.core.internal.update.AbstractSourceTreeOperation;

@SuppressWarnings( "restriction" )
public class WorkingSetMaterializationCallback
    implements IS2CodebaseChangeEventListener
{
    private static final Logger log = LoggerFactory.getLogger( WorkingSetMaterializationCallback.class );

    private static final String JAVA_WORKING_SET = "org.eclipse.jdt.ui.JavaWorkingSetPage";

    public void codebaseChanged( S2CodebaseChangeEvent event )
    {
        IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();

        for ( IWorkspaceSourceTree tree : event.getCodebase().getSourceTrees() )
        {
            IProject[] projects = getProjects( tree );

            String workingSetName = tree.getName();
            IWorkingSet workingSet = workingSetManager.getWorkingSet( workingSetName );

            if ( workingSet != null )
            {
                log.info( "Adding {} project(s) to working set '{}'", projects.length, workingSetName );
                workingSet.setElements( projects );
            }
            else
            {
                log.info( "Creating java working set '{}' with {} project(s)", workingSetName, projects.length );
                workingSet = workingSetManager.createWorkingSet( workingSetName, projects );
                workingSetManager.addWorkingSet( workingSet );

                workingSet.setId( JAVA_WORKING_SET );
            }
        }
    }

    private IProject[] getProjects( IWorkspaceSourceTree tree )
    {
        return AbstractSourceTreeOperation.getWorkspaceProjects( tree ).values().toArray( new IProject[0] );
    }
}
