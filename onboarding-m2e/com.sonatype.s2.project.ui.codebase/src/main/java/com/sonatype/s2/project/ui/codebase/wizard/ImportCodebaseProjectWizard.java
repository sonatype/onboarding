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
package com.sonatype.s2.project.ui.codebase.wizard;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.maven.ide.eclipse.io.S2IOFacade;

import com.sonatype.nexus.onboarding.rest.dto.CatalogEntryDTO;
import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.project.model.IP2LineupLocation;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.S2ProjectFacade;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.codebase.NewCodebaseProjectOperation;
import com.sonatype.s2.project.ui.internal.wizards.ImportProjectInfoWizardPage;
import com.sonatype.s2.project.ui.lineup.NewLineupProjectOperation;
import com.sonatype.s2.project.ui.lineup.wizard.AbstractLineupProjectImportWizard;

public class ImportCodebaseProjectWizard
    extends AbstractLineupProjectImportWizard
{
    private SelectCodebasePage codebaseSelectionPage;

    private ImportProjectInfoWizardPage projectInfoPage;

    private ImportProjectInfoWizardPage lineupInfoPage;

    private String nexusBaseUrl;

    private String codebaseBaseUrl;

    private CatalogEntryDTO selectedEntry;

    private IS2Project codebase;

    private IP2Lineup lineup;

    public void init( IWorkbench workbench, IStructuredSelection selection )
    {
        setWindowTitle( Messages.importCodebaseProjectWizard_downloadJobtitle );
        setNeedsProgressMonitor( true );
    }

    @Override
    public boolean performFinish()
    {
        final String projectName = projectInfoPage.getProjectName();
        final String lineupProjectName = lineupInfoPage.getProjectName();
        try
        {
            getContainer().run( true, true, new IRunnableWithProgress()
            {
                public void run( IProgressMonitor monitor )
                    throws InvocationTargetException, InterruptedException
                {
                    monitor.setTaskName( NLS.bind( Messages.importCodebaseProjectWizard_downloadJobimportJob,
                                                   selectedEntry.getName(), codebaseBaseUrl ) );

                    try
                    {
                        new NewCodebaseProjectOperation( projectName, codebase, codebaseBaseUrl ).createProject( monitor );

                        if ( lineup != null )
                        {
                            new NewLineupProjectOperation( lineupProjectName, lineup ).createProject( monitor );
                        }
                    }
                    catch ( CoreException e )
                    {
                        throw new InvocationTargetException( e );
                    }
                }
            } );
        }
        catch ( InterruptedException e )
        {
            return false;
        }
        catch ( InvocationTargetException e )
        {
            handleException( e );
            return false;
        }

        return true;
    }

    @Override
    public void addPages()
    {
        codebaseSelectionPage = new SelectCodebasePage()
        {
            protected void saveSelection( Version version )
            {
                super.saveSelection( version );
                CatalogEntryDTO entry = version.getEntry();
                if ( entry != selectedEntry )
                {
                    selectedEntry = entry;
                    nexusBaseUrl = getServerUrl();
                    if ( nexusBaseUrl.endsWith( "/" ) )
                    {
                        nexusBaseUrl = nexusBaseUrl.substring( 0, nexusBaseUrl.length() - 1 );
                    }
                    codebaseBaseUrl =
                        nexusBaseUrl + IS2Project.PROJECT_REPOSITORY_PATH + selectedEntry.getUrl().substring( 1 ) + "/"; //$NON-NLS-1$
                    codebase = null;
                    lineup = null;

                    projectInfoPage.setCoordinates( "", "", "" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    lineupInfoPage.setCoordinates( "", "", "" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    getContainer().updateButtons();
                }
            };

            @Override
            public boolean canFlipToNextPage()
            {
                // prevent repeated calls to getNextPage()
                return isPageComplete();
            }

            @Override
            public IWizardPage getNextPage()
            {
                if ( codebase != null || downloadEverything() )
                {
                    return super.getNextPage();
                }
                else
                {
                    return null;
                }
            }
        };
        addPage( codebaseSelectionPage );

        projectInfoPage = new ImportProjectInfoWizardPage();
        projectInfoPage.setCoordinatesTitle( Messages.importProjectInfoWizardPage_codebase_title );
        projectInfoPage.setDescription( Messages.importProjectInfoWizardPage_codebase_description );
        projectInfoPage.setTemplatePrefix( "codebase" ); //$NON-NLS-1$
        projectInfoPage.setTemplateTitle( Messages.importProjectInfoWizardPage_codebase_project );
        projectInfoPage.setTitle( Messages.importProjectInfoWizardPage_codebase_title );
        addPage( projectInfoPage );

        lineupInfoPage = addLineupInfoPage();
    }

    private boolean downloadEverything()
    {
        try
        {
            getContainer().run( true, true, new IRunnableWithProgress()
            {
                public void run( IProgressMonitor monitor )
                    throws InvocationTargetException, InterruptedException
                {
                    monitor.setTaskName( Messages.importCodebaseProjectWizard_downloadJob10 );

                    codebase = loadCodebase( codebaseBaseUrl, monitor );
                    IP2LineupLocation lineupLocation = codebase.getP2LineupLocation();
                    if ( lineupLocation != null )
                    {
                        lineup = loadLineup( lineupLocation.getUrl(), nexusBaseUrl, monitor );
                    }
                }
            } );

            projectInfoPage.setCoordinates( codebase.getGroupId(), codebase.getArtifactId(), codebase.getVersion() );

            if ( lineup != null )
            {
                lineupInfoPage.setCoordinates( lineup.getGroupId(), lineup.getId(), lineup.getVersion() );
            }
            else
            {
                lineupInfoPage.setPageComplete( true );
            }
            getContainer().updateButtons();
        }
        catch ( InvocationTargetException e )
        {
            handleException( e );
            return false;
        }
        catch ( InterruptedException e )
        {
            return false;
        }

        return true;
    }

    private IS2Project loadCodebase( String codebaseBaseUrl, IProgressMonitor monitor )
        throws InvocationTargetException
    {
        String uri = codebaseBaseUrl + IS2Project.PROJECT_DESCRIPTOR_FILENAME;

        IS2Project project;
        try
        {
            InputStream is = S2IOFacade.openStream( uri, monitor );
            try
            {
                project = S2ProjectFacade.loadProject( is, true );
            }
            finally
            {
                IOUtil.close( is );
            }

            return project;
        }
        catch ( IOException e )
        {
            throw new InvocationTargetException( e );
        }
        catch ( URISyntaxException e )
        {
            throw new InvocationTargetException( e );
        }
    }

    @Override
    public IWizardPage getNextPage( IWizardPage page )
    {
        IWizardPage nextPage = super.getNextPage( page );
        if ( lineup == null && nextPage == lineupInfoPage )
        {
            // skip lineup page if there is no lineup
            return super.getNextPage( nextPage );
        }
        return nextPage;
    }
}
