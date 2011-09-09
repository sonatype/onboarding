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
package com.sonatype.s2.project.core.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Model;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.internal.update.SourceTreeUpdateOperation;
import com.sonatype.s2.utils.GenericBackgroundProcessingQueue;

public class ModulesAddRemoveJob
    extends GenericBackgroundProcessingQueue<ModulesAddRemoveJob.Request>
{

    protected static final Logger log = LoggerFactory.getLogger( ModulesAddRemoveJob.class );

    public ModulesAddRemoveJob()
    {
        super( "Update Maven Modules" );
    }

    public static class Request
    {
        final Set<IPath> oldModules = new HashSet<IPath>();

        final Set<IPath> newModules = new HashSet<IPath>();

        public void addOldModules( List<IPath> modules )
        {
            oldModules.addAll( modules );
        }

        public void addNewModules( List<IPath> modules )
        {
            newModules.addAll( modules );
        }
    }

    @Override
    protected void process( final Request request, IProgressMonitor monitor )
        throws CoreException
    {
        final IStatus[] problem = new IStatus[1];

        ResourcesPlugin.getWorkspace().run( new IWorkspaceRunnable()
        {
            public void run( IProgressMonitor monitor )
                throws CoreException
            {
                problem[0] = processInWorkspace( request, monitor );
            }
        }, monitor );

        if ( problem[0] != null )
        {
            throw new CoreException( problem[0] );
        }
    }

    protected IStatus processInWorkspace( Request request, IProgressMonitor monitor )
    {
        MavenPlugin mavenPlugin = MavenPlugin.getDefault();
        IProjectConfigurationManager configurationManager = mavenPlugin.getProjectConfigurationManager();
        IMaven maven = mavenPlugin.getMaven();

        S2CodebaseRegistry registry = S2ProjectCore.getInstance().getCodebaseRegistry();

        Map<String, IWorkspaceSourceTree> sourceTrees = new HashMap<String, IWorkspaceSourceTree>();

        Map<IPath, IProject> projects = getWorkspaceProjects();

        ArrayList<IStatus> problems = new ArrayList<IStatus>();

        // remove removed module projects
        for ( IPath location : request.oldModules )
        {
            if ( monitor.isCanceled() )
            {
                return Status.CANCEL_STATUS;
            }

            if ( request.newModules.contains( location ) )
            {
                // nothing's changed
                continue;
            }

            IProject project = projects.get( location );
            if ( project == null )
            {
                log.debug( "Project at {} has been removed already", location );
                continue;
            }

            IWorkspaceSourceTree sourceTree = registry.getSourceTree( location );
            if ( sourceTree != null )
            {
                log.debug( "Project at location {} will be processed as part of source tree {}", location,
                           sourceTree.getLocation() );
                sourceTrees.put( sourceTree.getLocation(), sourceTree );
            }
            else
            {
                if ( !new File( location.toFile(), "pom.xml" ).exists() )
                {
                    try
                    {
                        log.debug( "Deleting removed module project {} at {}", project, project.getLocation() );
                        project.delete( false, true, monitor );
                    }
                    catch ( CoreException e )
                    {
                        log.debug( "Could not delete removed module project {} at {}",
                                   new Object[] { project, project.getLocation(), e } );
                        problems.add( e.getStatus() );
                    }
                }
            }
        }

        // add added module projects

        ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration();

        List<MavenProjectInfo> infos = new ArrayList<MavenProjectInfo>();

        for ( IPath location : request.newModules )
        {
            if ( monitor.isCanceled() )
            {
                return Status.CANCEL_STATUS;
            }

            if ( request.oldModules.contains( location ) )
            {
                continue;
            }

            IProject project = projects.get( location );
            if ( project != null )
            {
                log.debug( "Project at location {} has been imported into workspace already", location );
                continue;
            }

            IWorkspaceSourceTree sourceTree = registry.getSourceTree( location );
            if ( sourceTree != null )
            {
                log.debug( "Project at location {} will be processed as part of source tree {}", location,
                           sourceTree.getLocation() );
                sourceTrees.put( sourceTree.getLocation(), sourceTree );
            }
            else
            {
                try
                {
                    File pomFile = location.append( "pom.xml" ).toFile();
                    Model model = maven.readModel( pomFile );
                    String projectName = importConfiguration.getProjectName( model );

                    MavenProjectInfo info = new MavenProjectInfo( projectName, pomFile, model, null );
                    infos.add( info );
                    log.debug( "Project at location {} added to to-import list", location );
                }
                catch ( CoreException e )
                {
                    // this should be result in error marker on the aggerator created by m2eclipse
                    log.debug( "Could not read module pom.xml", e );
                    // problems.add( e.getStatus() );
                }
            }
        }

        if ( !infos.isEmpty() )
        {
            log.debug( "Importing {} new projects", infos.size() );
            try
            {
                configurationManager.importProjects( infos, importConfiguration, monitor );
            }
            catch ( CoreException e )
            {
                problems.add( e.getStatus() );
            }
        }

        for ( IWorkspaceSourceTree sourceTree : sourceTrees.values() )
        {
            if ( monitor.isCanceled() )
            {
                return Status.CANCEL_STATUS;
            }

            SourceTreeUpdateOperation op = new SourceTreeUpdateOperation( sourceTree, false );
            try
            {
                op.run( monitor );
            }
            catch ( InterruptedException e )
            {
                return Status.CANCEL_STATUS;
            }
            catch ( CoreException e )
            {
                problems.add( e.getStatus() );
            }
        }

        if ( problems.isEmpty() )
        {
            return Status.OK_STATUS;
        }

        return new MultiStatus( S2ProjectPlugin.PLUGIN_ID, IStatus.ERROR,
                                problems.toArray( new IStatus[problems.size()] ), null, null );
    }

    public static Map<IPath, IProject> getWorkspaceProjects()
    {
        IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();

        Map<IPath, IProject> projects = new LinkedHashMap<IPath, IProject>();

        for ( IProject project : workspace.getProjects() )
        {
            projects.put( project.getLocation(), project );
        }

        return projects;
    }

}
