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

import static com.sonatype.s2.project.core.internal.update.AbstractSourceTreeOperation.getRoots;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.ide.IDEUpdater;
import com.sonatype.s2.project.core.ide.IIDEUpdater;
import com.sonatype.s2.project.core.internal.S2CodebaseRegistry;
import com.sonatype.s2.project.core.internal.WorkspaceCodebase;
import com.sonatype.s2.project.core.internal.WorkspaceSourceTree;
import com.sonatype.s2.project.core.team.ITeamProvider;
import com.sonatype.s2.project.core.team.TeamOperationResult;
import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;

/**
 * @author igor
 */
public class DetermineCodebaseUpdateStatusOperation
{
    private static final Logger log = LoggerFactory.getLogger( DetermineCodebaseUpdateStatusOperation.class );
    private final IWorkspaceCodebase codebase;
    
    public DetermineCodebaseUpdateStatusOperation( IWorkspaceCodebase codebase )
    {
        this.codebase = codebase;
    }
    
    public void run( IProgressMonitor monitor )
        throws CoreException
    {
        if ( codebase.getDescriptorUrl() == null )
        {
            throw new IllegalStateException();
        }

        log.info( "Checking update status of codebase {}", codebase.getArtifactId() );

        SubMonitor progress =
            SubMonitor.convert( monitor, "Checking update status of " + codebase.getArtifactId(), 100 );

        progress.setTaskName( "Loading codebase descriptor" );
		IS2Project newProject = loadCodebaseDescriptor( progress.newChild( 5, SubMonitor.SUPPRESS_NONE ) );

		WorkspaceCodebase newCodebase = S2CodebaseRegistry.createCodebase( newProject );
        progress.setWorkRemaining( 95 );

        ( (WorkspaceCodebase) codebase ).setPending( newCodebase );

        // Deal with p2
        IIDEUpdater updater = IDEUpdater.getUpdater();
        if ( updater != null )
        {
            progress.setTaskName( "Eclipse Installation" );
            ( (WorkspaceCodebase) codebase ).setIsP2LineupUpToDate( updater.isUpToDate( newProject.getP2LineupLocation(),
                                                                                        progress.newChild( 5,
                                                                                                           SubMonitor.SUPPRESS_NONE ) ) );
            progress.setWorkRemaining( 85 );
        }
        else
        {
            ( (WorkspaceCodebase) codebase ).setIsP2LineupUpToDate( IIDEUpdater.UNKNOWN );
        }

        // deal with source trees
        Map<String, IWorkspaceSourceTree> oldTrees = getSourceTreesMap( codebase );
        Map<String, WorkspaceSourceTree> newModules = getCodebaseModules( newProject );

        int workRemaining = 80;
        int workStep = 80 / Math.max( 1, newModules.size() );

        for ( Map.Entry<String, WorkspaceSourceTree> entry : newModules.entrySet() )
        {
            log.debug( "Checking update status of source tree {}", entry.getKey() );
            WorkspaceSourceTree sourceTree = (WorkspaceSourceTree) oldTrees.get( entry.getKey() );

            String status = null;
            if ( sourceTree != null )
            {
                // Existing source tree
                progress.setTaskName( "Source tree " + sourceTree.getName() );

                sourceTree = sourceTree.clone();
                File location = new File( sourceTree.getLocation() );

                if ( !location.exists() )
                {
                    status = IWorkspaceSourceTree.STATUS_ADDED;
                }
                else
                {
                    if ( !equalsIgnoreOrder( sourceTree.getProfiles(), entry.getValue().getProfiles() ) )
                    {
                        log.debug( "Profiles have changed" );
                        status = IWorkspaceSourceTree.STATUS_CHANGED;
                        sourceTree.setProfiles( entry.getValue().getProfiles() );
                    }
                    if ( !equalsIgnoreOrder( sourceTree.getRoots(), getRoots( entry.getValue() ) ) )
                    {
                        log.debug( "Roots have changed" );
                        status = IWorkspaceSourceTree.STATUS_CHANGED;
                        sourceTree.setRoots( entry.getValue().getRoots() );
                    }
                }
                if ( status == null )
                {
                    ITeamProvider teamProvider = AbstractSourceTreeOperation.getTeamProvider( sourceTree.getScmUrl() );
                    if ( teamProvider != null )
                    {
                        TeamOperationResult updateStatus =
                            teamProvider.getUpdateStatus( sourceTree,
                                                          progress.newChild( workStep, SubMonitor.SUPPRESS_NONE ) );
                        log.debug( "Team provider returned status: {}", updateStatus );
                        sourceTree.setStatusMessage( updateStatus.getMessage() );
                        sourceTree.setStatusHelp( updateStatus.getHelp() );
                        status = updateStatus.getStatus();
                    }
                    else
                    {
                        log.warn( "Cannot find team provider for {}", sourceTree.getScmUrl() );
                        sourceTree.setStatusMessage( "Unknown/unsupported team provider" );
                        status = IWorkspaceSourceTree.STATUS_NOT_SUPPORTED;
                    }
                }

                sourceTree.setStatus( status );

                progress.setWorkRemaining( workRemaining -= workStep );
            }
            else
            {
                log.debug( "Found new source tree {}", entry.getKey() );
                sourceTree = entry.getValue();

                File location = new File( sourceTree.getLocation() );

                if ( location.exists() )
                {
                	//TODO mkleint: did I get the message right?
                	sourceTree.setStatusMessage("Source tree root exists");
                	sourceTree.setStatusHelp("A new source tree was added to the codebase, but it appears that the workspace already contains a a root/folder with that name.\n To resolve the problem you should either delete or move the existing content.");
                    status = IWorkspaceSourceTree.STATUS_NOT_SUPPORTED;
                }
                else
                {
                    status = IWorkspaceSourceTree.STATUS_ADDED;
                }
                sourceTree.setStatus( status );
            }
            log.info( "Update status for source tree {}: {}", sourceTree.getName(), status );

            newCodebase.addSourceTree( sourceTree );
        }

        for ( Map.Entry<String, IWorkspaceSourceTree> entry : oldTrees.entrySet() )
        {
            if ( !newModules.containsKey( entry.getKey() ) )
            {
                WorkspaceSourceTree sourceTree = ( (WorkspaceSourceTree) entry.getValue() ).clone();
                String status;
                if ( new File( sourceTree.getLocation() ).exists() )
                {
                	//TODO mkleint: did I get the message right?
                	sourceTree.setStatusMessage("Source tree root to be removed manually");
                	sourceTree.setStatusHelp("A source tree was removed from the codebase. Automated source code removal is not supported.\nTo resolve the problem, check your scm sources for local changes and delete them afterwards.");
                    status = IWorkspaceSourceTree.STATUS_NOT_SUPPORTED;
                }
                else
                {
                    status = IWorkspaceSourceTree.STATUS_REMOVED;
                }
                sourceTree.setStatus( status );
                newCodebase.addSourceTree( sourceTree );
            }
        }
    }

