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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.core.internal.project.MavenProjectImportResult;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.scm.MavenCheckoutOperation;
import org.eclipse.m2e.scm.MavenProjectScmInfo;
import org.eclipse.m2e.scm.internal.ScmHandlerFactory;
import org.eclipse.m2e.scm.spi.ScmHandler;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.internal.S2ProjectPlugin;
import com.sonatype.s2.project.core.internal.WorkspaceSourceTree;
import com.sonatype.s2.project.core.team.ITeamProvider;
import com.sonatype.s2.project.model.IS2Module;

@SuppressWarnings( "restriction" )
public class SourceTreeImportOperation
    extends AbstractSourceTreeOperation
    implements IUpdateOperation
{
    private static final Logger log = LoggerFactory.getLogger( SourceTreeImportOperation.class );
    private File location;

    public SourceTreeImportOperation( IWorkspaceSourceTree sourceTree )
    {
        super( sourceTree );
    }

    public void run( IProgressMonitor monitor )
        throws CoreException, InterruptedException
    {
        SubMonitor subProgress = SubMonitor.convert( monitor );

        ProjectImportConfiguration configuration = newProjectImportConfiguration();

        log.debug( "Processing module {}", sourceTree.getName() );
        this.location = new File( sourceTree.getLocation() );

        if ( location.exists() )
        {
            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                 "Cannot create S2 module project. File already exists: "
                                                     + location.getAbsolutePath() ) );
        }

        ITeamProvider teamProvider = getTeamProvider( sourceTree.getScmUrl() );

        checkoutSourceTree( location, subProgress );

//        location = renameCheckoutLocation( configuration, location );

        List<MavenProjectInfo> projectInfos = collectProjects( location, subProgress );

        List<IMavenProjectImportResult> projectImportResults;
        if ( !projectInfos.isEmpty() )
        {
            log.debug( "Importing {} materialized projects", projectInfos.size() );
            projectImportResults = configurationManager.importProjects( projectInfos, configuration,
                                                 subProgress.newChild( 40, SubMonitor.SUPPRESS_NONE ) );

        }
        else
        {
            IProject project = createSimpleProject( subProgress.newChild( 40, SubMonitor.SUPPRESS_NONE ) );
            projectImportResults = new ArrayList<IMavenProjectImportResult>();
            projectImportResults.add( new MavenProjectImportResult( null, project ) );
        }

        teamProvider.afterProjectsImport( projectImportResults, location, monitor );

        ( (WorkspaceSourceTree) sourceTree ).setLocation( location.getAbsolutePath() );
        ( (WorkspaceSourceTree) sourceTree ).setStatus( IWorkspaceSourceTree.STATUS_UPTODATE );
    }

    private IProject createSimpleProject( SubMonitor monitor )
        throws CoreException
    {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot workspaceRoot = workspace.getRoot();

        IProject project = workspaceRoot.getProject( sourceTree.getName() );

        if ( location.getParentFile().equals( workspaceRoot.getLocation().toFile() ) )
        {
            project.create( monitor );
        }
        else
        {
            IProjectDescription description = workspace.newProjectDescription( project.getName() );
            description.setLocation( new Path( location.getAbsolutePath() ) );
            project.create( description, monitor );
        }

        if ( !project.isOpen() )
        {
            project.open( monitor );
        }

        return project;
    }

//    private File renameCheckoutLocation( ProjectImportConfiguration configuration, File location )
//    {
//        if ( getRoots( sourceTree ).indexOf( "." ) >= 0 )
//        {
//            File pom = new File( location, IMavenConstants.POM_FILE_NAME );
//
//            try
//            {
//                Model model = maven.readModel( pom );
//
//                String projectName = configuration.getProjectName( model );
//                if ( !projectName.equals( location.getName() ) )
//                {
//                    File projectDir = new File( location.getParentFile(), projectName ).getAbsoluteFile();
//
//                    if ( !projectDir.exists() && location.renameTo( projectDir ) )
//                    {
//                        return projectDir;
//                    }
//                }
//            }
//            catch ( CoreException e )
//            {
//                log.debug( "Could not read pom.xml file " + pom.getAbsolutePath(), e );
//            }
//        }
//
//        return location;
//    }

    protected void checkoutSourceTree( File location, SubMonitor subProgress )
        throws CoreException, InterruptedException
    {
        String scmUrl = sourceTree.getScmUrl();
        String scmBranch = sourceTree.getScmBranch();

        ScmHandler scmHandler = ScmHandlerFactory.getHandler( scmUrl );
        if ( scmHandler == null )
        {
            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                 "SCM provider is not available for " + scmUrl ) );
        }

        MavenProjectScmInfo scmInfo =
            new MavenProjectScmInfo( sourceTree.getName(), null, null, scmBranch, "HEAD", scmUrl, scmUrl );
        setScmAuthData( scmInfo, scmUrl );
        MavenCheckoutOperation checkout = new MavenCheckoutOperation( location, Collections.singletonList( scmInfo ) )
        {
            protected File getUniqueDir( File baseDir )
            {
                return baseDir;
            }
        };
        log.debug( "Checking out module {} from {}", sourceTree.getName(), scmUrl );
        long checkoutStart = System.currentTimeMillis();
        try
        {
            checkout.run( subProgress.newChild( 90, SubMonitor.SUPPRESS_NONE ) );
        }
        catch ( CoreException e )
        {
            try
            {
                FileUtils.deleteDirectory( location );
            }
            catch ( IOException io )
            {
                log.warn( "Failed to delete target directory " + location + " of failed checkout: " + io.getMessage(),
                          io );
            }
            throw e;
        }
        log.debug( "Checked out module {} in {} ms", sourceTree.getName(), ""
            + ( System.currentTimeMillis() - checkoutStart ) );
    }

    public static File getModuleLocation( IS2Module module )
    {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        File location = new File( workspaceRoot.getLocation().toFile(), module.getName() ).getAbsoluteFile();
        return location;
    }

    private void setScmAuthData( MavenProjectScmInfo scmInfo, String uri )
        throws CoreException
    {
        IAuthData authData = AuthFacade.getAuthService().select( uri );
        if ( authData != null && !authData.isAnonymousAccessRequired() )
        {
            if ( authData.allowsUsernameAndPassword() )
            {
                scmInfo.setUsername( authData.getUsername() );
                scmInfo.setPassword( authData.getPassword() );
            }

            if ( authData.allowsCertificate() )
            {
                scmInfo.setSSLCertificate( authData.getCertificatePath() );
                scmInfo.setSSLCertificatePassphrase( authData.getCertificatePassphrase() );
            }
        }
    }

    public File getLocation()
    {
        return location;
    }
}
