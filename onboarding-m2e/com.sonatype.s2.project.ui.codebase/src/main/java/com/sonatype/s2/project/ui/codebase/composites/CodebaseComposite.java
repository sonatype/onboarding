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
package com.sonatype.s2.project.ui.codebase.composites;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.composites.ValidatingComposite;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;

import com.sonatype.s2.project.model.IS2Project;

abstract public class CodebaseComposite
    extends ValidatingComposite
{
    private ListenerList listeners;

    private boolean updating;

    private IS2Project project;

    public CodebaseComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                              FormToolkit toolkit )
    {
        super( parent, widthGroup, validationGroup, toolkit );
        this.listeners = new ListenerList();
    }

    public void setProject( IS2Project project )
    {
        this.project = project;
        update();
    }

    public IS2Project getProject()
    {
        return project;
    }

    public void addCodebaseChangeListener( ICodebaseChangeListener listener )
    {
        assert listener instanceof ICodebaseChangeListener;
        listeners.add( listener );
    }

    public void removeCodebaseChangeListener( ICodebaseChangeListener listener )
    {
        listeners.remove( listener );
    }

    protected void notifyCodebaseChangeListeners()
    {
        if ( !updating )
        {
            for ( Object listener : listeners.getListeners() )
            {
                ( (ICodebaseChangeListener) listener ).codebaseChanged( project );
            }
        }
    }

    public void update()
    {
        updating = true;
        update( project );
        updating = false;
    }

    abstract protected void update( IS2Project project );
}
