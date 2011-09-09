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
package com.sonatype.s2.project.ui.materialization.update;

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.ui.materialization.Images;
import com.sonatype.s2.project.ui.materialization.Messages;

public class WorkspaceNode
    extends CodebaseViewNode
{
    public WorkspaceNode( IWorkspaceCodebase workspaceCodebase )
    {
        super( workspaceCodebase, Messages.workspaceNode_title, null, Images.WORKSPACE,
               new CodebaseViewNode[] { new TreeRootNode( workspaceCodebase )
               // , new MavenSettingsNode( workspaceCodebase )
               // , new EclipsePreferencesNode( workspaceCodebase )
               } );
    }
}