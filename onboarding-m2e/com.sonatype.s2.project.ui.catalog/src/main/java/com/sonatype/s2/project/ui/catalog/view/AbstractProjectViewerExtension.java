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
package com.sonatype.s2.project.ui.catalog.view;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;

abstract public class AbstractProjectViewerExtension
    implements IProjectViewerExtension
{
    protected ProjectDescriptorViewer projectViewer;

    public void setProjectViewer( ProjectDescriptorViewer projectViewer )
    {
        this.projectViewer = projectViewer;
    }

    public void createPageContent( ProjectViewerPage page, Composite body, FormToolkit toolkit )
    {
    }

    public void createPages() throws PartInitException
    {
    }
}
