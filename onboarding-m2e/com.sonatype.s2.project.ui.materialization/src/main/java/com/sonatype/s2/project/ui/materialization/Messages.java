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
package com.sonatype.s2.project.ui.materialization;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "com.sonatype.s2.project.ui.materialization.messages"; //$NON-NLS-1$

    public static String codebaseUpdateJob_automaticNotPossible;

    public static String codebaseUpdateJob_title;

    public static String codebaseUpdateView_checkAction_title;

    public static String codebaseUpdateView_checkAction_tooltip;

    public static String codebaseUpdateView_column_name;

    public static String codebaseUpdateView_column_status;

    public static String codebaseUpdateView_column_url;

    public static String codebaseUpdateView_copyAction_title;

    public static String codebaseUpdateView_copyAction_tooltip;

    public static String codebaseUpdateView_errorCreatingTeamProvider;

    public static String codebaseUpdateView_helpAction_title;

    public static String codebaseUpdateView_helpAction_tooltip;

    public static String codebaseUpdateView_linkTemplate;

    public static String codebaseUpdateView_materializeNow;

    public static String codebaseUpdateView_noCodebasesFound;

    public static String codebaseUpdateView_refreshJob;

    public static String codebaseUpdateView_statusFormat;

    public static String codebaseUpdateView_synchronizeAction_title;

    public static String codebaseUpdateView_synchronizeAction_tooltip;

    public static String codebaseUpdateView_updateAction_title;

    public static String codebaseUpdateView_updateAction_tooltip;

    public static String codebaseUpdateView_updateAllAction_title;

    public static String codebaseUpdateView_updateAllAction_tooltip;

    public static String codebaseUpdateView_updateLink_tooltip;

    public static String eclipseNode_status_notLineupManaged;

    public static String eclipseNode_status_notUpToDate;

    public static String eclipseNode_status_unknown;

    public static String eclipseNode_status_upToDate;

    public static String eclipseNode_status_error;
    
    public static String eclipseNode_title;

    public static String eclipseNode_updateJob;

    public static String eclipsePreferencesNode_title;

    public static String mavenSettingsNode_title;

    public static String sourceTreeNode_job_updateCodebase;

    public static String sourceTreeNode_status_added;

    public static String sourceTreeNode_status_changed;

    public static String sourceTreeNode_status_notSupported;

    public static String sourceTreeNode_status_removed;

    public static String sourceTreeNode_status_upToDate;

    public static String treeRootNode_title;

    public static String workspaceNode_title;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages( BUNDLE_NAME, Messages.class );
    }

    private Messages()
    {
    }
}
