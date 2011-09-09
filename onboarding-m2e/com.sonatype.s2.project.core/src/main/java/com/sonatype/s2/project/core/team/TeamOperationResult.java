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
package com.sonatype.s2.project.core.team;

import com.sonatype.s2.project.core.IWorkspaceSourceTree;

public class TeamOperationResult
{
    private final String status;

    private final String message;

    private final String help;

    public static final TeamOperationResult RESULT_UPTODATE =
        new TeamOperationResult( IWorkspaceSourceTree.STATUS_UPTODATE, null, null );

    public static final TeamOperationResult RESULT_CHANGED =
        new TeamOperationResult( IWorkspaceSourceTree.STATUS_CHANGED, null, null );

    public static final TeamOperationResult RESULT_UNAUTHORIZED =
        new TeamOperationResult( IWorkspaceSourceTree.STATUS_UNAUTHORIZED, null, null );

    public TeamOperationResult( String status, String message, String help )
    {
        this.status = status;
        this.message = message;
        this.help = help;
    }

    public String getStatus()
    {
        return status;
    }

    public String getMessage()
    {
        return message;
    }

    public String getHelp()
    {
        return help;
    }

    @Override
    public String toString()
    {
        return "Status:" + status + ", Message:" + message;
    }

}
