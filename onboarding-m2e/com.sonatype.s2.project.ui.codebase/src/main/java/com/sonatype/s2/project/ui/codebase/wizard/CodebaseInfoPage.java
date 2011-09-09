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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.codebase.composites.CodebaseInfoComposite;

public class CodebaseInfoPage
    extends WizardPage
{
    private CodebaseInfoComposite codebaseInfoComposite;

    private IS2Project project;

    private Image codebaseImage;

    protected CodebaseInfoPage( IS2Project project )
    {
        super( CodebaseInfoPage.class.getName() );
        this.project = project;

        setDescription( Messages.codebaseInfoComposite_description );
        setTitle( Messages.codebaseInfoComposite_title );
    }

    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayout( new GridLayout() );

        SwtValidationGroup validationGroup = SwtValidationGroup.create( SwtValidationUI.createUI( this ) );
        WidthGroup widthGroup = new WidthGroup();
        composite.addControlListener( widthGroup );

        codebaseInfoComposite = new CodebaseInfoComposite( composite, widthGroup, validationGroup, null )
        {
            @Override
            protected void saveImage( Image image )
            {
                setImage( image );
                codebaseImage = image;
            }
        };
        codebaseInfoComposite.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );
        codebaseInfoComposite.setProject( project );

        setControl( composite );
    }

    public Image getCodebaseImage()
    {
        return codebaseImage;
    }
}
