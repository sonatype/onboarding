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
package com.sonatype.s2.project.core;


public class S2CodebaseChangeEvent
{
    private final IWorkspaceCodebase oldCodebase;

    private final IWorkspaceCodebase codebase;

    public S2CodebaseChangeEvent( IWorkspaceCodebase oldCodebase, IWorkspaceCodebase codebase )
    {
        this.oldCodebase = oldCodebase;
        this.codebase = codebase;
    }

    public IWorkspaceCodebase getCodebase()
    {
        return codebase;
    }

    public IWorkspaceCodebase getOldCodebase()
    {
        return oldCodebase;
    }
}
