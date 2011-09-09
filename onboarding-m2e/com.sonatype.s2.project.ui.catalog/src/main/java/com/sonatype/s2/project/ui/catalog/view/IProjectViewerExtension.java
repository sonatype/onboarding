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

public interface IProjectViewerExtension
{
    /** Sets the project editor instance. */
    public abstract void setProjectViewer( ProjectDescriptorViewer projectViewer );

    /** Contributes page content. */
    public abstract void createPageContent( ProjectViewerPage page, Composite body, FormToolkit toolkit );

    /** Adds extra pages to the form editor. */
    public abstract void createPages() throws PartInitException;
}