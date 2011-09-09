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
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.treewalk.FileTreeIterator;

import com.sonatype.s2.project.core.team.TeamOperationResult;

public class SourceTreeStatusOperation
    extends AbstractSyncOperation
{
    protected SourceTreeStatusOperation( Repository repository )
        throws IOException
    {
        super( repository );
    }

    public TeamOperationResult getUpdateStatus( IProgressMonitor monitor )
        throws IOException, URISyntaxException
    {
        String location = repository.getDirectory().getAbsolutePath();

        if ( remoteName == null || remoteRefspec == null )
        {
            return notSupported( Messages.noUpstreamBranch, Messages.noUpstreamBranchHelp, branch, location );
        }

        // if ( repository == null )
        // {
        // return notSupported( "Could not find git local repository %s.", location );
        // }

        String branch = repository.getBranch();

        Ref headRef = repository.getRef( Constants.R_HEADS + "/" + branch );

        if ( headRef == null )
        {
            return notSupported( Messages.unknownLocalBranch, Messages.unknownLocalBranchHelp, branch, location );
        }

        RemoteConfig remoteConfig = getRemoteConfig( remoteName );

        if ( remoteConfig == null )
        {
            return notSupported( Messages.unknownRemote, Messages.unknownRemoteHelp, remoteName, location );
        }

        fetchFromRemote( remoteConfig, monitor );

        Ref remoteRef = getRemoteRef( remoteName, remoteRefspec );

        if ( remoteRef == null )
        {
            return notSupported( Messages.unknownRefspec, Messages.unknownRefspecHelp, remoteRefspec, remoteName,
                                 location );
        }

        // TODO deal with bad git repository states

        // now lets check if we have incoming commits (see corresponding logic in org.eclipse.jgit.api.MergeCommand)
        RevWalk rw = new RevWalk( repository );

        try
        {
            RevCommit headCommit = rw.lookupCommit( headRef.getObjectId() );
            RevCommit remoteCommit = rw.lookupCommit( remoteRef.getObjectId() );

            if ( rw.isMergedInto( remoteCommit, headCommit ) )
            {
                return TeamOperationResult.RESULT_UPTODATE;
            }

            // we know we have incoming commits at this point, lets see if we can handle them

            IndexDiff diff = new IndexDiff( repository, Constants.HEAD, new FileTreeIterator( repository ) );
            diff.diff();
            if ( !diff.getChanged().isEmpty() )
            {
                return notSupported( Messages.stagedChanges, Messages.stagedChangesHelp, location );
            }

            if ( rw.isMergedInto( headCommit, remoteCommit ) )
            {
                return TeamOperationResult.RESULT_CHANGED; // fast-forward
            }

            // real merge
            return notSupported( Messages.localCommits, Messages.localCommitsHelp, location );
        }
        finally
        {
            rw.dispose();
        }
    }
}
