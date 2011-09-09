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

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.m2e.integration.tests.common.HyperlinkBot;
import org.eclipse.m2e.integration.tests.common.UIIntegrationTestCase;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.sonatype.s2.project.core.IS2ProjectCatalogRegistry;
import com.sonatype.s2.project.core.IS2ProjectCatalogRegistryEntry;
import com.sonatype.s2.project.core.IS2ProjectCatalogRegistryListener;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.test.HttpServer;
import com.sonatype.s2.project.ui.catalog.Messages;

@RunWith( SWTBotJunit4ClassRunner.class )
public class CatalogViewTest
    extends UIIntegrationTestCase
{
    private static final String USERNAME = "username";

    private static final String PASSWORD = "password";

    private static final String CATALOG = "/catalog";

    private static final String CATALOG2 = "/catalog2";

    private static final String ROLE = "role";

    private static HttpServer httpServer;

    private static String baseUrl;

    @BeforeClass
    public static void createServer()
        throws Exception
    {
        httpServer = new HttpServer();
        httpServer.addSecuredRealm( "/*", ROLE );
        httpServer.addUser( USERNAME, PASSWORD, ROLE );
        httpServer.addResources( "/", "resources" );
        httpServer.setHttpPort( 8089 );
        httpServer.start();
        baseUrl = httpServer.getHttpUrl();

        createCatalogList();
    }

    @AfterClass
    public static void stopServer()
    {
        httpServer.stop();
    }

    private static void createCatalogList()
        throws Exception
    {
        IS2ProjectCatalogRegistry registry = S2ProjectCore.getInstance().getProjectCatalogRegistry();
        List<IS2ProjectCatalogRegistryEntry> entries = registry.getCatalogEntries( monitor );
        final CountDownLatch latch = new CountDownLatch( entries.size() + 1 );
        IS2ProjectCatalogRegistryListener listener = new IS2ProjectCatalogRegistryListener()
        {
            public void catalogRemoved( IS2ProjectCatalogRegistryEntry entry )
            {
                latch.countDown();
            }

            public void catalogAdded( IS2ProjectCatalogRegistryEntry entry )
            {
                latch.countDown();
            }
        };
        registry.addListener( listener );
        for ( IS2ProjectCatalogRegistryEntry entry : entries )
        {
            registry.removeCatalog( entry.getUrl() );
        }
        registry.addCatalog( baseUrl + CATALOG );
        latch.await();
        registry.removeListener( listener );
    }

    /**
     * Fresh workspace, first load.<BR/>
     * Tests authentication pop-up and persistent realms.
     * <P/>
     * 1. Open "Other views" from the menu. <BR/>
     * 2. Find the "Sonatype Studio" node, expand it, double click the "S2 Catalog" child (this verifies that the S2
     * Catalog view extension is registered and loaded). <BR/>
     * 3. Make sure the S2 Catalog view is actually open, focus on it. <BR/>
     * 4. An authentication dialog should pop up (the assumption is that the secure store is empty, so it should ask for
     * credentials). <BR/>
     * 5. Uncheck the "anonymous" checkbox, fill in the credentials. Click OK. <BR/>
     * 6. Find the S2 Catalog view again. <BR/>
     * 7. Expand the tree in the S2 Catalog view, find the "Test Catalog" node (it will appear once the default sample
     * catalog is loaded).
     */
    @Test
    @Ignore
    public void test1catalogViewInitialLoad()
        throws Exception
    {
        openCatalogView();

        focusOnCatalogView();

        enterCredentials( Messages.catalogContentProvider_errors_authenticationError );

        focusOnCatalogView();
        waitForAllBuildsToComplete();

        expandSampleCatalog();

        closeCatalogView();
    }

    /**
     * Same workspace. <BR/>
     * Clears the credentials persisted in the previous test, attempts to reload the catalog.
     * <P/>
     * 1. Open "Other views" from the menu. <BR/>
     * 2. Find the "Sonatype Studio" node, expand it, double click the "S2 Catalog" child (this verifies that the S2
     * Catalog view extension is registered and loaded). <BR/>
     * 3. Make sure the S2 Catalog view is actually open, focus on it. <BR/>
     * 4. Expand the tree in the S2 Catalog view, find the "Test Catalog" node (the catalog should load right away,
     * using the credentials from the previous session). <BR/>
     * 5. Select the top catalog node, click the "Credentials" button on the toolbar (the button should be enabled).<BR/>
     * 6. Focus on the credentials dialog. <BR/>
     * 7. Reset the credentials. <BR/>
     * 8. Reload the view using the toolbar button. <BR/>
     * 9. Enter the correct credentials. <BR/>
     * 10. Expand the tree in the S2 Catalog view, find the "Sample catalogue" node making sure the load was successful.
     */
    @Test
    @Ignore
    public void test2catalogViewPasswordChange()
        throws Exception
    {
        openCatalogView();

        focusOnCatalogView();

        expandSampleCatalog();

        UiHelpers.waitForAllUiJobsToComplete( 30 * 1000 );

        bot.tree().select( 0 );
        bot.activeView().toolbarButton( Messages.catalogView_actions_editCredentials_tooltip ).click();

        resetCredentials( Messages.catalogView_actions_editCredentials_dialogTitle );

        focusOnCatalogView();
        bot.activeView().toolbarButton( Messages.catalogView_actions_reload_tooltip ).click();

        enterCredentials( Messages.catalogContentProvider_errors_authenticationError );
        bot.sleep( 500 );

        focusOnCatalogView();
        expandSampleCatalog();

        closeCatalogView();
    }

    /**
     * Same workspace. <BR/>
     * Adds a second catalog, removes it, checks the URL history.
     * <P/>
     * 1. Open "Other views" from the menu. <BR/>
     * 2. Find the "Sonatype Studio" node, expand it, double click the "S2 Catalog" child (this verifies that the S2
     * Catalog view extension is registered and loaded). <BR/>
     * 3. Make sure the S2 Catalog view is actually open, focus on it. <BR/>
     * 4. Make sure there is only one item in the catalog. <BR/>
     * 5. Click the "Add" button on the toolbar, enter a new URL with credentials, click OK. <BR/>
     * 6. Focus back on the catalog, make sure there are now two items in the tree. <BR/>
     * 7. Find the new catalog record in the tree, select it. <BR/>
     * 8. Click the "Delete" button on the toolbar, select "OK". <BR/>
     * 9. Focus on the catalog again, make sure there's only one record left. <BR/>
     * 10. Open the "Add" dialog again, obtain the URL history. <BR/>
     * 11. Make sure the recently used catalog URL has been saved in the URL history.
     */
    @Test
    @Ignore
    public void test3catalogViewAddRemove()
        throws Exception
    {
        openCatalogView();

        focusOnCatalogView();
        assertEquals( 1, bot.tree().rowCount() );

        final String title = Messages.catalogView_actions_add_dialogTitle;
        final String catalogUrl = baseUrl + CATALOG2;

        bot.activeView().toolbarButton( Messages.catalogView_actions_add_tooltip ).click();
        bot.shell( title ).activate();
        bot.comboBoxWithLabel( Messages.catalogContentProvider_catalogUrlLabel ).setText( catalogUrl );
        enterCredentials( title );
        bot.sleep( 500 );

        focusOnCatalogView();
        assertEquals( 2, bot.tree().rowCount() );

        deleteCatalog2();

        focusOnCatalogView();
        assertEquals( 1, bot.tree().rowCount() );

        bot.activeView().toolbarButton( Messages.catalogView_actions_add_tooltip ).click();
        bot.shell( title ).activate();
        String[] history = bot.comboBoxWithLabel( Messages.catalogContentProvider_catalogUrlLabel ).items();
        assertEquals( 1, history.length );
        assertEquals( catalogUrl, history[0] );
        bot.button( "Cancel" ).click();

        closeCatalogView();
    }

    /**
     * Same workspace. <BR/>
     * Opens a project editor, flips through the tabs, tests the materialization button.
     * <P/>
     * 1. Open "Other views" from the menu. <BR/>
     * 2. Find the "Sonatype Studio" node, expand it, double click the "S2 Catalog" child (this verifies that the S2
     * Catalog view extension is registered and loaded). <BR/>
     * 3. Make sure the S2 Catalog view is actually open, focus on it. <BR/>
     * 4. Expand the "Test Catalog" in the S2 Catalog view, select the "Test Project" node. <BR/>
     * 5. Double-click the test project node. <BR/>
     * 6. Make sure a "Test Project" form editor is now open. <BR/>
     * 7. Switch to the "module2" tab, check if the page contains the hyperlinks from the project descriptor in all the
     * correct locations. <BR/>
     * 8. Switch back to the main project tab, check the hyperlinks there. <BR/>
     * 9. Find a "module1" hyperlink and click it. <BR/>
     * 10. Make sure the editor is now showing the first module page. <BR/>
     * 11. Click the "Materialize" link on the toolbar. <BR/>
     * 12. Wait for the materialization wizard to appear and close it. <BR/>
     * 13. Switch the focus back to the catalog view and select a different project. <BR/>
     * 14. Verify that the editor title and contents changed accordingly.
     */
    @Test
    @Ignore
    public void test4catalogEditor()
        throws Exception
    {
        openCatalogView();
        focusOnCatalogView();

        expandSampleCatalog();
        bot.sleep( 1500 );
        bot.tree().expandNode( "Test Catalog" ).getNode( "Test Project" ).select();
        enterCredentials( Messages.projectEditor_errors_eclipsePreferencesAuthenticationError );

        bot.editorByTitle( "Test Project" ).setFocus();

        bot.cTabItem( "module2" ).activate();
        bot.sleep( 500 );
        assertEquals( "http://home/module2",
                      bot.hyperlinkWithLabel( Messages.projectEditor_details_projectHome ).getText() );
        assertEquals( "http://docs/module2",
                      bot.hyperlinkWithLabel( Messages.projectEditor_details_documentation ).getText() );
        assertEquals( "http://issues/module2",
                      bot.hyperlinkWithLabel( Messages.projectEditor_details_issueTracking ).getText() );
        assertEquals( "http://build/module2", bot.hyperlinkWithLabel( Messages.projectEditor_details_builds ).getText() );
        assertEquals( "http://scm/module2",
                      bot.hyperlinkWithLabel( Messages.projectEditor_details_scmLocation ).getText() );

        bot.cTabItem( "Test Project" ).activate();
        bot.sleep( 500 );
        assertEquals( "http://home/url", bot.hyperlinkWithLabel( Messages.projectEditor_details_projectHome ).getText() );
        assertEquals( "http://docs/url",
                      bot.hyperlinkWithLabel( Messages.projectEditor_details_documentation ).getText() );
        assertEquals( "JDT, M2E", bot.hyperlinkWithLabel( Messages.projectEditor_details_eclipsePreferences ).getText() );

        HyperlinkBot module1Link = bot.hyperlinkWithLabel( Messages.projectEditor_modules_label );
        assertEquals( "module1", module1Link.getText() );
        module1Link.click();
        bot.sleep( 500 );
        assertEquals( "http://home/module1",
                      bot.hyperlinkWithLabel( Messages.projectEditor_details_projectHome ).getText() );

        bot.toolbarButtonWithTooltip( Messages.actions_materializeProject_tooltip ).click();
        bot.shell( Messages.materializationWizard_title ).activate();
        bot.button( "Cancel" ).click();

        focusOnCatalogView();

        bot.tree().expandNode( "Test Catalog" ).getNode( "Test Project 2" ).select();
        bot.sleep( 1000 );
        bot.editorByTitle( "Test Project 2" ).setFocus();
        assertEquals( "http://home/url2",
                      bot.hyperlinkWithLabel( Messages.projectEditor_details_projectHome ).getText() );

        closeCatalogView();
    }

    /**
     * Same workspace. <BR/>
     * Tests the empty view actions, adds/removes a catalog, checks URL history updates.
     * <P/>
     * 1. Open "Other views" from the menu. <BR/>
     * 2. Find the "Sonatype Studio" node, expand it, double click the "S2 Catalog" child (this verifies that the S2
     * Catalog view extension is registered and loaded). <BR/>
     * 3. Make sure the S2 Catalog view is actually open, focus on it. <BR/>
     * 4. Locate the "Test Catalog" item, select it. <BR/>
     * 5. Click the "Remove" button on the toolbar, confirm the removal. <BR/>
     * 6. Make sure that the empty catalog actions are now displayed. <BR/>
     * 7. Find the "Add Catalog" hyperlink, click it. <BR/>
     * 8. Enter the "yet another" catalog URL (same as in test3), confirm selection. <BR/>
     * 9. Check the number of items in the tree (should be 1). <BR/>
     * 10. Delete the recently added catalog. <BR/>
     * 11. Click the "Add Catalog" button on the toolbar. <BR/>
     * 11. Check the URL history, make sure the URL has only been saved once (despite the fact that it's been used
     * several times).
     */
    @Test
    @Ignore
    public void test5emptyCatalog()
        throws Exception
    {
        openCatalogView();
        focusOnCatalogView();

        bot.tree().expandNode( "Test Catalog" ).select().click();
        bot.activeView().toolbarButton( Messages.catalogView_actions_remove_tooltip ).click();
        bot.shell( Messages.catalogView_actions_remove_dialogTitle ).activate();
        bot.button( "OK" ).click();
        bot.sleep( 500 );

        focusOnCatalogView();

        HyperlinkBot addLink = bot.hyperlinkWithLabel( Messages.catalogView_empty_message );
        assertEquals( Messages.catalogView_empty_addNew, addLink.getText() );
        addLink.click();
        bot.sleep( 500 );

        final String title = Messages.catalogView_actions_add_dialogTitle;
        final String catalogUrl = baseUrl + CATALOG2;

        bot.shell( title ).activate();
        bot.comboBoxWithLabel( Messages.catalogContentProvider_catalogUrlLabel ).setText( catalogUrl );
        bot.button( "OK" ).click();
        bot.sleep( 500 );

        focusOnCatalogView();
        assertEquals( 1, bot.tree().rowCount() );

        deleteCatalog2();

        focusOnCatalogView();

        bot.activeView().toolbarButton( Messages.catalogView_actions_add_tooltip ).click();
        bot.shell( title ).activate();
        String[] history = bot.comboBoxWithLabel( Messages.catalogContentProvider_catalogUrlLabel ).items();
        assertEquals( 1, history.length );
        assertEquals( catalogUrl, history[0] );
        bot.button( "Cancel" ).click();

        closeCatalogView();
    }

    private void resetCredentials( String shellTitle )
    {
        bot.shell( shellTitle ).activate();
        bot.textWithLabel( "&Username:" ).setText( "" );
        bot.button( "OK" ).click();
    }

    private void expandSampleCatalog()
    {
        bot.tree().expandNode( "Test Catalog" );
    }

    private void focusOnCatalogView()
    {
        bot.viewByTitle( Messages.catalogView_title ).setFocus();
    }

    private void openCatalogView()
    {
        bot.menu( "Window" ).menu( "Show View" ).menu( "Other..." ).click();
        bot.shell( "Show View" ).activate();
        bot.tree().expandNode( Messages.mavenStudio ).getNode( Messages.catalogView_title ).doubleClick();
        waitForAllBuildsToComplete();
    }

    private void closeCatalogView()
    {
        bot.viewByTitle( Messages.catalogView_title ).close();
    }

    private void enterCredentials( String shellTitle )
    {
        bot.shell( shellTitle ).activate();
        bot.textWithLabel( "&Username:" ).setText( USERNAME );
        bot.textWithLabel( "&Password:" ).setText( PASSWORD );
        bot.button( "OK" ).click();

        bot.sleep( 500 );
        closeSecureStoragePopup();
    }

    private void closeSecureStoragePopup()
    {
        for ( SWTBotShell shell : bot.shells() )
        {
            if ( "Secure Storage".equals( shell.getText() ) )
            {
                shell.close();
            }
        }
    }

    private void deleteCatalog2()
    {
        bot.tree().expandNode( "Yet Another Catalog" ).select();
        bot.activeView().toolbarButton( Messages.catalogView_actions_remove_tooltip ).click();
        bot.shell( Messages.catalogView_actions_remove_dialogTitle ).activate();
        bot.button( "OK" ).click();
        bot.sleep( 500 );
    }
}
