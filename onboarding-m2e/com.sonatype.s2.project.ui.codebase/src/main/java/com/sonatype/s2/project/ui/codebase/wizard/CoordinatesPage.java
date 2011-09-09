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
package com.sonatype.s2.project.ui.codebase.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.codebase.composites.CodebaseGAVComposite;
import com.sonatype.s2.project.ui.internal.composites.NexusUrlComposite;
import com.sonatype.s2.project.ui.internal.wizards.NexusUrlPage;

public class CoordinatesPage
    extends NexusUrlPage
{
    private CodebaseGAVComposite gavComposite;

    private IS2Project project;

    protected CoordinatesPage( IS2Project project )
    {
        this.project = project;

        setDescription( Messages.coordinatesPage_description );
        setTitle( Messages.coordinatesPage_title );
    }

    @Override
    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayout( new GridLayout() );
        composite.addControlListener( getWidthGroup() );

        Label nexusServerLabel = new Label( composite, SWT.NONE );
        nexusServerLabel.setText( Messages.coordinatesPage_nexusServer_label );

        NexusUrlComposite nexusUrlComposite = createNexusUrlComposite( composite );
        ( (GridData) nexusUrlComposite.getLayoutData() ).horizontalIndent = 10;

        Label codebaseCoordinatesLabel = new Label( composite, SWT.NONE );
        codebaseCoordinatesLabel.setText( Messages.coordinatesPage_codebaseCoordinates_label );
        GridData codebaseCoordinatesData = new GridData( SWT.LEFT, SWT.TOP, false, false );
        codebaseCoordinatesData.verticalIndent = 10;
        codebaseCoordinatesLabel.setLayoutData( codebaseCoordinatesData );

        gavComposite = new CodebaseGAVComposite( composite, getWidthGroup(), getValidationGroup(), null );
        GridData gavData = new GridData( SWT.FILL, SWT.TOP, true, false );
        gavData.horizontalIndent = 10;
        gavComposite.setLayoutData( gavData );
        gavComposite.setProject( project );

        setControl( composite );
    }
}
