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

import java.io.File;

import org.eclipse.m2e.integration.tests.common.UIIntegrationTestCase;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.sonatype.s2.project.tests.common.SshServer;
import com.sonatype.s2.project.ui.internal.Messages;
import com.sonatype.s2.ssh.SshHandlerManager;

@RunWith( SWTBotJunit4ClassRunner.class )
@Ignore
public class MaterializationWizardTest
    extends UIIntegrationTestCase
{
    protected static File root = new File( "" ).getAbsoluteFile();

    /**
     * Tests a project file with misconfigured security realms.
     * <P/>
     * 1. Open "Import" from the menu. <BR/>
     * 2. Find the "Sonatype Suite" node, expand it, double click the "Materialize Codebase" child (this verifies that
     * the S2 Wizard extension is registered and loaded). <BR/>
     * 3. Enter a project URL, click "Next" to attempt to load the project. <BR/>
     * 4. The "Finish" button should be disabled. <BR/>
     * 5. Cancel the action.
     */
    @Test
    public void test1brokenRealms()
        throws Exception
    {
        openProject( "resources/projects/brokenrealms.xml" );

        assertWizardError( "Undefined security realm with id='security.realm.this-should-not-be-here'" );
        assertFalse( "The Finish button should be disabled if realms failed to load",
                     bot.button( "Finish" ).isEnabled() );

        bot.button( "Cancel" ).click();
    }

    /**
     * Tests the realm page behavior.
     * <P/>
     * 1. Invoke the wizard from the "import" menu. <BR/>
     * 2. Enter a project URL, click "Next". <BR/>
     * 3. Check the error message, it should indicate errors. <BR/>
     * 4. The "Next" button should be disabled. <BR/>
     * 5. The "Ignore Errors" checkbox should be enabled, but deselected. <BR/>
     * 6. Check the "Ignore Errors" checkbox. <BR/>
     * 7. Make sure the "Next" button is now enabled so the wizard may proceed. <BR/>
     * 8. Cancel the action.
     */
    @Test
    public void test2ignoreAuthErrors()
        throws Exception
    {
        openProject( "resources/projects/autherrors.xml" );
        next();
        bot.sleep( 500 );
        next();

        assertWizardError( Messages.materializationWizard_realmsPage_errors_problemsDetected );
        assertTrue( "The Next button should be enabled now if there are validation errors",
                    bot.button( "Next >" ).isEnabled() );

        bot.button( "Cancel" ).click();
    }

    private void next()
    {
        bot.button( "Next >" ).click();
    }

    /**
     * Tests the validator page behavior.
     * <P/>
     * 1. Invoke the wizard from the "import" menu. <BR/>
     * 2. Enter a project URL, click "Next" (we expect no realm errors). <BR/>
     * 3. The error message should say that Eclipse installation is incompatible with the project. <BR/>
     * 4. The "Finish" button should be disabled. <BR/>
     * 5. Select the first failed item on the list. <BR/>
     * 6. Make sure the description field displays the correct status text. <BR/>
     * 7. The "Remediate" button should be disabled since the validator does not support it. <BR/>
     * 8. The "Ignore Errors" checkbox should be enabled, but deselected. <BR/>
     * 9. Check the "Ignore Errors" checkbox. <BR/>
     * 10. Make sure the "Finish" button is now enabled so the wizard may proceed. <BR/>
     * 11. Deselect the "Show Errors Only" checkbox. <BR/>
     * 12. Validate that a greater number of validation results are visible now. <BR/>
     * 13. Cancel the action.
     */
    @Test
    public void test3failingValidator()
        throws Exception
    {
        openProject( "resources/projects/failalways.xml" );
        next();

        assertWizardError( Messages.materializationWizard_validationPage_errors_validationKaputt );
        assertFalse( "The Finish button should be disabled if there are validation errors",
                     bot.button( "Finish" ).isEnabled() );

        bot.tree().select( 0 );
        bot.text( "FAIL" );
        assertFalse( "The Remediate button should be disabled if the validator does not support it",
                     bot.button( Messages.materializationWizard_validationPage_remediateButton ).isEnabled() );

        SWTBotCheckBox ignoreErrors =
            bot.checkBox( Messages.materializationWizard_validationPage_ignoreValidationResults );
        assertTrue( "The Ignore Errors checkbox should be enabled after the initial validation",
                    ignoreErrors.isEnabled() );
        assertFalse( "The Ignore Errors checkbox should not be checked after the initial validation",
                     ignoreErrors.isChecked() );

        ignoreErrors.select();
        assertTrue( "The Finish button should become enabled once the Ignore Errors checkbox is checked",
                    bot.button( "Finish" ).isEnabled() );

        int before = bot.tree().rowCount();
        bot.checkBox( Messages.validationStatusViewer_showErrorsOnly ).deselect();
        assertTrue( "The tree should display additional results after Show Errors Only is deselected",
                    before < bot.tree().rowCount() );

        bot.button( "Cancel" ).click();
    }

    /**
     * Tests the validator page behavior.
     * <P/>
     * 1. Invoke the wizard from the "import" menu. <BR/>
     * 2. Enter a project URL, click "Next"/"Next" (we expect no realm errors). <BR/>
     * 3. The error message should say that additional action is required. <BR/>
     * 4. The "Finish" button should be disabled. <BR/>
     * 5. Select the first failed item on the list. <BR/>
     * 6. Make sure the description field displays the correct status text. <BR/>
     * 7. The "Remediate" button should be enabled since the validator supports it. <BR/>
     * 8. Click "Remediate". <BR/>
     * 9. Make sure the "Finish" button is now enabled so the wizard may proceed. <BR/>
     * 10. The error message should be gone, the wizard should display the success message now. <BR/>
     * 11. Select "Show errors only", no errors should be displayed. <BR/>
     * 12. Cancel the action.
     */
    @Test
    public void test4remediatingValidator()
        throws Exception
    {
        openProject( "resources/projects/failfirst.xml" );
        next();

        assertWizardError( Messages.materializationWizard_validationPage_errors_validationIncomplete );
        assertFalse( "The Finish button should be disabled if there are validation errors",
                     bot.button( "Finish" ).isEnabled() );

        bot.tree().select( 0 );
        bot.text( "FAIL" );
        SWTBotButton remediateButton = bot.button( Messages.materializationWizard_validationPage_remediateButton );
        assertTrue( "The Remediate button should be enabled if the validator supports it", remediateButton.isEnabled() );

        remediateButton.click();

        bot.sleep( 1000 );
        bot.checkBox( Messages.validationStatusViewer_showErrorsOnly ).select();
        assertTrue( "The Finish button should become enabled once the errors are fixed",
                    bot.button( "Finish" ).isEnabled() );
        assertWizardError( null );
        assertWizardMessage( Messages.materializationWizard_validationPage_validationSuccessful );
        assertEquals( "No validation errors should remain on the list", 0, bot.tree().rowCount() );

        bot.button( "Cancel" ).click();
    }

    /**
     * Tests a straightforward project materialization attempt.
     * <P/>
     * 1. Invoke the wizard from the "import" menu. <BR/>
     * 2. Enter a project URL, click "Next"/"Next"/"Finish" - the import should proceed without errors. <BR/>
     * 3. After the build is complete, make sure the project is present in the package explorer. <BR/>
     * 4. Cleanup.
     */
    @Test
    public void test5simpleProject()
        throws Exception
    {
        openProject( "resources/projects/simpleproject.xml" );
        next();

        bot.button( "Finish" ).click();

        waitForAllBuildsToComplete();
        selectProject( "simple-project" );
        clearProjects();
    }

    /**
     * Tests an unknown SSH host popup.
     * <P/>
     * 1. Start an SSH server. <BR/>
     * 2. Invoke the wizard from the "import" menu. <BR/>
     * 3. Paste a PMD that contains to a git ssh URL. <BR/>
     * 4. The validator should pop up a warning message. <BR/>
     * 5. Dispose of the message and the wizard, cleanup.
     */
    @Test
    public void test6sshPopup()
        throws Exception
    {
        File sshDirectory = File.createTempFile( "SshPopupTest", "ssh" );
        sshDirectory.delete();
        File knownHosts = new File( sshDirectory, "known_hosts" );

        SshHandlerManager sshManager = SshHandlerManager.getInstance();
        sshManager.setSshDirectory( sshDirectory );

        SshServer sshServer = null;

        try
        {
            sshServer = new SshServer();
            sshServer.setPort( 8021 );
            sshServer.start();

            openProject( "resources/projects/git-ssh.xml" );
            next();

            bot.shell( Messages.sshHandler_title ).activate();
            bot.button( "No" ).click();

            bot.shell( Messages.materializationWizard_title ).activate();
            bot.button( "Cancel" ).click();
        }
        finally
        {
            sshServer.stop();

            knownHosts.delete();
            sshDirectory.delete();
        }
    }

    private void openWizard()
    {
        bot.menu( "File" ).menu( "Import..." ).click();
        bot.shell( "Import" ).activate();
        bot.tree().expandNode( Messages.mavenStudio ).getNode( Messages.materializationWizard_title ).doubleClick();
    }

    private void openProject( String filename )
    {
        openWizard();
        bot.comboBoxWithLabel( Messages.materializationWizard_urlPage_urlLabel ).setText(
                                                                                          new File( root, filename ).toURI().toString() );
        bot.button( "Next >" ).click();
    }
}
