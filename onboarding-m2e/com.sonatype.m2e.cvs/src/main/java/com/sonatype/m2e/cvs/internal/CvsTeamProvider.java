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

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation;
import org.eclipse.team.internal.ccvs.ui.operations.UpdateOnlyMergableOperation;
import org.eclipse.ui.IWorkbenchPart;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.internal.update.AbstractSourceTreeOperation;
import com.sonatype.s2.project.core.team.ITeamProvider;
import com.sonatype.s2.project.core.team.TeamOperationResult;

@SuppressWarnings( "restriction" )
public class CvsTeamProvider
    implements ITeamProvider
{
    private static final Logger log = LoggerFactory.getLogger( CvsTeamProvider.class );

    public TeamOperationResult getUpdateStatus( IWorkspaceSourceTree sourceTree, IProgressMonitor monitor )
        throws CoreException
    {
        log.debug( "Getting update status for source tree '{}', location '{}'", sourceTree.getName(),
                   sourceTree.getLocation() );

        setPassword( sourceTree );

        boolean shouldRemoveProject = false;
        IProject project = getExistingRootProject( sourceTree, monitor );
        if ( project == null )
        {
            project = createRoot( sourceTree, monitor );
            shouldRemoveProject = true;
        }

        try
        {
            CvsCheckUpdateStatusOperation op =
                new CvsCheckUpdateStatusOperation( null, RepositoryProviderOperation.asResourceMappers( project.members() ),
                                   new LocalOption[0] );
            try
            {
                op.run( monitor );
            }
            catch ( Exception e )
            {
                throw new CoreException( new Status( IStatus.ERROR, getClass().getName(), e.getMessage(), e ) );
            }

            if ( op.hasIncomingChanges() )
            {
                return TeamOperationResult.RESULT_CHANGED;
            }
            else
            {
                return cvsOperationStatus2TeamOperationResult( op.getErrors() );
            }
        }
        finally
        {
            if ( shouldRemoveProject )
            {
                project.delete( false, true, monitor );
            }
        }
    }

    private TeamOperationResult cvsOperationStatus2TeamOperationResult( IStatus[] errors )
    {
        if ( errors == null || errors.length == 0 )
        {
            return TeamOperationResult.RESULT_UPTODATE;
        }
        for ( IStatus error : errors )
        {
            if ( error.getCode() == CVSStatus.AUTHENTICATION_FAILURE )
            {
                return TeamOperationResult.RESULT_UNAUTHORIZED;
            }
        }
        return new TeamOperationResult( "ERROR", errors[0].toString(), null /* help */);
    }

    private static class _UpdateOnlyMergableOperation
        extends UpdateOnlyMergableOperation
    {
        public _UpdateOnlyMergableOperation( IWorkbenchPart part, IProject project, IResource[] resources,
                                             LocalOption[] localOptions )
        {
            super( part, project, resources, localOptions );
        }

        @Override
        protected IStatus[] getErrors()
        {
            return super.getErrors();
        }
    }

    public TeamOperationResult updateFromRepository( IWorkspaceSourceTree sourceTree, IProgressMonitor monitor )
        throws CoreException
    {
        log.debug( "Updating from repository for source tree '{}', location '{}'", sourceTree.getName(),
                   sourceTree.getLocation() );

        CVSProviderPlugin.getPlugin().setAutoshareOnImport( true );

        setPassword( sourceTree );

        boolean shouldRemoveProject = false;
        IProject project = getExistingRootProject( sourceTree, monitor );
        if ( project == null )
        {
            project = createRoot( sourceTree, monitor );
            shouldRemoveProject = true;
        }
        // Using UpdateOperation results in possibly undesirable behaviour, conflicting files are merged but further
        // updates are impossible until the conflict is resolved
        _UpdateOnlyMergableOperation op =
            new _UpdateOnlyMergableOperation( null, project, new IResource[] { project }, new LocalOption[0] );
        try
        {
            op.run( monitor );
            IStatus[] errors = op.getErrors();
            if ( errors != null && errors.length > 0 )
            {
                if ( shouldRemoveProject )
                {
                    project.delete( false, true, monitor );
                }
                return cvsOperationStatus2TeamOperationResult( errors );
            }
        }
        catch ( Exception e )
        {
            if ( shouldRemoveProject )
            {
                project.delete( false, true, monitor );
            }
            throw new CoreException( new Status( IStatus.ERROR, getClass().getName(),
                                                 "An error occurred updating from CVS.", getCause( e ) ) );
        }

        CVSProviderPlugin.getPlugin().getChangeSetManager();
        return op.getSkippedFiles().length > 0 ? new TeamOperationResult( IWorkspaceSourceTree.STATUS_NOT_SUPPORTED,
                                                                          "Merge conflicts", null )
                        : TeamOperationResult.RESULT_UPTODATE;
    }

    public void afterProjectsImport( List<IMavenProjectImportResult> projectImportResults, File location,
                                     IProgressMonitor monitor )
        throws CoreException
    {
        // nothing required
    }

    /*
     * Get the IProjects for a IWorkspaceSourceTree
     */
    static IProject[] getProjects( IWorkspaceSourceTree sourceTree )
    {
        Collection<IProject> projects = AbstractSourceTreeOperation.getWorkspaceProjects( sourceTree ).values();

        return projects.toArray( new IProject[projects.size()] );
    }

    /*
     * Create a project in the SourceTree root
     */
    private static IProject createRoot( IWorkspaceSourceTree sourceTree, IProgressMonitor monitor )
        throws CoreException
    {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject( UUID.randomUUID().toString() );
        project.create( monitor );
        IProjectDescription description = project.getDescription();
        description.setLocation( Path.fromOSString( sourceTree.getLocation() ) );
        return project;
    }

    /*
     * Get the existing root if the project exists
     */
    private static IProject getExistingRootProject( IWorkspaceSourceTree sourceTree, IProgressMonitor monitor )
    {
        IPath root = Path.fromOSString( sourceTree.getLocation() );
        for ( IProject project : getProjects( sourceTree ) )
        {
            if ( root.equals( project.getLocation() ) )
            {
                return project;
            }
        }
        return null;
    }

    private Exception getCause( Throwable e )
    {
        if ( e.getCause() instanceof CVSException )
            return (CVSException) e.getCause();
        else if ( e.getCause() == null )
            return e instanceof Exception ? (Exception) e : null;
        return getCause( e.getCause() );
    }

    public static void setPassword( IWorkspaceSourceTree tree )
    {
        try
        {
            IAuthData data = AuthFacade.getAuthService().select( tree.getScmUrl() );
            if ( data == null || ( data.getUsername().equals( "" ) && data.getPassword().equals( "" ) ) )
            {
                return;
            }
            for ( IProject project : CvsTeamProvider.getProjects( tree ) )
            {
                RepositoryProvider provider = RepositoryProvider.getProvider( project );
                if ( provider instanceof CVSTeamProvider )
                {
                    ICVSRepositoryLocation location = ( (CVSTeamProvider) provider ).getRemoteLocation();
                    location.setUsername( data.getUsername() );
                    location.setPassword( data.getPassword() );
                    // All projects in a IWorkspaceSourceTree should be associated with the same location
                    break;
                }
            }
        }
        catch ( CVSException e )
        {
            log.error( "Failed to set password for source tree: " + tree.getName(), e );
            throw new RuntimeException( e );
        }
    }
}
