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
package com.sonatype.s2.project.ui.lineup.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;

import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.project.ui.internal.wizards.ImportProjectInfoWizardPage;
import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.project.ui.lineup.NewLineupProjectOperation;

public class ImportLineupProjectWizard
    extends AbstractLineupProjectImportWizard
{
    private SelectLineupPage lineupPage;

    private ImportProjectInfoWizardPage lineupInfoPage;

    public void init( IWorkbench workbench, IStructuredSelection selection )
    {
        setWindowTitle( Messages.importLineupProjectWizard_title );
        setNeedsProgressMonitor( true );
    }

    @Override
    public boolean performFinish()
    {
        final String lineupUrl = lineupPage.getLineupUrl();
        final String lineupProjectName = lineupInfoPage.getProjectName();
        try
        {
            getContainer().run( true, true, new IRunnableWithProgress()
            {
                public void run( IProgressMonitor monitor )
                    throws InvocationTargetException, InterruptedException
                {
                    monitor.setTaskName( NLS.bind( Messages.importLineupProjectWizard_job, lineupProjectName, lineupUrl ) );

                    IP2Lineup lineup = loadLineup( lineupUrl, lineupPage.getServerUrl(), monitor );
                    try
                    {
                        new NewLineupProjectOperation( lineupProjectName, lineup ).createProject( monitor );
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
        lineupPage = new SelectLineupPage( null, null )
        {
            @Override
            protected void saveSelection( Version lineup )
            {
                super.saveSelection( lineup );
                lineupInfoPage.setCoordinates( lineup.getGroupId(), lineup.getArtifactId(), lineup.getVersion() );
                getContainer().updateButtons();
            }
        };
        addPage( lineupPage );

        lineupInfoPage = addLineupInfoPage();
    }
}
