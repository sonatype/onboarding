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
package com.sonatype.s2.project.core.internal.update;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.ide.IIDEUpdater;
import com.sonatype.s2.project.core.internal.S2ProjectPlugin;
import com.sonatype.s2.project.core.internal.WorkspaceCodebase;

public class CodebaseUpdateOperation
{
    private static final Logger log = LoggerFactory.getLogger( CodebaseUpdateOperation.class );

    private final IWorkspaceCodebase originalCodebase;

    private final IWorkspaceCodebase codebase;

    public CodebaseUpdateOperation( IWorkspaceCodebase originalCodebase )
    {
        this.originalCodebase = originalCodebase;
        this.codebase = originalCodebase.getPending();

        if ( this.codebase == null )
        {
            throw new IllegalArgumentException();
        }

        if ( ( (WorkspaceCodebase) codebase ).getS2Project() == null )
        {
            throw new IllegalArgumentException();
        }
    }

    public void run( IProgressMonitor monitor )
        throws CoreException
    {
        log.info( "Started codebase update." );
        List<IStatus> exceptions = new ArrayList<IStatus>();
        List<IUpdateOperation> operations = getOperations();

        for ( IUpdateOperation op : operations )
        {
            log.debug( "Calling update operation {}", op.getClass().getCanonicalName() );
            try
            {
                op.run( monitor );
            }
            catch ( CoreException e )
            {
                exceptions.add( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                            "Exception occured during the automatic update.", e ) );
            }
            catch ( InterruptedException e )
            {
                exceptions.add( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                            "Exception occured during the automatic update.", e ) );
            }
        }

        if ( !exceptions.isEmpty() )
        {
            throw new CoreException( new MultiStatus( S2ProjectPlugin.PLUGIN_ID, 0,
                                                      exceptions.toArray( new IStatus[exceptions.size()] ),
                                                      "Exception occured during the automatic update.", null ) );
        }

        S2ProjectCore.getInstance().replaceWorkspaceCodebase( originalCodebase, codebase );
        log.info( "Finished codebase update successfully." );
    }

    public List<IUpdateOperation> getOperations()
        throws CoreException
    {
        List<IUpdateOperation> operations = new ArrayList<IUpdateOperation>();

        if ( IIDEUpdater.NOT_UP_TO_DATE.equals( originalCodebase.getIsP2LineupUpToDate() ) )
        {
            operations.add( new P2LineupUpdateOperation( codebase ) );
            log.debug( "IDE update detected" ); //$NON-NLS-1$
            return operations;
        }

        for ( IWorkspaceSourceTree tree : codebase.getSourceTrees() )
        {
            if ( IWorkspaceSourceTree.STATUS_UPTODATE.equals( tree.getStatus() ) )
            {
                // nothing to do
            }
            else if ( IWorkspaceSourceTree.STATUS_REMOVED.equals( tree.getStatus() ) )
            {
                // we do not automatically remove local copy yet
                // model update is handled by S2ProjectCore.replaceWorkspaceCodebase
            }
            else if ( IWorkspaceSourceTree.STATUS_ADDED.equals( tree.getStatus() ) )
            {
                operations.add( new SourceTreeImportOperation( tree ) );
            }
            else if ( IWorkspaceSourceTree.STATUS_CHANGED.equals( tree.getStatus() ) )
            {
                operations.add( new SourceTreeUpdateOperation( tree ) );
            }
            else
            {
                throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                     "Cannot update source tree " + tree.getLocation() ) );
            }
        }

        return operations;
    }

    public IStatus getStatus()
    {
        // TODO Auto-generated method stub
        return Status.OK_STATUS;
    }
}
