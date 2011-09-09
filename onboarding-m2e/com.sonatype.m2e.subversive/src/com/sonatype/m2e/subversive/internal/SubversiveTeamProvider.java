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

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.team.svn.core.connector.SVNChangeStatus;
import org.eclipse.team.svn.core.connector.SVNConnectorAuthenticationException;
import org.eclipse.team.svn.core.connector.SVNEntryStatus;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.file.RemoteStatusOperation;
import org.eclipse.team.svn.core.operation.file.SVNFileStorage;
import org.eclipse.team.svn.core.operation.file.UpdateOperation;
import org.eclipse.team.svn.core.operation.local.IUnresolvedConflictDetector;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.operation.remote.management.SaveRepositoryLocationsOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.ResourcesParentsProvider;
import org.eclipse.team.svn.core.utility.ILoggedOperationFactory;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.operation.ClearUpdateStatusesOperation;

import com.sonatype.m2e.subversive.SubversiveHelper;
import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.internal.update.AbstractSourceTreeOperation;
import com.sonatype.s2.project.core.team.ITeamProvider;
import com.sonatype.s2.project.core.team.TeamOperationResult;

public class SubversiveTeamProvider
    implements ITeamProvider
{
    /*
     * (non-Javadoc)
     * @see
     * com.sonatype.s2.project.core.team.ITeamProvider#getUpdateStatus(com.sonatype.s2.project.core.IWorkspaceSourceTree
     * , org.eclipse.core.runtime.IProgressMonitor)
     */
    public TeamOperationResult getUpdateStatus( IWorkspaceSourceTree sourceTree, IProgressMonitor monitor )
        throws CoreException
    {
        SubversiveHelper.installNonInteractiveOptionProvider();
        try
        {
            setCredentials( new File( sourceTree.getLocation() ), sourceTree.getScmUrl() );
            File[] files = new File[] { new File( sourceTree.getLocation() ) };

            // Get the version of the remote root
            RemoteStatusOperation remoteOp = new RemoteStatusOperation( files, true );
            remoteOp.run( monitor );
            if ( !remoteOp.getStatus().isOK() )
            {
                return svnOperationStatus2TeamOperationResult( remoteOp.getStatus() );
            }

            SVNChangeStatus[] statuses = remoteOp.getStatuses();
            String returnStatus = IWorkspaceSourceTree.STATUS_UPTODATE;
            for ( SVNChangeStatus status : statuses )
            {
                if ( ( status.repositoryTextStatus == SVNEntryStatus.Kind.MODIFIED && status.textStatus == SVNEntryStatus.Kind.MODIFIED )
                    || status.textStatus == SVNEntryStatus.Kind.CONFLICTED )
                {
                    returnStatus = IWorkspaceSourceTree.STATUS_NOT_SUPPORTED;
                    break;
                }
                else if ( status.repositoryTextStatus == SVNEntryStatus.Kind.MODIFIED
                    || status.repositoryTextStatus == SVNEntryStatus.Kind.ADDED )
                {
                    returnStatus = IWorkspaceSourceTree.STATUS_CHANGED;
                }
            }
            return new TeamOperationResult( returnStatus, null, null );
        }
        finally
        {
            SubversiveHelper.restoreOptionProvider();
        }
    }

    /*
     * (non-Javadoc)
     * @see com.sonatype.s2.project.core.team.ITeamProvider#updateFromRepository(com.sonatype.s2.project.core.
     * IWorkspaceSourceTree, org.eclipse.core.runtime.IProgressMonitor)
     */
    public TeamOperationResult updateFromRepository( IWorkspaceSourceTree sourceTree, IProgressMonitor monitor )
        throws CoreException
    {
        SubversiveHelper.installNonInteractiveOptionProvider();
        try
        {
            IProject[] projects = getProjects( sourceTree );
            setCredentials( new File( sourceTree.getLocation() ), sourceTree.getScmUrl() );
            // Logic here is similar to org.eclipse.team.svn.ui.action.local.UpdateAction
            UpdateOperation mainOp =
                new UpdateOperation( new File[] { new File( sourceTree.getLocation() ) }, SVNRevision.HEAD, false,
                                     false );
            ConflictDetector conflictDetector = new ConflictDetector( mainOp );
            CompositeOperation op = new CompositeOperation( mainOp.getId(), mainOp.getMessagesClass() );

            SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation( projects );
            op.add( saveOp );
            op.add( mainOp );
            op.add( new RestoreProjectMetaOperation( saveOp ) );
            op.add( new ClearUpdateStatusesOperation( projects ), new IActionOperation[] { mainOp } );
            op.add( new RefreshResourcesOperation( new ResourcesParentsProvider( projects ) ) );

            // Uncommenting the below line will result in a popup in the event of merge conflicts
            // op.add( new NotifyUnresolvedConflictOperation( conflictDetector ) );
            try
            {
                op.run( monitor );
            }
            catch ( Exception e )
            {
                throw new CoreException( new Status( IStatus.ERROR, getClass().getName(), e.getMessage(), e ) );
            }

            if ( conflictDetector.hasUnresolvedConflicts() )
            {
                return new TeamOperationResult( IWorkspaceSourceTree.STATUS_NOT_SUPPORTED, "Merge conflicts", null );
            }
            else
            {
                return svnOperationStatus2TeamOperationResult( op.getStatus() );
            }
        }
        finally
        {
            SubversiveHelper.restoreOptionProvider();
        }
    }

    private TeamOperationResult svnOperationStatus2TeamOperationResult( IStatus svnOperationStatus )
    {
        if ( svnOperationStatus.isOK() )
        {
            return TeamOperationResult.RESULT_UPTODATE;
        }
        if ( isUnauthorizedStatus( svnOperationStatus ) )
        {
            return TeamOperationResult.RESULT_UNAUTHORIZED;
        }

        return new TeamOperationResult( "ERROR", svnOperationStatus.toString(), null /* help */);
    }

    private boolean isUnauthorizedStatus( IStatus svnOperationStatus )
    {
        Throwable e = svnOperationStatus.getException();
        if ( e instanceof SVNConnectorAuthenticationException )
        {
            return true;
        }

        if ( svnOperationStatus.isMultiStatus() )
        {
            for ( IStatus childStatus : svnOperationStatus.getChildren() )
            {
                if ( isUnauthorizedStatus( childStatus ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * @see com.sonatype.s2.project.core.team.ITeamProvider#afterProjectsImport(java.util.List, java.io.File,
     * org.eclipse.core.runtime.IProgressMonitor)
     */
    public void afterProjectsImport( List<IMavenProjectImportResult> projectImportResults, File location,
                                     IProgressMonitor monitor )
        throws CoreException
    {
    }

    /*
     * Get the IProject from the IWorkspaceSourceTree
     */
    public static IProject[] getProjects( IWorkspaceSourceTree sourceTree )
    {
        Collection<IProject> projects = AbstractSourceTreeOperation.getWorkspaceProjects( sourceTree ).values();

        return projects.toArray( new IProject[projects.size()] );
    }

    /*
     * Set the authentication information for the RepositoryLocation
     */
    private static void setCredentials( File root, String scmUrl )
        throws CoreException
    {
        if ( scmUrl.startsWith( SubversiveHelper.SVN_SCM_ID ) )
        {
            scmUrl = scmUrl.substring( SubversiveHelper.SVN_SCM_ID.length() );
        }
        boolean needsSave = false;

        // Subversive file operations use an IRepositoryLocation which references the SVN root path, so we need to set
        // the credentials there
        IRepositoryResource svnRepositoryResource = SVNFileStorage.instance().asRepositoryResource( root, false );
        IRepositoryLocation svnRepositoryLocation = svnRepositoryResource.getRepositoryLocation();
        if ( svnRepositoryLocation != null )
        {
            try
            {
                if ( SubversiveHelper.setCredentials( scmUrl, svnRepositoryLocation ) )
                {
                    needsSave = true;
                }
            }
            catch ( MalformedURLException e )
            {
                throw new CoreException( new Status( IStatus.ERROR, "SubversiveTeamProvider", 0, "Invalid url "
                    + scmUrl, e ) );
            }
        }

        // Set the credentials on the svn remote resource too
        IRepositoryResource remoteSvnRepositoryResource = SVNUtility.asRepositoryResource( scmUrl, true );
        if ( remoteSvnRepositoryResource == null )
        {
            return;
        }
        IRepositoryLocation remoteSvnRepositoryLocation = remoteSvnRepositoryResource.getRepositoryLocation();
        if ( remoteSvnRepositoryLocation != null )
        {
            try
            {
                if ( SubversiveHelper.setCredentials( scmUrl, remoteSvnRepositoryLocation ) )
                {
                    needsSave = true;
                }
            }
            catch ( MalformedURLException e )
            {
                throw new CoreException( new Status( IStatus.ERROR, "SubversiveTeamProvider", 0, "Invalid url "
                    + scmUrl, e ) );
            }
        }

        if ( needsSave )
        {
            SaveRepositoryLocationsOperation saveOp = new SaveRepositoryLocationsOperation();
            ProgressMonitorUtility.doTaskExternal( saveOp, new NullProgressMonitor(), ILoggedOperationFactory.EMPTY );
            IStatus status = saveOp.getStatus();
            if ( status != null && !status.isOK() )
            {
                throw new CoreException( status );
            }
        }
    }

    /*
     * Bridge between the file level UpdateOperation and conflicts which are visible in the workspace
     */
    private static class ConflictDetector
        implements IUnresolvedConflictDetector
    {
        private UpdateOperation op;

        private List<IResource> unprocessed;

        public ConflictDetector( UpdateOperation op )
        {
            this.op = op;
        }

        public boolean hasUnresolvedConflicts()
        {
            unprocessed = null;
            if ( !op.hasUnresolvedConflicts() )
            {
                return false;
            }
            // The unresolved conflicts could exist outside the visible projects, so we need to ensure the list of
            // IResources is non-empty.
            if ( unprocessed == null )
            {
                getUnprocessed();
            }
            return !unprocessed.isEmpty();
        }

        public IResource[] getUnprocessed()
        {
            if ( unprocessed == null )
            {
                unprocessed = getResources( op.getUnprocessed() );
            }
            return unprocessed.toArray( new IResource[unprocessed.size()] );
        }

        public IResource[] getProcessed()
        {
            List<IResource> resources = getResources( op.getProcessed() );
            return resources.toArray( new IResource[resources.size()] );
        }

        public String getMessage()
        {
            return op.getMessage();
        }

        /*
         * Convert an array of Files into an array of IResources
         */
        private List<IResource> getResources( File[] files )
        {
            List<IResource> resources = new ArrayList<IResource>();
            for ( File file : files )
            {
                IResource resource =
                    ResourcesPlugin.getWorkspace().getRoot().findMember( new Path( file.toString() ).makeRelativeTo( ResourcesPlugin.getWorkspace().getRoot().getLocation() ) );
                if ( resource != null )
                {
                    resources.add( resource );
                }
            }
            return resources;
        }

        /* The methods below should be unused */
        public void addUnprocessed( IResource unprocessed )
        {
        }

        public void removeProcessed( IResource resource )
        {
        }

        public void setConflictMessage( String message )
        {
        }

        public void setUnresolvedConflict( boolean hasUnresolvedConflict )
        {
        }
    }
}