    private static <T> boolean equalsIgnoreOrder( Collection<T> a, Collection<T> b )
    {
        return a.containsAll( b ) && b.containsAll( a );
    }

    private static Map<String, WorkspaceSourceTree> getCodebaseModules( IS2Project newProject )
    {
        Map<String, WorkspaceSourceTree> modules = new LinkedHashMap<String, WorkspaceSourceTree>();

        for ( IS2Module module : newProject.getModules() )
        {
            WorkspaceSourceTree sourceTree = S2CodebaseRegistry.createSourceTree( module, getModuleLocation( module ) );
            modules.put( getSourceTreeKey( sourceTree.getName(), sourceTree.getScmUrl(), sourceTree.getScmBranch() ),
                         sourceTree );
        }

        return modules;
    }

    private static File getModuleLocation( IS2Module module )
    {
        return SourceTreeImportOperation.getModuleLocation( module );
    }

    private IS2Project loadCodebaseDescriptor( IProgressMonitor monitor )
        throws CoreException
    {
        return S2ProjectCore.getInstance().loadProject( codebase.getDescriptorUrl(), monitor );
    }

    private static Map<String, IWorkspaceSourceTree> getSourceTreesMap( IWorkspaceCodebase codebase )
    {
        Map<String, IWorkspaceSourceTree> trees = new LinkedHashMap<String, IWorkspaceSourceTree>();

        for ( IWorkspaceSourceTree tree : codebase.getSourceTrees() )
        {
            // TODO to support manual switch/checkout/etc, scm location should be determined by looking at the actual
            // source tree
            trees.put( getSourceTreeKey( tree.getName(), tree.getScmUrl(), tree.getScmBranch() ), tree );
        }

        return trees;
    }

    private static String getSourceTreeKey( String name, String scmUrl, String scmBranch )
    {
        StringBuilder key = new StringBuilder();
        key.append( name ).append( '/' ).append( scmUrl );
        if ( scmBranch != null )
        {
            key.append( '@' ).append( scmBranch );
        }
        return key.toString();
    }
}
