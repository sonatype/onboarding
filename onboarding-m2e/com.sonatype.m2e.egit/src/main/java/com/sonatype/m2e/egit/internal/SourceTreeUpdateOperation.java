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
package com.sonatype.m2e.egit.internal;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.CheckoutConflictException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import com.sonatype.s2.project.core.team.TeamOperationResult;

public class SourceTreeUpdateOperation
    extends AbstractSyncOperation
{

    protected SourceTreeUpdateOperation( Repository repository )
        throws IOException
    {
        super( repository );
    }

    public TeamOperationResult update( IProgressMonitor monitor )
        throws IOException, CoreException
    {
        Ref remoteRef = getRemoteRef( remoteName, remoteRefspec );

        MergeOperation op = new MergeOperation( repository, remoteRef.getName() );

        try
        {
            op.execute( monitor );
        }
        catch ( JGitInternalException e )
        {
            if ( isMergeConflict( e ) )
            {
                // git refuses to merge incoming commit if it conflicts with local uncommitted changes
                return notSupported( Messages.hardMergeConflicts, Messages.hardMergeConflictsHelp,
                                     repository.getDirectory() );
            }
            throw new CoreException( new Status( IStatus.ERROR, getClass().getName(), e.getMessage(), e ) );
        }

        switch ( op.getResult().getMergeStatus() )
        {
            case CONFLICTING:
                return notSupported( Messages.mergeConflicts, Messages.mergeConflictsHelp, repository.getDirectory() );
            case FAST_FORWARD:
            case ALREADY_UP_TO_DATE:
            case MERGED:
                return TeamOperationResult.RESULT_UPTODATE;
            case FAILED:
            case NOT_SUPPORTED:
            default:
                return notSupported( Messages.mergeFailed, Messages.mergeFailedHelp, repository.getDirectory(),
                                     op.getResult().getMergeStatus() );
        }
    }

    private boolean isMergeConflict( JGitInternalException e )
    {
        return e.getCause() instanceof CheckoutConflictException;
    }
}
