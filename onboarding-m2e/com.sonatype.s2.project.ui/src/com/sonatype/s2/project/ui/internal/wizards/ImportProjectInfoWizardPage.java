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
package com.sonatype.s2.project.ui.internal.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.composites.GAVComposite;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;

import com.sonatype.s2.project.ui.internal.composites.ProjectNameTemplateComposite;

public class ImportProjectInfoWizardPage
    extends WizardPage
{
    private GAVComposite gavComposite;

    private ProjectNameTemplateComposite projectNameTemplateComposite;

    private SwtValidationGroup validationGroup;

    private WidthGroup widthGroup;

    private String coordinatesTitle;

    private String templateTitle;

    private String templatePrefix;

    public ImportProjectInfoWizardPage()
    {
        super( ImportProjectInfoWizardPage.class.getName() );

        validationGroup = SwtValidationGroup.create( SwtValidationUI.createUI( this ) );
        widthGroup = new WidthGroup();
    }

    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayout( new GridLayout() );
        composite.addControlListener( widthGroup );

        setControl( composite );

        Label coordinatesLabel = new Label( composite, SWT.NONE );
        coordinatesLabel.setText( coordinatesTitle );

        gavComposite = new GAVComposite( composite, widthGroup, validationGroup, null, 0 );
        GridData gavData = new GridData( SWT.FILL, SWT.TOP, true, false );
        gavData.horizontalIndent = 10;
        gavComposite.setLayoutData( gavData );
        gavComposite.setEditable( false );

        Label templateLabel = new Label( composite, SWT.NONE );
        templateLabel.setText( templateTitle );
        GridData templateLabelData = new GridData( SWT.LEFT, SWT.TOP, false, false );
        templateLabelData.verticalIndent = 10;
        templateLabel.setLayoutData( templateLabelData );

        projectNameTemplateComposite =
            new ProjectNameTemplateComposite( composite, widthGroup, validationGroup, templatePrefix );
        GridData templateData = new GridData( SWT.FILL, SWT.TOP, true, false );
        templateData.horizontalIndent = 10;
        projectNameTemplateComposite.setLayoutData( templateData );
    }

    public String getProjectName()
    {
        return projectNameTemplateComposite.getProjectName();
    }

    public void setCoordinates( String groupId, String artifactId, String version )
    {
        gavComposite.setGroupId( groupId );
        gavComposite.setArtifactId( artifactId );
        gavComposite.setVersion( version );

        projectNameTemplateComposite.setCoordinates( groupId, artifactId, version );
    }

    public void setCoordinatesTitle( String text )
    {
        coordinatesTitle = text;
    };

    public void setTemplateTitle( String text )
    {
        templateTitle = text;
    };

    public void setTemplatePrefix( String text )
    {
        templatePrefix = text;
    };
}
