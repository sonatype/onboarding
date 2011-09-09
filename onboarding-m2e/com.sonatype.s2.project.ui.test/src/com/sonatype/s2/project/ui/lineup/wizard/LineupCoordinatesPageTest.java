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

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputComposite;
import org.maven.ide.eclipse.ui.tests.common.AbstractWizardPageTest;

import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.publisher.nexus.NexusLineupPublisher;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;

public class LineupCoordinatesPageTest
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
    public void testNexusDoesNotSupportLineups()
        throws Exception
    {
        httpServer = new HttpServer();
        httpServer.addResources( "/", "resources/NexusUrlPageTest/testNexusUrlGoodCredentials", "xml" );
        httpServer.addResourceErrorResponse( NexusLineupPublisher.P2_RESOURCE_URI, 404 );
        httpServer.start();
        String nexusUrl = httpServer.getHttpUrl();

        LineupCoordinatesPage page = createTestPage();
        setText( page, UrlInputComposite.URL_CONTROL_NAME, nexusUrl );

        page.getNextPage();
        assertEquals( Messages.lineupCoordinatesPage_messageForNotFoundException, page.getMessage() );
    }

    private LineupCoordinatesPage createTestPage()
    {
        if ( wizard != null )
        {
            wizard.dispose();
        }
        wizard = new DummyWizard();

        NexusLineupPublishingInfo info = new NexusLineupPublishingInfo();
        info.getLineup().setGroupId( "com.mycompany" );
        info.getLineup().setId( "baselineup" );
        info.getLineup().setVersion( "1.0" );
        LineupCoordinatesPage page = new LineupCoordinatesPage( info );

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
