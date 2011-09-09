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

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.prefs.PreferenceGroup;
import com.sonatype.s2.project.ui.internal.Images;

public abstract class AbstractCodebaseEditorInput
    extends PlatformObject
    implements ICodebaseEditorInput
{
    private ImageDescriptor imageDescriptor = Images.CATALOG_ENTRY_DESCRIPTOR;

    private IS2Project project;

    private Collection<PreferenceGroup> eclipsePreferenceGroups;

    protected AbstractCodebaseEditorInput()
    {
    }

    protected AbstractCodebaseEditorInput( IS2Project project )
    {
        this.project = project;
    }

    public IS2Project getProject()
    {
        return project;
    }

    public String getTitle()
    {
        return project == null ? null : project.getName();
    }
    protected void setProject( IS2Project project )
    {
        this.project = project;
    }

    public IPersistableElement getPersistable()
    {
        return null;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public Object getAdapter( Class adapter )
    {
        return super.getAdapter( adapter );
    }

    public boolean exists()
    {
        return project != null;
    }

    public String getName()
    {
        return project != null ? project.getName() : null;
    }

    public String getToolTipText()
    {
        return getName();
    }

    public ImageDescriptor getImageDescriptor()
    {
        return imageDescriptor;
    }

    protected void setImageDescriptor( ImageDescriptor imageDescriptor )
    {
        this.imageDescriptor = imageDescriptor;
    }

    public Collection<PreferenceGroup> getEclipsePreferenceGroups()
    {
        return eclipsePreferenceGroups;
    }

    public void setEclipsePreferenceGroups( Collection<PreferenceGroup> preferenceGroups )
    {
        eclipsePreferenceGroups = preferenceGroups;
    }
}
