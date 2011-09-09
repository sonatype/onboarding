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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.integration.tests.common.UIIntegrationTestCase;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.widgets.Form;
import org.junit.Assert;
import org.junit.Test;

import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.S2ProjectFacade;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.codebase.NewCodebaseProjectOperation;
import com.sonatype.s2.project.ui.codebase.editor.CodebaseDescriptorEditor;
import com.sonatype.s2.publisher.S2PublisherConstants;

public class ProjectDescriptorEditorTest
    extends UIIntegrationTestCase
{
    private static final String ARTIFACT_ID_CONTROL = "artifactIdText";

    private static final String BUILDS_CONTROL = "buildsText";

    private static final String BUILDS_URL = "http://builds";

    private static final String DESCRIPTION = "description";

    private static final String DESCRIPTION_CONTROL = "descriptionText";

    private static final String DOCUMENTATION_CONTROL = "documentationText";

    private static final String ECLIPSE_INSTALL_CONTROL = "eclipsePathText";

    private static final String ECLIPSE_INSTALL_CUSTOMIZE_CONTROL = "eclipsePathCheckbox";

    private static final String ECLIPSE_INSTALL_PATH = "eclipse/install/path";

    private static final String ECLIPSE_WORKSPACE_CONTROL = "workspacePathText";

    private static final String ECLIPSE_WORKSPACE_CUSTOMIZE_CONTROL = "workspacePathCheckbox";

    private static final String ECLIPSE_WORKSPACE_PATH = "eclipse/workspace/path";

    private static final String FEED_URL = "http://feed";

    private static final String ISSUE_TRACKING_CONTROL = "issueTrackingText";

    private static final String ISSUES_URL = "http://issues";

    private static final String MAVEN_SETTINGS_CONTROL = "mavenSettingsLocationText";

    private static final String MAVEN_SETTINGS_URL = "http://mavensettings";

    private static final String MODULE_DOCS_URL = "http://moduledocs";

    private static final String MODULE_HOME_CONTROL = "homepageText";

    private static final String MODULE_HOME_URL = "http://modulehome";

    private static final String MODULE_NAME = "module1";

    private static final String MODULE_NAME_2 = "module2";

    private static final String MODULE_NAME_CONTROL = "moduleNameText";

    private static final String ORIGINAL_PROJECT_NAME = "s2project1";

    private static final String P2_LINEUP_CONTROL = "p2LineupLocationText";

    private static final String P2_LINEUP_URL = "http://p2lineuplocation";

    private static final String PROFILE = "profile";

    private static final String PROJECT_DOCS_URL = "http://projectdocs";

    private static final String PROJECT_HOME_CONTROL = "homepageText";

    private static final String PROJECT_HOME_URL = "http://projecthome";

    private static final String PROJECT_NAME = "s2project2";

    private static final String PROJECT_NAME_CONTROL = "projectNameText";

    private static final String REQUIRED_MEMORY = "512";

    private static final String REQUIRED_MEMORY_CONTROL = "requiredMemoryText";

    private static final String ROOT = "root";

    private static final String SCM_LOCATION_CONTROL = "scmUrlText";

    private static final String SCM_LOCATION_URL = "http://scmlocation";

    private static final String SCM_TYPE_COMBO_CONTROL = "scmTypeCombo";

    private static final String SCM_TYPE = "svn";

    private void addRemoveItems( int index, String title, String value )
    {
        SWTBotTable table = bot.table( index );

        bot.button( "Add...", index ).click();
        bot.shell( title ).activate();
        assertFalse( "The OK button should be disabled until the text is entered", bot.button( "OK" ).isEnabled() );
        bot.text().setText( value );
        bot.button( "OK" ).click();
        assertEquals( "A record has just been added to the list", 1, table.rowCount() );

        bot.button( "Add...", index ).click();
        bot.shell( title ).activate();
        bot.text().setText( value + '2' );
        bot.button( "OK" ).click();
        assertEquals( "A second record has just been added to the list", 2, table.rowCount() );

        table.select( 1 );
        bot.button( "Remove", index ).click();
        assertEquals( "The second record has just been removed from the list", 1, table.rowCount() );
    }

    private void addModule( SWTBotTable modules, String name )
    {
        bot.button( "Add...", 0 ).click();
        bot.shell( Messages.sourceTreeListComposite_add_title ).activate();
        assertFalse( "The Finish button should be disabled until the module name is entered",
                     bot.button( "Finish" ).isEnabled() );
        bot.textWithName( MODULE_NAME_CONTROL ).setText( name );
        assertFalse( "The Finish button should be disabled until the scm location is entered",
                     bot.button( "Finish" ).isEnabled() );
        bot.ccomboBoxWithName( SCM_TYPE_COMBO_CONTROL ).setSelection( SCM_TYPE );
        bot.textWithName( SCM_LOCATION_CONTROL ).setText( SCM_LOCATION_URL );
        bot.button( "Finish" ).click();
    }

    private void addRemoveSecondModule( SWTBotTable modules )
    {
        addModule( modules, MODULE_NAME_2 );
        assertEquals( "A second module has just been added", 2, modules.rowCount() );

        bot.cTabItem( MODULE_NAME ).activate();
        bot.cTabItem( Messages.codebaseDetailsPage_title ).activate();

        modules.select( 1 );
        bot.button( "Remove", 0 ).click();
        bot.shell( Messages.projectEditor_modules_delete_title ).activate();
        bot.button( "OK" ).click();
        assertEquals( "The second module has just been deleted", 1, modules.rowCount() );
    }

    private void changeFileAndValidateReload()
        throws CoreException, IOException
    {
        bot.cTabItem( Messages.codebaseDetailsPage_title ).activate();

        bot.viewById( PACKAGE_EXPLORER_VIEW_ID ).setFocus();
        IFile file = getPmdFile();

        InputStream in = null;
        IS2Project project;
        try
        {
            in = file.getContents();
            project = S2ProjectFacade.loadProject( in, false );
            project.setName( ORIGINAL_PROJECT_NAME );
        }
        finally
        {
            if ( in != null )
            {
                in.close();
            }
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        S2ProjectFacade.writeProject( project, buffer );
        ByteArrayInputStream is = new ByteArrayInputStream( buffer.toByteArray() );
        file.setContents( is, false, true, monitor );

        bot.editorByTitle( PROJECT_NAME ).setFocus();
        bot.textWithName( PROJECT_NAME_CONTROL ).setFocus();
        waitForAllBuildsToComplete();

        assertEquals( "Project name (restored from file)", ORIGINAL_PROJECT_NAME,
                      bot.textWithName( PROJECT_NAME_CONTROL ).getText() );
    }

    private void checkEditorMessageAfterSave()
    {
        SWTBotEditor editor = bot.editorByTitle( ORIGINAL_PROJECT_NAME );
        IEditorPart editorPart = editor.getReference().getEditor( true );
        assertTrue( "CodebaseDescriptorEditor", editorPart instanceof CodebaseDescriptorEditor );
        Form form =
            ( (CodebaseDescriptorEditor) editorPart ).getActivePageInstance().getManagedForm().getForm().getForm();
        bot.textWithName( ARTIFACT_ID_CONTROL ).setText( "" );
        bot.sleep( 500 );
        String message = form.getMessage();
        bot.menu( "File" ).menu( "Save" ).click();
        waitForAllBuildsToComplete();
        assertEquals( "Editor validation message should not disappear after save", message, form.getMessage() );
    }

    private void createFirstModule( SWTBotTable modules )
    {
        addModule( modules, "module" );
        assertEquals( "A module has just been added", 1, modules.rowCount() );
    }

    private void createS2Project()
        throws CoreException
    {
        NewCodebaseProjectOperation op =
            new NewCodebaseProjectOperation( ORIGINAL_PROJECT_NAME, "groupId", ORIGINAL_PROJECT_NAME, "1.0.0", null );
        op.createProject( monitor );

        bot.editorByTitle( ORIGINAL_PROJECT_NAME ).setFocus();
    }

    private void renameFirstModule( SWTBotTable modules )
    {
        SWTBotText moduleName = bot.textWithName( MODULE_NAME_CONTROL );
        moduleName.setFocus();
        moduleName.setText( MODULE_NAME );

        bot.cTabItem( Messages.codebaseDetailsPage_title ).activate();
        assertEquals( "First module has just been renamed", MODULE_NAME, modules.cell( 0, 0 ) );
    }

    private void reopenEditor()
    {
        bot.viewById( PACKAGE_EXPLORER_VIEW_ID ).setFocus();
        selectProject( ORIGINAL_PROJECT_NAME ).expandNode( ORIGINAL_PROJECT_NAME ).expandNode(
                                                                                               S2PublisherConstants.PMD_PATH ).getNode(
                                                                                                                                        IS2Project.PROJECT_DESCRIPTOR_FILENAME ).doubleClick();
        bot.editorByTitle( PROJECT_NAME ).setFocus();
    }

    private void saveAndClose()
    {
        bot.menu( "File" ).menu( "Close" ).click();
        bot.shell( "Save Resource" ).activate();
        bot.button( "Yes" ).click();
    }

    private void setModuleData()
    {
        bot.ccomboBoxWithName( SCM_TYPE_COMBO_CONTROL ).setSelection( SCM_TYPE );

        setUrlText( Messages.scmLocationComposite_location_url, SCM_LOCATION_CONTROL, SCM_LOCATION_URL );
        setUrlText( Messages.sourceTreeInfoComposite_homepage_name, MODULE_HOME_CONTROL, MODULE_HOME_URL );
        setUrlText( Messages.sourceTreeInfoComposite_documentation_name, DOCUMENTATION_CONTROL, MODULE_DOCS_URL );
        setUrlText( Messages.sourceTreeInfoComposite_issues_name, ISSUE_TRACKING_CONTROL, ISSUES_URL );
        setUrlText( Messages.sourceTreeInfoComposite_builds_name, BUILDS_CONTROL, BUILDS_URL );
    }

    private void setProjectData()
    {
        bot.textWithName( PROJECT_NAME_CONTROL ).setText( PROJECT_NAME );
        bot.textWithName( DESCRIPTION_CONTROL ).setText( DESCRIPTION );

        bot.textWithName( PROJECT_HOME_CONTROL ).setText( PROJECT_HOME_URL );
        bot.textWithName( DOCUMENTATION_CONTROL ).setText( PROJECT_DOCS_URL );

        bot.textWithName( P2_LINEUP_CONTROL ).setText( P2_LINEUP_URL );
        setUrlText( Messages.projectEditor_mavenSettings_label, MAVEN_SETTINGS_CONTROL, MAVEN_SETTINGS_URL );

        SWTBotText eclipseInstallText = bot.textWithName( ECLIPSE_INSTALL_CONTROL );
        SWTBotCheckBox eclipseInstallCheckbox = bot.checkBoxWithName( ECLIPSE_INSTALL_CUSTOMIZE_CONTROL );
        assertEquals( "Eclipse install path default", IS2Project.DEFAULT_INSTALL_PATH, eclipseInstallText.getText() );
        assertTrue( "Customize install path checkbox is enabled", eclipseInstallCheckbox.isEnabled() );
        assertTrue( "Customize install path checkbox is checked", eclipseInstallCheckbox.isChecked() );
        eclipseInstallText.setText( "" );
        assertTrue( "Customize install path checkbox is disabled", !eclipseInstallCheckbox.isEnabled() );
        eclipseInstallText.setText( ECLIPSE_INSTALL_PATH );
        assertTrue( "Customize install path checkbox is enabled", eclipseInstallCheckbox.isEnabled() );

        SWTBotText eclipseWorkspaceText = bot.textWithName( ECLIPSE_WORKSPACE_CONTROL );
        SWTBotCheckBox eclipseWorkspaceCheckbox = bot.checkBoxWithName( ECLIPSE_WORKSPACE_CUSTOMIZE_CONTROL );
        assertEquals( "Eclipse workspace path default", IS2Project.DEFAULT_WORKSPACE_PATH,
                      eclipseWorkspaceText.getText() );
        assertTrue( "Customize workspace path checkbox is enabled", eclipseWorkspaceCheckbox.isEnabled() );
        assertTrue( "Customize workspace path checkbox is checked", eclipseWorkspaceCheckbox.isChecked() );
        eclipseWorkspaceText.setText( "" );
        assertTrue( "Customize workspace path checkbox is disabled", !eclipseWorkspaceCheckbox.isEnabled() );
        eclipseWorkspaceText.setText( ECLIPSE_WORKSPACE_PATH );
        assertTrue( "Customize workspace path checkbox is enabled", eclipseWorkspaceCheckbox.isEnabled() );
        eclipseWorkspaceCheckbox.deselect();

        bot.textWithName( REQUIRED_MEMORY_CONTROL ).setText( REQUIRED_MEMORY );
    }

    private void setUrlText( String hyperlinkLabel, String controlName, String urlText )
    {
        bot.textWithName( controlName ).setText( urlText );
    }

    @Test
    public void test()
        throws CoreException, IOException
    {
        createS2Project();

        setProjectData();

        SWTBotTable modules = bot.table( 0 );

        createFirstModule( modules );
        setModuleData();
        addRemoveItems( 0, Messages.projectEditor_modules_roots_newTitle, ROOT );
        addRemoveItems( 1, Messages.projectEditor_modules_profiles_newTitle, PROFILE );
        addRemoveItems( 2, Messages.projectEditor_modules_feeds_newTitle, FEED_URL );

        renameFirstModule( modules );
        addRemoveSecondModule( modules );

        saveAndClose();

        validateSavedProject();

        reopenEditor();

        validateProjectForm();
        validateModuleForm();

        changeFileAndValidateReload();

        checkEditorMessageAfterSave();
    }

    private void validateModuleForm()
    {
        bot.cTabItem( MODULE_NAME ).activate();

        assertEquals( "Module name", MODULE_NAME, bot.textWithName( MODULE_NAME_CONTROL ).getText() );
        assertEquals( "Module home URL", MODULE_HOME_URL, bot.textWithName( MODULE_HOME_CONTROL ).getText() );
        assertEquals( "Module docs URL", MODULE_DOCS_URL, bot.textWithName( DOCUMENTATION_CONTROL ).getText() );
        assertEquals( "Module issues URL", ISSUES_URL, bot.textWithName( ISSUE_TRACKING_CONTROL ).getText() );
        assertEquals( "Module builds URL", BUILDS_URL, bot.textWithName( BUILDS_CONTROL ).getText() );
        assertEquals( "Module SCM location type", SCM_TYPE, bot.ccomboBoxWithName( SCM_TYPE_COMBO_CONTROL ).getText() );
        assertEquals( "Module SCM location URL", SCM_LOCATION_URL, bot.textWithName( SCM_LOCATION_CONTROL ).getText() );

        SWTBotTable roots = bot.table( 0 );
        assertEquals( "Module roots", 1, roots.rowCount() );
        assertEquals( "Module root name in the list", ROOT, roots.cell( 0, 0 ) );

        SWTBotTable profiles = bot.table( 1 );
        assertEquals( "Module profiles", 1, profiles.rowCount() );
        assertEquals( "Module profile name in the list", PROFILE, profiles.cell( 0, 0 ) );

        SWTBotTable feeds = bot.table( 2 );
        assertEquals( "Module feeds", 1, feeds.rowCount() );
        assertEquals( "Module feed URL in the list", FEED_URL, feeds.cell( 0, 0 ) );
    }

    private void validateProjectForm()
    {
        assertEquals( "Project name", PROJECT_NAME, bot.textWithName( PROJECT_NAME_CONTROL ).getText() );
        assertEquals( "Project description", DESCRIPTION, bot.textWithName( DESCRIPTION_CONTROL ).getText() );
        assertEquals( "Project home URL", PROJECT_HOME_URL, bot.textWithName( PROJECT_HOME_CONTROL ).getText() );
        assertEquals( "Project docs URL", PROJECT_DOCS_URL, bot.textWithName( DOCUMENTATION_CONTROL ).getText() );
        assertEquals( "P2 lineup URL", P2_LINEUP_URL, bot.textWithName( P2_LINEUP_CONTROL ).getText() );
        assertEquals( "Maven settings URL", MAVEN_SETTINGS_URL, bot.textWithName( MAVEN_SETTINGS_CONTROL ).getText() );
        assertEquals( "Eclipse install path", ECLIPSE_INSTALL_PATH,
                      bot.textWithName( ECLIPSE_INSTALL_CONTROL ).getText() );
        assertTrue( "Customize eclipse path checkbox is checked",
                    bot.checkBoxWithName( ECLIPSE_INSTALL_CUSTOMIZE_CONTROL ).isChecked() );
        assertEquals( "Eclipse workspace path", ECLIPSE_WORKSPACE_PATH,
                      bot.textWithName( ECLIPSE_WORKSPACE_CONTROL ).getText() );
        assertTrue( "Customize eclipse path checkbox is not checked",
                    !bot.checkBoxWithName( ECLIPSE_WORKSPACE_CUSTOMIZE_CONTROL ).isChecked() );
        assertEquals( "Required memory", REQUIRED_MEMORY, bot.textWithName( REQUIRED_MEMORY_CONTROL ).getText() );

        SWTBotTable modules = bot.table( 0 );
        assertEquals( "Number of modules", 1, modules.rowCount() );
        assertEquals( "Module name in the list", MODULE_NAME, modules.cell( 0, 0 ) );
    }

    private void validateSavedProject()
        throws CoreException, IOException
    {
        IFile pmdFile = getPmdFile();
        Assert.assertTrue( "The S2 project file should be accessible", pmdFile.isAccessible() );

        InputStream is = null;
        try
        {
            is = pmdFile.getContents();
            IS2Project project = S2ProjectFacade.loadProject( is, false );

            assertEquals( "Project name", PROJECT_NAME, project.getName() );
            assertEquals( "Project description", DESCRIPTION, project.getDescription() );
            assertEquals( "Project home URL", PROJECT_HOME_URL, project.getHomeUrl() );
            assertEquals( "Project docs URL", PROJECT_DOCS_URL, project.getDocsUrl() );
            assertEquals( "P2 lineup URL", P2_LINEUP_URL, project.getP2LineupLocation().getUrl() );
            assertEquals( "Maven settings URL", MAVEN_SETTINGS_URL, project.getMavenSettingsLocation().getUrl() );
            assertEquals( "Eclipse install path", ECLIPSE_INSTALL_PATH,
                          project.getEclipseInstallationLocation().getDirectory() );
            assertTrue( "Eclipse install path is customizable",
                        project.getEclipseInstallationLocation().isCustomizable() );
            assertEquals( "Eclipse workspace path", ECLIPSE_WORKSPACE_PATH,
                          project.getEclipseWorkspaceLocation().getDirectory() );
            assertTrue( "Eclipse workspace path is not customizable",
                        !project.getEclipseWorkspaceLocation().isCustomizable() );
            assertEquals( "Required memory", REQUIRED_MEMORY + 'M', project.getPrerequisites().getRequiredMemory() );

            List<IS2Module> modules = project.getModules();
            assertEquals( "Number of modules", 1, modules.size() );

            IS2Module module = modules.get( 0 );
            assertEquals( "Module name", MODULE_NAME, module.getName() );
            assertEquals( "Module home URL", MODULE_HOME_URL, module.getHomeUrl() );
            assertEquals( "Module docs URL", MODULE_DOCS_URL, module.getDocsUrl() );
            assertEquals( "Module issues URL", ISSUES_URL, module.getIssuesUrl() );
            assertEquals( "Module builds URL", BUILDS_URL, module.getBuildUrl() );
            assertEquals( "Module SCM location", "scm:" + SCM_TYPE + ':' + SCM_LOCATION_URL,
                          module.getScmLocation().getUrl() );

            assertEquals( "Module roots", 1, module.getRoots().size() );
            assertEquals( "Module root name", ROOT, module.getRoots().get( 0 ) );
            assertEquals( "Module profiles", 1, module.getProfiles().size() );
            assertEquals( "Module profile name", PROFILE, module.getProfiles().get( 0 ) );
            assertEquals( "Module feeds", 1, module.getFeeds().size() );
            assertEquals( "Module feed URL", FEED_URL, module.getFeeds().get( 0 ) );
        }
        finally
        {
            if ( is != null )
            {
                is.close();
            }
        }
    }

    protected IFile getPmdFile()
    {
        IPath pmdPath =
            new Path( ORIGINAL_PROJECT_NAME + "/" + S2PublisherConstants.PMD_PATH + "/"
                + IS2Project.PROJECT_DESCRIPTOR_FILENAME );
        IFile pmdFile = ResourcesPlugin.getWorkspace().getRoot().getFile( pmdPath );
        return pmdFile;
    }
}
