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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.team.ITeamProvider;
import com.sonatype.s2.project.core.team.TeamOperationResult;
import com.sonatype.s2.project.validation.git.JSchSecurityContext;

public class EgitTeamProvider
    implements ITeamProvider
{
    private static final Logger log = LoggerFactory.getLogger( EgitTeamProvider.class );

    public TeamOperationResult getUpdateStatus( IWorkspaceSourceTree sourceTree, IProgressMonitor monitor )
        throws CoreException
    {
        log.debug( "Getting update status for source tree '{}', location '{}'", sourceTree.getName(),
                   sourceTree.getLocation() );
        try
        {
            Repository gitRepository = getRepository( sourceTree );
            log.debug( "Using git repository '{}'", gitRepository );
            IAuthData authData = AuthFacade.getAuthService().select( sourceTree.getScmUrl() );
            JSchSecurityContext secCtx = JSchSecurityContext.enter( authData );
            try
            {
                return new SourceTreeStatusOperation( gitRepository ).getUpdateStatus( monitor );
            }
            finally
            {
                secCtx.leave();
            }
        }
        catch ( IOException e )
        {
            log.debug( e.getMessage(), e );
            if ( isUnauthorizedException( e ) )
            {
                return TeamOperationResult.RESULT_UNAUTHORIZED;
            }
            return new TeamOperationResult( "ERROR", e.getMessage(), null /* help */);
        }
        catch ( URISyntaxException e )
        {
            log.debug( e.getMessage(), e );
            return new TeamOperationResult( "ERROR", e.getMessage(), null /* help */);
        }
    }

    private boolean isUnauthorizedException( Exception e )
    {
        if ( e.getMessage() == null )
        {
            return false;
        }
        return e.getMessage().indexOf( "Auth cancel" ) >= 0;
    }

    public TeamOperationResult updateFromRepository( IWorkspaceSourceTree tree, IProgressMonitor monitor )
        throws CoreException
    {
        log.debug( "Updating from repository for source tree '{}', location '{}'", tree.getName(), tree.getLocation() );
        try
        {
            Repository gitRepository = getRepository( tree );
            log.debug( "Using git repository '{}'", gitRepository );
            return new SourceTreeUpdateOperation( gitRepository ).update( monitor );
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, getClass().getName(), e.getMessage(), e ) );
        }
    }

    public void afterProjectsImport( List<IMavenProjectImportResult> projectImportResults, File scmRepositoryLocation,
                                     IProgressMonitor monitor )
        throws CoreException
    {
        log.debug( "Connecting {} projects to git repository {}", projectImportResults.size(),
                   scmRepositoryLocation.getAbsolutePath() );
        File gitRepository = new File( scmRepositoryLocation, ".git" );
        if ( !gitRepository.exists() )
        {
            throw new CoreException( new Status( IStatus.ERROR, getClass().getName(), "Git repository does not exist: "
                + gitRepository.getAbsolutePath() ) );
        }

        Set<IProject> projects = new LinkedHashSet<IProject>();
        for ( IMavenProjectImportResult projectImportResult : projectImportResults )
        {
            IProject project = projectImportResult.getProject();
            if ( project != null )
            {
                log.debug( "Connecting project {}", project.getName() );
                projects.add( project );
            }
        }
        connectProjectsToGitRepository( projects, gitRepository, monitor );
    }

    public void connectProjectsToGitRepository( Set<IProject> projects, File gitRepository, IProgressMonitor monitor )
        throws CoreException
    {
        Map<IProject, File> projectsMappedToRepositories = new LinkedHashMap<IProject, File>();
        for ( IProject project : projects )
        {
            projectsMappedToRepositories.put( project, gitRepository );
        }
        ConnectProviderOperation connectProviderOperation = new ConnectProviderOperation( projectsMappedToRepositories );
        connectProviderOperation.execute( monitor );
    }

    private Repository getRepository( IWorkspaceSourceTree sourceTree )
    {
        IPath root = Path.fromOSString( sourceTree.getLocation() );

        for ( IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects() )
        {
            if ( root.isPrefixOf( project.getLocation() ) )
            {
                RepositoryMapping repositoryMapping = RepositoryMapping.getMapping( project );
                if ( repositoryMapping != null && repositoryMapping.getRepository() != null )
                {
                    return repositoryMapping.getRepository();
                }
            }
        }

        throw new IllegalStateException( "Cannot find git repository for source tree '" + sourceTree.getName() + "'" );
    }

}
