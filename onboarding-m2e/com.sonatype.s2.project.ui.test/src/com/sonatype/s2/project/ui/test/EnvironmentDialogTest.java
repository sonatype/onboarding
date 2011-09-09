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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.m2e.integration.tests.common.UIIntegrationTestCase;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.junit.Test;

import com.sonatype.s2.p2lineup.model.IP2LineupTargetEnvironment;
import com.sonatype.s2.p2lineup.model.P2LineupTargetEnvironment;
import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.project.ui.lineup.dialogs.EnvironmentDialog;

public class EnvironmentDialogTest
    extends UIIntegrationTestCase
{
    private static final String IU_ID = "iu.id";

    private EnvironmentDialog ed;

    @Test
    public void test()
    {
        P2LineupTargetEnvironment environment = new P2LineupTargetEnvironment();
        environment.setOsgiArch( Platform.ARCH_X86 );
        environment.setOsgiOS( Platform.OS_WIN32 );
        environment.setOsgiWS( Platform.WS_WIN32 );
        final Set<IP2LineupTargetEnvironment> targetEnvironments = new LinkedHashSet<IP2LineupTargetEnvironment>();
        targetEnvironments.add( environment );

        bot.getDisplay().syncExec( new Runnable()
        {
            public void run()
            {
                ed = new EnvironmentDialog( bot.getDisplay().getActiveShell(), IU_ID, targetEnvironments );
                ed.setBlockOnOpen( false );
                ed.open();
            }
        } );

        bot.shell( Messages.environmentDialog_title ).activate();

        SWTBotTable table = bot.table( 0 );
        assertFalse( "\"Allow all\" should not be selected", bot.radio( Messages.environmentDialog_allowAll ).isSelected() );
        assertTrue( "\"Allow some\" should be selected", bot.radio( Messages.environmentDialog_allowSome ).isSelected() );
        assertTrue( "Environment list should be enabled", table.isEnabled() );

        Set<IP2LineupTargetEnvironment> selection = ed.getSelection();
        assertEquals( "One item should be selected", 1, selection.size() );
        assertEquals( "win32/win32/x86 should be selected", environment, selection.iterator().next() );

        bot.radio( Messages.environmentDialog_allowAll ).click();
        assertTrue( "\"Allow all\" should be selected", bot.radio( Messages.environmentDialog_allowAll ).isSelected() );
        assertFalse( "\"Allow some\" should not be selected", bot.radio( Messages.environmentDialog_allowSome ).isSelected() );
        assertFalse( "Environment list should be disabled", table.isEnabled() );
        assertNull( "The dialog should return null if \"allow all\" is selected", ed.getSelection() );

        bot.radio( Messages.environmentDialog_allowSome ).click();
        assertFalse( "\"Allow all\" should not be selected", bot.radio( Messages.environmentDialog_allowAll ).isSelected() );
        assertTrue( "\"Allow some\" should be selected", bot.radio( Messages.environmentDialog_allowSome ).isSelected() );
        assertTrue( "Environment list should be enabled", table.isEnabled() );
        assertEquals( "An item should be selected again", 1, ed.getSelection().size() );

        bot.button( "OK" ).click();
    }
}
