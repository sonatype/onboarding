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
package com.sonatype.m2e.egit.internal;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "com.sonatype.m2e.egit.internal.messages"; //$NON-NLS-1$

    static
    {
        // initialize resource bundle
        NLS.initializeMessages( BUNDLE_NAME, Messages.class );
    }

    private Messages()
    {
    }
    
    public static String noUpstreamBranch;
    public static String noUpstreamBranchHelp;

    public static String unknownLocalBranch;
    public static String unknownLocalBranchHelp;

    public static String unknownRemote;
    public static String unknownRemoteHelp;

    public static String unknownRefspec;
    public static String unknownRefspecHelp;

    public static String stagedChanges;
    public static String stagedChangesHelp;

    public static String localCommits;
    public static String localCommitsHelp;

    public static String mergeConflicts;
    public static String mergeConflictsHelp;

    public static String mergeFailed;
    public static String mergeFailedHelp;

    public static String hardMergeConflicts;
    public static String hardMergeConflictsHelp;
}
