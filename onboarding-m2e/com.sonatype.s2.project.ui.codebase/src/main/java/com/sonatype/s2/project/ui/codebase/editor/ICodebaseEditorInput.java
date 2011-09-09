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
package com.sonatype.s2.project.ui.codebase.editor;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction.MavenPathStorageEditorInput;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.prefs.PreferenceGroup;

public interface ICodebaseEditorInput
    extends IEditorInput
{
    public IS2Project getProject();
    
    public String getTitle();

    public Image getCodebaseImage();

    public Collection<PreferenceGroup> getEclipsePreferenceGroups();

    public void setEclipsePreferenceGroups( Collection<PreferenceGroup> preferenceGroups );

    public MavenPathStorageEditorInput getMavenSettings();

    public void doSave( IProgressMonitor monitor )
        throws CoreException;
}
