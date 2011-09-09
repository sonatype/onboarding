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
package com.sonatype.s2.project.ui.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.m2e.integration.tests.common.UIIntegrationTestCase;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.ui.internal.Messages;

@RunWith( SWTBotJunit4ClassRunner.class )
public class PreferencesExportWizardTest
    extends UIIntegrationTestCase
{

    private HttpServer httpServer;

    @After
    public void tearDown()
        throws Exception
    {
        if ( httpServer != null )
        {
            httpServer.stop();
            httpServer = null;
        }
    }

    protected HttpServer startHttpServer()
        throws Exception
    {
        httpServer = new HttpServer();
        httpServer.addResources( "/prefs", "target/test-classes/prefs/" );
        httpServer.addUser( "testuser", "testpass", "auth" );
        httpServer.addSecuredRealm( "/prefs/*", "auth" );
        httpServer.setHttpsPort( -1 );
        return httpServer.start();
    }

    /**
     * Verify that Eclipse preferences can be exported to a file.
     */
    @Test
    public void testExportToFile()
        throws Exception
    {
        File prefFile = new File( "target/test-classes/prefs/s2prefs.jar" );
        prefFile.delete();
        assertFalse( prefFile.exists() );

        bot.menu( "File" ).menu( "Export..." ).click();

        bot.shell( "Export" ).activate();
        bot.tree().expandNode( Messages.mavenStudio ).getNode( Messages.exportWizard_title ).doubleClick();

        bot.button( "Deselect All" ).click();
        assertFalse( bot.tree().getTreeItem( "JDT" ).isChecked() );
        assertFalse( bot.tree().getTreeItem( "M2E" ).isChecked() );

        bot.radio( "Save To File:" ).click();
        bot.comboBox( 0 ).setFocus();
        assertTrue( bot.comboBox( 0 ).isActive() );
        bot.comboBox( 0 ).setText( prefFile.getAbsolutePath() );
        assertTrue( bot.comboBox( 0 ).isActive() );

        bot.button( "Select All" ).click();
        assertTrue( bot.tree().getTreeItem( "JDT" ).isChecked() );
        assertTrue( bot.tree().getTreeItem( "M2E" ).isChecked() );

        bot.button( "Finish" ).click();
        waitForAllBuildsToComplete();

        assertTrue( prefFile.exists() );
        assertTrue( prefFile.length() > 0 );
    }

    /**
     * Verify that Eclipse preferences can be uploaded to a HTTP server requiring authentication.
     */
    @Test
    public void testUploadToServer()
        throws Exception
    {
        File prefFile = new File( "target/test-classes/prefs/s2prefs.jar" );
        prefFile.delete();
        assertFalse( prefFile.exists() );

        HttpServer httpServer = startHttpServer();
        String prefUrl = httpServer.getHttpUrl() + "/prefs/s2prefs.jar";

        bot.menu( "File" ).menu( "Export..." ).click();

        bot.shell( "Export" ).activate();
        bot.tree().expandNode( Messages.mavenStudio ).getNode( Messages.exportWizard_title ).doubleClick();

        bot.button( "Select All" ).click();
        assertTrue( bot.tree().getTreeItem( "JDT" ).isChecked() );
        assertTrue( bot.tree().getTreeItem( "M2E" ).isChecked() );

        bot.radio( "Upload To:" ).click();
        bot.comboBox( 1 ).setFocus();
        assertTrue( bot.comboBox( 1 ).isActive() );
        bot.comboBox( 1 ).setText( prefUrl );
        assertTrue( bot.comboBox( 1 ).isActive() );

        bot.textWithLabel( "&Username:" ).setText( "testuser" );
        bot.textWithLabel( "&Password:" ).setText( "testpass" );

        bot.sleep( 1000 );
        bot.button( "Finish" ).click();
        bot.sleep( 1000 );
        waitForAllBuildsToComplete();

        assertTrue( prefFile.exists() );
        assertTrue( prefFile.length() > 0 );
    }

}
