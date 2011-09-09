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
package com.sonatype.s2.project.ui.materialization.update.spi;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.graphics.Image;

import com.sonatype.s2.project.core.IWorkspaceCodebase;

public interface CodebaseViewNodeProvider
{

    Image getImage();

    String getURL();

    String getName();

    String getStatus();

    boolean isUpdateAvailable();

    IStatus update(IProgressMonitor monitor);
    
    void setWorkspaceCodebase( IWorkspaceCodebase codebase );

}
