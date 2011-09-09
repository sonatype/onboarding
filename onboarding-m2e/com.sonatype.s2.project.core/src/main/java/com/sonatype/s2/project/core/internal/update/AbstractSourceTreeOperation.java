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
import java.util.Set;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Model;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.internal.MavenProjectScanner;
import com.sonatype.s2.project.core.internal.S2MavenProjectInfo;
import com.sonatype.s2.project.core.team.ITeamProvider;

public class AbstractSourceTreeOperation
{
    private static final Logger log = LoggerFactory.getLogger( AbstractSourceTreeOperation.class );

    protected final IProjectConfigurationManager configurationManager;

    protected final MavenProjectManager projectManager;

    protected final IMaven maven;

    protected final IWorkspaceSourceTree sourceTree;

    protected AbstractSourceTreeOperation( IWorkspaceSourceTree module )
    {
        this.sourceTree = module;

        MavenPlugin plugin = MavenPlugin.getDefault();
        this.configurationManager = plugin.getProjectConfigurationManager();
        this.maven = plugin.getMaven();
        this.projectManager = plugin.getMavenProjectManager();
    }

    protected List<MavenProjectInfo> collectProjects( File location, SubMonitor subProgress )
        throws InterruptedException, CoreException
    {
        List<MavenProjectInfo> projectInfos = new ArrayList<MavenProjectInfo>();

        List<String> roots = getRoots( sourceTree );

        log.debug( "Scanning for projects in {}/{}", location, roots );
        SubMonitor subSubProgress =
            SubMonitor.convert( subProgress.newChild( 10, SubMonitor.SUPPRESS_NONE ), roots.size() * 100 );
        for ( String rootRelpath : roots )
        {
            File basedir = new File( location, rootRelpath );
            MavenProjectScanner scanner = new MavenProjectScanner( basedir, sourceTree.getProfiles() )
            {
                @Override
                protected MavenProjectInfo newMavenProjectInfo( String label, File pomFile, Model model,
                                                                MavenProjectInfo parent )
                {
                    return new S2MavenProjectInfo( sourceTree, label, pomFile, model, parent );
                }
            };
            scanner.run( subSubProgress.newChild( 100, SubMonitor.SUPPRESS_NONE ) );

            IStatus scanStatus = scanner.getStatus();
            if ( !scanStatus.isOK() )
            {
                log.warn( "Could not scan Maven projects. {}", scanStatus.toString() );
                return projectInfos;
            }

            Set<MavenProjectInfo> moduleProjects = configurationManager.collectProjects( scanner.getProjects() );

            if ( log.isDebugEnabled() )
            {
                for ( MavenProjectInfo projectInfo : moduleProjects )
                {
                    log.debug( "Found project info {}", projectInfo );
                }
            }

            projectInfos.addAll( moduleProjects );
        }

        return projectInfos;
    }

    public static List<String> getRoots( IWorkspaceSourceTree module )
    {
        List<String> roots = module.getRoots();
        if ( roots == null )
        {
            roots = new ArrayList<String>();
        }
        if ( roots.isEmpty() )
        {
            roots.add( "." );
        }
        else if ( roots.indexOf( "." ) > 0 )
        {
            // always import from checkout root first... for no good reason.
            roots.remove( "." );
            roots.add( 0, "." );
        }
        return roots;
    }

    private List<String> getActiveProfiles()
        throws CoreException
    {
        List<String> result = new ArrayList<String>();

        MavenExecutionRequest request = maven.createExecutionRequest( new NullProgressMonitor() );
        result.addAll( request.getActiveProfiles() );
        result.addAll( sourceTree.getProfiles() );

        return result;
    }

    protected ProjectImportConfiguration newProjectImportConfiguration()
        throws CoreException
    {
        ResolverConfiguration resolverConfig = new ResolverConfiguration();

        StringBuilder profiles = new StringBuilder();
        for ( String profile : getActiveProfiles() )
        {
            if ( profiles.length() > 0 )
            {
                profiles.append( ',' );
            }
            profiles.append( profile );
        }
        resolverConfig.setActiveProfiles( profiles.toString() );

        ProjectImportConfiguration importConfig = new ProjectImportConfiguration( resolverConfig );
        return importConfig;
    }

    protected static ITeamProvider getTeamProvider( String scmUrl )
        throws CoreException
    {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint extensionPoint = registry.getExtensionPoint( "com.sonatype.s2.project.core.teamProviders" );
        if ( extensionPoint != null )
        {
            for ( IExtension extension : extensionPoint.getExtensions() )
            {
                for ( IConfigurationElement element : extension.getConfigurationElements() )
                {
                    if ( "provider".equals( element.getName() ) )
                    {
                        String type = "scm:" + element.getAttribute( "type" );
                        if ( scmUrl.startsWith( type ) )
                        {
                            try
                            {
                                return (ITeamProvider) element.createExecutableExtension( "class" );
                            }
                            catch ( CoreException e )
                            {
                                log.debug( "Could not create team provider integration", e );
                            }
                        }
                    }
                }
            }
        }

        return new NullTeamProvider();
        // throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
        // "Team provider integration is not available for " + scmUrl ) );
    }

//    protected IScmLocation getScmLocation()
//        throws CoreException
//    {
//        IScmLocation scmLocation = module.getScmLocation();

//        if ( scmLocation == null || scmLocation.getUrl() == null || scmLocation.getUrl().trim().length() == 0 )
//        {
//            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID, "Module " + module.getName()
//                + " does not have scm url" ) );
//        }
//        return scmLocation;
//    }

    public static Map<IPath, IProject> getWorkspaceProjects( IWorkspaceSourceTree tree )
    {
        IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();

        IPath root = Path.fromOSString( tree.getLocation() );

        Map<IPath, IProject> projects = new LinkedHashMap<IPath, IProject>();

        for ( IProject project : workspace.getProjects() )
        {
            if ( root.isPrefixOf( project.getLocation() ) )
            {
                projects.put( project.getLocation(), project );
            }
        }

        return projects;
    }
    
}
