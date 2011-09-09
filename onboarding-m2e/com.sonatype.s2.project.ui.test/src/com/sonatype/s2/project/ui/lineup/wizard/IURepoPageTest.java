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

import java.net.HttpURLConnection;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.junit.Test;
import org.maven.ide.eclipse.ui.tests.common.AbstractWizardPageTest;

import com.sonatype.nexus.p2.rest.model.P2LineupErrorResponse;
import com.sonatype.nexus.p2.rest.model.P2LineupRepositoryError;
import com.sonatype.nexus.p2.rest.model.P2LineupUnresolvedInstallableUnit;
import com.sonatype.s2.p2lineup.model.IP2LineupSourceRepository;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupInstallableUnit;
import com.sonatype.s2.p2lineup.model.P2LineupSourceRepository;
import com.sonatype.s2.p2lineup.model.io.xstream.P2LineupXstreamIO;
import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.ui.internal.Images;
import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.project.ui.lineup.composites.RepositoryComposite;
import com.sonatype.s2.project.ui.lineup.composites.RootIUComposite;
import com.sonatype.s2.publisher.nexus.NexusLineupPublisher;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;

public class IURepoPageTest
    extends AbstractWizardPageTest
{
    private WizardDialog dialog;

    private static class DummyWizard
        extends Wizard
    {
        @Override
        public boolean performFinish()
        {
            return false;
        }
    }

    private HttpServer httpServer;

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            if ( httpServer != null )
            {
                httpServer.stop();
            }
        }
        finally
        {
            super.tearDown();
        }
    }

    @Test
    public void testEmptyLineup()
        throws Exception
    {
        IURepoPage page = createTestPage( createLineup() );
        assertPageMessage( page, Messages.repositoryComposite_repositoryRequired, IMessageProvider.ERROR );
    }

    @Test
    public void testValidLineup()
        throws Exception
    {
        P2Lineup lineup = createLineup();
        P2LineupSourceRepository p2LineupSourceRepository = new P2LineupSourceRepository();
        p2LineupSourceRepository.setUrl( "http://foo" );
        lineup.addRepository( p2LineupSourceRepository );
        P2LineupInstallableUnit p2LineupInstallableUnit = new P2LineupInstallableUnit();
        p2LineupInstallableUnit.setId( "foo.IU" );
        lineup.addRootInstallableUnit( p2LineupInstallableUnit );
        IURepoPage page = createTestPage( lineup );
        assertPageMessage( page, null, IMessageProvider.NONE );
    }

    @Test
    public void testNoIUs()
        throws Exception
    {
        P2Lineup lineup = createLineup();
        P2LineupSourceRepository p2LineupSourceRepository = new P2LineupSourceRepository();
        p2LineupSourceRepository.setUrl( "http://foo" );
        lineup.addRepository( p2LineupSourceRepository );
        IURepoPage page = createTestPage( lineup );
        assertPageMessage( page, Messages.rootIUComposite_iuRequired, IMessageProvider.ERROR );
    }

    @Test
    public void testNoRepositories()
        throws Exception
    {
        P2Lineup lineup = createLineup();
        P2LineupInstallableUnit p2LineupInstallableUnit = new P2LineupInstallableUnit();
        p2LineupInstallableUnit.setId( "foo.IU" );
        lineup.addRootInstallableUnit( p2LineupInstallableUnit );
        IURepoPage page = createTestPage( lineup );
        assertPageMessage( page, Messages.repositoryComposite_repositoryRequired, IMessageProvider.ERROR );
    }

    @Test
    public void testRepositoryError()
        throws Exception
    {
        httpServer = new HttpServer();
        httpServer.start();

        String nexusUrl = httpServer.getHttpUrl();
        String sourceRepoUrl = "/foo";
        String errorMessage = "Bad repo";
        P2LineupRepositoryError p2LineupError = new P2LineupRepositoryError();
        p2LineupError.setRepositoryURL( IP2LineupSourceRepository.NEXUS_BASE_URL + sourceRepoUrl );
        p2LineupError.setErrorMessage( errorMessage );
        P2LineupErrorResponse errorResponse = new P2LineupErrorResponse();
        errorResponse.addError( p2LineupError );

        httpServer.addResourceErrorResponse( NexusLineupPublisher.P2_RESOURCE_URI, HttpURLConnection.HTTP_BAD_REQUEST,
                                             new P2LineupXstreamIO().writeErrorResponse( errorResponse ) );
        httpServer.setHttpPort( httpServer.getHttpPort() );
        httpServer.stop();
        httpServer.start();

        P2Lineup lineup = createLineup();
        P2LineupSourceRepository p2LineupSourceRepository = new P2LineupSourceRepository();
        p2LineupSourceRepository.setUrl( nexusUrl + sourceRepoUrl );
        lineup.addRepository( p2LineupSourceRepository );
        P2LineupInstallableUnit p2LineupInstallableUnit = new P2LineupInstallableUnit();
        p2LineupInstallableUnit.setId( "foo.IU" );
        lineup.addRootInstallableUnit( p2LineupInstallableUnit );

        IURepoPage page = createTestPage( lineup, nexusUrl );
        assertPageMessage( page, null, IMessageProvider.NONE );
        callValidate( page );
        assertPageMessage( page, Messages.remoteValidator_proxyWarning, IMessageProvider.ERROR );

        Table repoTable = (Table) getControlByName( page, RepositoryComposite.REPOSITORY_CONTROL );
        TableItem repoTableItem = repoTable.getItem( 0 );
        assertEquals( Images.ERROR_REPOSITORY, repoTableItem.getImage() );
        RepositoryComposite repoComposite =
            (RepositoryComposite) getControlByName( page, IURepoPage.REPOSITORY_COMPOSITE );
        TableViewer repoViewer = repoComposite.getViewer();
        repoViewer.setSelection( new StructuredSelection( repoTableItem.getData() ) );
        assertPageMessage( page, errorMessage, IMessageProvider.ERROR );
    }

    @Test
    public void testRepositoryWarning()
        throws Exception
    {
        httpServer = new HttpServer();
        httpServer.start();

        String nexusUrl = httpServer.getHttpUrl();
        String sourceRepoUrl = "/foo";
        String errorMessage = "Bad repo";
        P2LineupRepositoryError p2LineupError = new P2LineupRepositoryError();
        p2LineupError.setRepositoryURL( IP2LineupSourceRepository.NEXUS_BASE_URL + sourceRepoUrl );
        p2LineupError.setErrorMessage( errorMessage );
        p2LineupError.setWarning( true );
        P2LineupErrorResponse errorResponse = new P2LineupErrorResponse();
        errorResponse.addError( p2LineupError );

        httpServer.addResourceErrorResponse( NexusLineupPublisher.P2_RESOURCE_URI, HttpURLConnection.HTTP_BAD_REQUEST,
                                             new P2LineupXstreamIO().writeErrorResponse( errorResponse ) );
        httpServer.setHttpPort( httpServer.getHttpPort() );
        httpServer.stop();
        httpServer.start();

        P2Lineup lineup = createLineup();
        P2LineupSourceRepository p2LineupSourceRepository = new P2LineupSourceRepository();
        p2LineupSourceRepository.setUrl( nexusUrl + sourceRepoUrl );
        lineup.addRepository( p2LineupSourceRepository );
        P2LineupInstallableUnit p2LineupInstallableUnit = new P2LineupInstallableUnit();
        p2LineupInstallableUnit.setId( "foo.IU" );
        lineup.addRootInstallableUnit( p2LineupInstallableUnit );

        IURepoPage page = createTestPage( lineup, nexusUrl );
        assertPageMessage( page, null, IMessageProvider.NONE );
        callValidate( page );
        assertPageMessage( page, Messages.remoteValidator_proxyWarning, IMessageProvider.WARNING );

        Table repoTable = (Table) getControlByName( page, RepositoryComposite.REPOSITORY_CONTROL );
        TableItem repoTableItem = repoTable.getItem( 0 );
        assertEquals( Images.WARN_REPOSITORY, repoTableItem.getImage() );
        RepositoryComposite repoComposite =
            (RepositoryComposite) getControlByName( page, IURepoPage.REPOSITORY_COMPOSITE );
        TableViewer repoViewer = repoComposite.getViewer();
        repoViewer.setSelection( new StructuredSelection( repoTableItem.getData() ) );
        assertPageMessage( page, errorMessage, IMessageProvider.WARNING );
    }

    @Test
    public void testRootIUError()
        throws Exception
    {
        String rootIUId = "foo.IU";
        String errorMessage = "Bad IU";
        P2LineupUnresolvedInstallableUnit p2LineupError = new P2LineupUnresolvedInstallableUnit();
        p2LineupError.setInstallableUnitId( rootIUId );
        p2LineupError.setErrorMessage( errorMessage );
        P2LineupErrorResponse errorResponse = new P2LineupErrorResponse();
        errorResponse.addError( p2LineupError );

        httpServer = new HttpServer();
        httpServer.addResourceErrorResponse( NexusLineupPublisher.P2_RESOURCE_URI, HttpURLConnection.HTTP_BAD_REQUEST,
                                             new P2LineupXstreamIO().writeErrorResponse( errorResponse ) );
        httpServer.start();

        String nexusUrl = httpServer.getHttpUrl();
        P2Lineup lineup = createLineup();
        P2LineupSourceRepository p2LineupSourceRepository = new P2LineupSourceRepository();
        p2LineupSourceRepository.setUrl( "http://foo" );
        lineup.addRepository( p2LineupSourceRepository );
        P2LineupInstallableUnit p2LineupInstallableUnit = new P2LineupInstallableUnit();
        p2LineupInstallableUnit.setId( rootIUId );
        lineup.addRootInstallableUnit( p2LineupInstallableUnit );

        IURepoPage page = createTestPage( lineup, nexusUrl );
        assertPageMessage( page, null, IMessageProvider.NONE );
        callValidate( page );
        assertPageMessage( page, Messages.remoteValidator_iuError, IMessageProvider.ERROR );

        Table repoTable = (Table) getControlByName( page, RootIUComposite.IU_CONTROL );
        TableItem repoTableItem = repoTable.getItem( 0 );
        assertEquals( Images.ERROR_INSTALLABLE_UNIT, repoTableItem.getImage() );
        RootIUComposite rootIUComposite = (RootIUComposite) getControlByName( page, IURepoPage.ROOT_IU_COMPOSITE );
        TableViewer repoViewer = rootIUComposite.getViewer();
        repoViewer.setSelection( new StructuredSelection( repoTableItem.getData() ) );
        assertPageMessage( page, errorMessage, IMessageProvider.ERROR );
    }

    @Test
    public void testRootIUWarning()
        throws Exception
    {
        String rootIUId = "foo.IU";
        String errorMessage = "Bad IU";
        P2LineupUnresolvedInstallableUnit p2LineupError = new P2LineupUnresolvedInstallableUnit();
        p2LineupError.setInstallableUnitId( rootIUId );
        p2LineupError.setErrorMessage( errorMessage );
        p2LineupError.setWarning( true );
        P2LineupErrorResponse errorResponse = new P2LineupErrorResponse();
        errorResponse.addError( p2LineupError );

        httpServer = new HttpServer();
        httpServer.addResourceErrorResponse( NexusLineupPublisher.P2_RESOURCE_URI, HttpURLConnection.HTTP_BAD_REQUEST,
                                             new P2LineupXstreamIO().writeErrorResponse( errorResponse ) );
        httpServer.start();

        String nexusUrl = httpServer.getHttpUrl();
        P2Lineup lineup = createLineup();
        P2LineupSourceRepository p2LineupSourceRepository = new P2LineupSourceRepository();
        p2LineupSourceRepository.setUrl( "http://foo" );
        lineup.addRepository( p2LineupSourceRepository );
        P2LineupInstallableUnit p2LineupInstallableUnit = new P2LineupInstallableUnit();
        p2LineupInstallableUnit.setId( rootIUId );
        lineup.addRootInstallableUnit( p2LineupInstallableUnit );

        IURepoPage page = createTestPage( lineup, nexusUrl );
        assertPageMessage( page, null, IMessageProvider.NONE );
        callValidate( page );
        assertPageMessage( page, Messages.remoteValidator_iuError, IMessageProvider.WARNING );

        Table repoTable = (Table) getControlByName( page, RootIUComposite.IU_CONTROL );
        TableItem repoTableItem = repoTable.getItem( 0 );
        assertEquals( Images.WARN_INSTALLABLE_UNIT, repoTableItem.getImage() );
        RootIUComposite rootIUComposite = (RootIUComposite) getControlByName( page, IURepoPage.ROOT_IU_COMPOSITE );
        TableViewer repoViewer = rootIUComposite.getViewer();
        repoViewer.setSelection( new StructuredSelection( repoTableItem.getData() ) );
        assertPageMessage( page, errorMessage, IMessageProvider.WARNING );
    }

    private void callValidate( IURepoPage page )
    {
        page.getNextPage();
    }

    private void assertPageMessage( WizardPage page, String message, int messageType )
    {
        assertEquals( "Unexpected page message", message, page.getMessage() );
        assertEquals( "Unexpected page message type", messageType, page.getMessageType() );
    }

    private P2Lineup createLineup()
    {
        P2Lineup lineup = new P2Lineup();
        lineup.setGroupId( "com.mycompany" );
        lineup.setId( "IURepoPageTest" );
        lineup.setVersion( "1.0" );
        return lineup;
    }

    private IURepoPage createTestPage( P2Lineup lineup )
    {
        return createTestPage( lineup, null );
    }

    private IURepoPage createTestPage( P2Lineup lineup, String nexusUrl )
    {
        if ( wizard != null )
        {
            wizard.dispose();
        }
        wizard = new DummyWizard();

        NexusLineupPublishingInfo info = new NexusLineupPublishingInfo( lineup );
        info.setServerUrl( nexusUrl );
        IURepoPage page = new IURepoPage( info );

        wizard.addPage( page );
        // assertPageNotComplete( page );

        Display display = Display.getCurrent();
        if ( display == null )
        {
            display = new Display();
        }
        Shell shell = new Shell( display );

        dialog = new WizardDialog( shell, wizard );
        dialog.create();
        // dialog.open();

        // assertPageComplete( page );
        return page;
    }
}
