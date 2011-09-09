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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.internal.WorkspaceSourceTree;
import com.sonatype.s2.project.core.team.ITeamProvider;
import com.sonatype.s2.project.core.team.TeamOperationResult;

/**
 * @author igor
 */
public class SourceTreeUpdateOperation
    extends AbstractSourceTreeOperation
    implements IUpdateOperation
{
    private static final Logger log = LoggerFactory.getLogger( SourceTreeUpdateOperation.class );
    private final boolean updateFromRepository;

    public SourceTreeUpdateOperation( IWorkspaceSourceTree tree )
    {
        this( tree, true );
    }

    public SourceTreeUpdateOperation( IWorkspaceSourceTree tree, boolean updateFromRepository )
    {
        super( tree );
        this.updateFromRepository = updateFromRepository;
    }

    public void run( IProgressMonitor monitor )
        throws InterruptedException, CoreException
    {
        log.info( "Updating source tree {}.", sourceTree.getLocation() );

        SubMonitor subProgress = SubMonitor.convert( monitor );

        File location = new File( sourceTree.getLocation() );

        Map<IPath, IProject> oldProjects = getWorkspaceProjects( sourceTree );

        ITeamProvider teamProvider = getTeamProvider( sourceTree.getScmUrl() );
        if ( teamProvider == null )
        {
            log.warn( "Cannot find team provider for {}", sourceTree.getScmUrl() );
        }

        if ( updateFromRepository && teamProvider != null)
        {
            log.debug( "Updating source tree {} from source code repository", location );

            // update from source repository, refresh workspace as necessary
            TeamOperationResult teamResult = teamProvider.updateFromRepository( sourceTree, monitor );
            log.info( "Source tree update result: {}.", teamResult );
            ( (WorkspaceSourceTree) sourceTree ).setStatus( teamResult.getStatus() );
            ( (WorkspaceSourceTree) sourceTree ).setStatusMessage( teamResult.getMessage() );
            ( (WorkspaceSourceTree) sourceTree ).setStatusMessage( teamResult.getHelp() );
        }

        // discover projects. MUST use the same impl as during materialization
        ProjectImportConfiguration configuration = newProjectImportConfiguration();

        Map<IPath, MavenProjectInfo> newProjects = getNewProjects( location, subProgress );

        // remove all removed projects
        for ( Map.Entry<IPath, IProject> entry : oldProjects.entrySet() )
        {
            if ( !newProjects.containsKey( entry.getKey() ) && !new File( entry.getKey().toFile(), "pom.xml" ).exists() )
            {
                // pom's gone, remove the project
                entry.getValue().delete( false, true, monitor );
            }
        }

        // add all added projects
        List<MavenProjectInfo> projectInfos = new ArrayList<MavenProjectInfo>();
        for ( Map.Entry<IPath, MavenProjectInfo> entry : newProjects.entrySet() )
        {
            if ( !oldProjects.containsKey( entry.getKey() ) )
            {
                projectInfos.add( entry.getValue() );
            }
        }

        if ( !projectInfos.isEmpty() )
        {
            log.debug( "Importing {} materialized projects", projectInfos.size() );
            List<IMavenProjectImportResult> importResults = configurationManager.importProjects( projectInfos, configuration,
                                                 subProgress.newChild( 40, SubMonitor.SUPPRESS_NONE ) );

            if ( teamProvider != null )
            {
                log.debug( "Enabling team provider for {} imported projects", importResults.size() );
                teamProvider.afterProjectsImport( importResults, location, monitor );
            }
        }
        log.debug( "Successfully updated source tree {}.", sourceTree.getLocation() );
    }

    private Map<IPath, MavenProjectInfo> getNewProjects( File location, SubMonitor subProgress )
        throws InterruptedException, CoreException
    {
        Map<IPath, MavenProjectInfo> projects = new LinkedHashMap<IPath, MavenProjectInfo>();

        for ( MavenProjectInfo info : collectProjects( location, subProgress ) )
        {
            projects.put( Path.fromOSString( info.getPomFile().getParentFile().getAbsolutePath() ), info );
        }

        return projects;
    }
}
