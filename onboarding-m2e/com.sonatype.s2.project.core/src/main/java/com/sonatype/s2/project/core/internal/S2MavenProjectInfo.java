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
package com.sonatype.s2.project.core.internal;

import java.io.File;

import org.apache.maven.model.Model;
import org.eclipse.m2e.core.project.MavenProjectInfo;

import com.sonatype.s2.project.core.IWorkspaceSourceTree;

public class S2MavenProjectInfo
    extends MavenProjectInfo
{

    private final IWorkspaceSourceTree module;

    public S2MavenProjectInfo( IWorkspaceSourceTree module, String label, File pomFile, Model model, MavenProjectInfo parent )
    {
        super( label, pomFile, model, parent );
        this.module = module;
    }

    public IWorkspaceSourceTree getS2Module()
    {
        return module;
    }
}
