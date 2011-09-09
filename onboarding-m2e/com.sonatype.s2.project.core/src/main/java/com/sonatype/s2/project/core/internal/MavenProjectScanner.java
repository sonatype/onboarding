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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.AbstractProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project scanner that discovers maven parent/module structure on local filesystem.
 */
public class MavenProjectScanner
    extends AbstractProjectScanner<MavenProjectInfo>
{
    private static final Logger log = LoggerFactory.getLogger( MavenProjectScanner.class );

    private final File basedir;

    private IMaven maven = MavenPlugin.getDefault().getMaven();

    private final List<String> profiles;

    public MavenProjectScanner( File basedir )
    {
        this( basedir, null );
    }

    public MavenProjectScanner( File basedir, List<String> profiles )
    {
        this.basedir = basedir;
        this.profiles = profiles;
    }

    @Override
    public String getDescription()
    {
        return basedir.getAbsolutePath();
    }

    @Override
    public void run( IProgressMonitor monitor )
        throws InterruptedException
    {
        try
        {
            processProject( basedir, null/* parentInfo */, new HashSet<File>(), monitor );
        }
        catch ( Exception e )
        {
            addError( e );
        }
    }

    private void processProject( File basedir, MavenProjectInfo parentInfo, Set<File> scanned, IProgressMonitor monitor )
        throws CoreException, IOException
    {
        if ( !basedir.exists() || !basedir.isDirectory() )
        {
            log.debug( "Skipping non-existent base directory {} from scanning", basedir );
            return;
        }

        basedir = basedir.getCanonicalFile();

        if ( !scanned.add( basedir ) )
        {
            log.debug( "Excluded already scanned base directory {} from re-scanning", basedir );
            return;
        }

        SubMonitor progress = SubMonitor.convert( monitor, "Analyzing project directory " + basedir, 12 );

        MavenExecutionRequest request = maven.createExecutionRequest( progress.newChild( 1 ) );
        File pomFile = new File( basedir, IMavenConstants.POM_FILE_NAME );
        request.setPom( pomFile );
        if ( profiles != null )
        {
            request.getActiveProfiles().addAll( profiles );
        }

        log.debug( "Reading POM {} with profiles {}", pomFile, profiles );

        MavenExecutionResult result = maven.readProject( request, progress.newChild( 1 ) );

        if ( result.hasExceptions() )
        {
            for ( Throwable e : result.getExceptions() )
            {
                addError( e );
            }
        }

        MavenProject project = result.getProject();

        if ( project != null )
        {
            MavenProjectInfo projectInfo =
                newMavenProjectInfo( getProjectLabel( project ), pomFile, project.getModel(), parentInfo );

            addProject( projectInfo, parentInfo );

            // TODO detached parent

            SubMonitor subProgress =
                SubMonitor.convert( progress.newChild( 10, SubMonitor.SUPPRESS_NONE ), project.getModules().size() );

            for ( String module : project.getModules() )
            {
                File moduleDir = new File( basedir, module );

                processProject( moduleDir, projectInfo, scanned, subProgress.newChild( 1 ) );
            }
        }
        else
        {
            log.debug( "Failed to read {}, creating stub project", pomFile );

            // will create project even if no pom or bad pom
            MavenProjectInfo projectInfo = newMavenProjectInfo( pomFile.getAbsolutePath(), pomFile, null, parentInfo );

            addProject( projectInfo, parentInfo );
        }

        progress.done();
    }

    private void addProject( MavenProjectInfo projectInfo, MavenProjectInfo parentInfo )
    {
        if ( parentInfo != null )
        {
            log.debug( "Adding project info {} to parent {}", projectInfo, parentInfo );

            parentInfo.add( projectInfo );
        }
        else
        {
            log.debug( "Adding project info {}", projectInfo );

            addProject( projectInfo );
        }
    }

    protected MavenProjectInfo newMavenProjectInfo( String projectLabel, File pomFile, Model model,
                                                    MavenProjectInfo parentInfo )
    {
        return new MavenProjectInfo( projectLabel, pomFile, model, parentInfo );
    }

    private String getProjectLabel( MavenProject project )
    {
        return project.getName();
    }

    public IStatus getStatus()
    {
        List<Throwable> errors = getErrors();
        if ( errors.size() == 0 )
        {
            return Status.OK_STATUS;
        }

        List<IStatus> statuses = new ArrayList<IStatus>( errors.size() );
        for ( Throwable t : errors )
        {
            if ( t instanceof CoreException )
            {
                statuses.add( ( (CoreException) t ).getStatus() );
            }
            else
            {
                statuses.add( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID, t.getMessage(), t ) );
            }
        }
        return new MultiStatus( S2ProjectPlugin.PLUGIN_ID, 0, statuses.toArray( new IStatus[] {} ),
                                "Failed to scan projects", null );
    }
}
