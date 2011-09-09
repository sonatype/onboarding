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

import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.m2e.integration.tests.common.UIIntegrationTestCase;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBotAssert;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.sonatype.s2.project.tests.common.SvnServer;
import com.sonatype.s2.project.tests.common.Util;

@RunWith( SWTBotJunit4ClassRunner.class )
public class SubversiveTest
    extends UIIntegrationTestCase
{

    private SvnServer svnServer;

    @Before
    public void setUp()
        throws Exception
    {
        clearProjects();
    }

    @After
    public void tearDown()
        throws Exception
    {
        if ( svnServer != null )
        {
            svnServer.stop();
            svnServer = null;
        }
    }

    protected SvnServer startSvnServer( String dump, String config )
        throws Exception
    {
        svnServer = new SvnServer();
        svnServer.setDumpFile( "resources/scm/svn/" + dump );
        svnServer.setConfDir( "resources/scm/svn/" + config );
        svnServer.start();
        return svnServer;
    }

    private SWTBotView openRepositoriesView()
    {
        bot.menu( "Window" ).menu( "Show View" ).menu( "Other..." ).click();
        bot.shell( "Show View" ).activate();
        bot.tree().expandNode( "SVN" ).getNode( "SVN Repositories" ).doubleClick();
        return bot.viewById( "org.eclipse.team.svn.ui.repository.RepositoriesView" );
    }

    /**
     * Verify that the command "Checkout as Maven Project" is available from the Subversive repository view.
     */
    @Test
    public void testCheckoutAsMavenProjectFromSubversiveRepositoryView()
        throws Exception
    {
        startSvnServer( "simple.dump", "conf-a" );
        String svnUrl = svnServer.getUrl() + "/simple";

        SWTBotView view = openRepositoriesView();

        view.toolbarPushButton( "New Repository Location" ).click();
        bot.sleep( 100 );

        bot.shell( "New Repository Location" ).activate();
        bot.comboBox( 0 ).setText( svnUrl );
        bot.button( "Finish" ).click();
        bot.sleep( 200 );

        SWTBotTree tree = view.bot().tree();
        tree.setFocus();
        SWTBotTreeItem repoItem = tree.getTreeItem( svnUrl );
        repoItem.select().expand();
        bot.sleep( 100 );

        SWTBotAssert.assertEnabled( repoItem.getNode( 0 ).contextMenu( "Check out as Maven Project..." ) );
        repoItem.contextMenu( "Check out as Maven Project..." ).click();
        bot.sleep( 100 );

        bot.shell( "Checkout as Maven project from SCM" ).activate();
        bot.button( "Finish" ).click();
        waitForAllBuildsToComplete();
        Util.waitForTeamSharingJobs();

        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject( "svn-test" );
        assertTrue( project.isAccessible() );
        project.delete( true, true, null );

        view.close();
    }

}
