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

import java.util.List;

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.ui.materialization.Images;
import com.sonatype.s2.project.ui.materialization.Messages;

public class TreeRootNode
    extends CodebaseViewNode
{
    public TreeRootNode( IWorkspaceCodebase workspaceCodebase )
    {
        super( workspaceCodebase, Messages.treeRootNode_title, null, Images.TREES );

        IWorkspaceCodebase pendingCodebase = workspaceCodebase.getPending();

        List<IWorkspaceSourceTree> trees =
            ( pendingCodebase == null ? workspaceCodebase : pendingCodebase ).getSourceTrees();
        if ( trees != null )
        {
            SourceTreeNode[] sourceTrees = new SourceTreeNode[trees.size()];
            for ( int i = sourceTrees.length - 1; i >= 0; i-- )
            {
                sourceTrees[i] = new SourceTreeNode( workspaceCodebase, trees.get( i ) );
            }
            setChildren( sourceTrees );
        }
    }
}
