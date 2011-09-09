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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;

import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;

abstract public class SourceTreeComposite
    extends CodebaseComposite
{
    private IS2Module module;

    public SourceTreeComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                                FormToolkit toolkit )
    {
        super( parent, widthGroup, validationGroup, toolkit );
    }

    @Override
    protected void update( IS2Project project )
    {
        update( project, module );
    }

    abstract protected void update( IS2Project project, IS2Module module );

    public void setModule( IS2Project project, IS2Module module )
    {
        this.module = module;
        setProject( project );
    }

    public IS2Module getModule()
    {
        return module;
    }

}
