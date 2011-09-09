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
package com.sonatype.s2.project.ui.materialization.wizards;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.internal.Messages;
import com.sonatype.s2.project.ui.internal.wizards.AbstractMaterializationWizard;
import com.sonatype.s2.project.ui.internal.wizards.ProjectUrlPage;
import com.sonatype.s2.project.ui.materialization.MaterializationJob;

public class S2ProjectMaterializationWizard
    extends AbstractMaterializationWizard
    implements IImportWizard
{

    private ProjectUrlPage projectUrlPage;

    public S2ProjectMaterializationWizard()
    {
        this( null, null );
    }

    public S2ProjectMaterializationWizard( IS2Project project, IStatus validationStatus )
    {
        super();
        setWindowTitle( Messages.materializationWizard_title );

        this.project = project;
        this.validationStatus = validationStatus;
    }

    @Override
    public boolean performFinish()
    {
        new MaterializationJob( project ).schedule();

        return true;
    }

    public void init( IWorkbench workbench, IStructuredSelection selection )
    {
    }

    @Override
    public void addPages()
    {
        if ( project == null )
        {
            projectUrlPage = new ProjectUrlPage();
            addPage( projectUrlPage );
        }
        else
        {
            addMaterializationPages();
        }
    }

    @Override
    protected void addMaterializationPages()
    {
        super.addMaterializationPages();
    }

    @Override
    public boolean canFinish()
    {
        return project != null && super.canFinish();
    }
}
