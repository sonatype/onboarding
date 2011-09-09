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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.io.S2IOFacade;

import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.IP2LineupSourceRepository;
import com.sonatype.s2.p2lineup.model.P2LineupHelper;
import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Reader;
import com.sonatype.s2.project.ui.internal.wizards.ImportProjectInfoWizardPage;
import com.sonatype.s2.project.ui.lineup.Activator;
import com.sonatype.s2.project.ui.lineup.Messages;

public abstract class AbstractLineupProjectImportWizard
    extends Wizard
    implements IImportWizard
{
    protected IP2Lineup loadLineup( String lineupUrl, String serverUrl, IProgressMonitor monitor )
        throws InvocationTargetException
    {
        if ( lineupUrl == null || lineupUrl.trim().length() <= 0 )
        {
            return null;
        }

        IP2Lineup lineup;
        try
        {
            StringBuilder url = new StringBuilder( lineupUrl );
            if ( !lineupUrl.endsWith( "/" ) )
            {
                url.append( '/' );
            }
            url.append( IP2Lineup.LINEUP_FILENAME );

            InputStream is = S2IOFacade.openStream( url.toString(), monitor );
            try
            {
                lineup = new P2LineupXpp3Reader().read( is, false /* strict */);
            }
            finally
            {
                IOUtil.close( is );
            }

            P2LineupHelper.replaceNexusServerURLInLineupRepositories( lineup,
                                                                      IP2LineupSourceRepository.NEXUS_BASE_URL,
                                                                      serverUrl );
            return lineup;
        }
        catch ( XmlPullParserException e )
        {
            // TODO I think we need to extract root cause IOException in many cases
            throw new InvocationTargetException( e );
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

    protected void handleException( InvocationTargetException e )
    {
        IStatus status;
        Throwable cause = e.getCause();
        if ( cause instanceof CoreException )
        {
            status = ( (CoreException) cause ).getStatus();
        }
        else
        {
            status = new Status( IStatus.ERROR, Activator.PLUGIN_ID, cause.getMessage(), cause );
        }
        StatusManager.getManager().handle( status, StatusManager.BLOCK | StatusManager.LOG );
    }

    protected ImportProjectInfoWizardPage addLineupInfoPage()
    {
        ImportProjectInfoWizardPage projectInfoPage = new ImportProjectInfoWizardPage();
        projectInfoPage.setCoordinatesTitle( Messages.importProjectInfoWizardPage_lineup_title );
        projectInfoPage.setDescription( Messages.importProjectInfoWizardPage_lineup_description );
        projectInfoPage.setTemplatePrefix( "lineup" );
        projectInfoPage.setTemplateTitle( Messages.importProjectInfoWizardPage_lineup_project );
        projectInfoPage.setTitle( Messages.importProjectInfoWizardPage_lineup_title );
        addPage( projectInfoPage );
        return projectInfoPage;
    }
}
