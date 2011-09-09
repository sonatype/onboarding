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
package com.sonatype.s2.project.ui.internal.composites;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.composites.ValidatingComposite;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.maven.ide.eclipse.ui.common.validation.SonatypeValidators;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;

import com.sonatype.s2.project.ui.internal.Messages;

public class ProjectNameTemplateComposite
    extends ValidatingComposite
{
    private static final String GROUP_ID = "\\[groupId\\]"; //$NON-NLS-1$

    private static final String ARTIFACT_ID = "\\[artifactId\\]"; //$NON-NLS-1$

    private static final String VERSION = "\\[version\\]"; //$NON-NLS-1$

    private Combo nameTemplateCombo;

    private Text projectNameText;

    private String projectName;

    private String groupId = "";

    private String artifactId = "";

    private String version = "";

    public ProjectNameTemplateComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                                         String namePrefix )
    {
        super( parent, widthGroup, validationGroup );
        setLayout( new GridLayout( 2, false ) );

        createNameTemplateControls( namePrefix );
        createProjectNameControls();
        addToValidationGroup( nameTemplateCombo, new Validator<String>()
        {
            private Validator<String> idValidator = SonatypeValidators.createArtifactIdValidators();

            public void validate( Problems problems, String componentName, String value )
            {
                value = calculateNameTemplate();
                StringValidators.REQUIRE_NON_EMPTY_STRING.validate( problems, componentName, value );
                idValidator.validate( problems, componentName, value );
                SonatypeValidators.EXISTS_IN_WORKSPACE.validate( problems, componentName, value );
            }

            public Class<String> modelType()
            {
                return String.class;
            }
        } );
    }

    private void createNameTemplateControls( String namePrefix )
    {
        Label label = new Label( this, SWT.NONE );
        label.setText( Messages.projectNameTemplateComposite_template_label );
        label.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        addToWidthGroup( label );

        nameTemplateCombo = new Combo( this, getCComboStyle() );
        nameTemplateCombo.setLayoutData( createInputData() );

        nameTemplateCombo.add( namePrefix + "-[artifactId]" ); //$NON-NLS-1$
        nameTemplateCombo.add( namePrefix + "-[artifactId]-[version]" ); //$NON-NLS-1$
        nameTemplateCombo.add( namePrefix + "-[groupId]-[artifactId]-[version]" ); //$NON-NLS-1$
        nameTemplateCombo.setText( nameTemplateCombo.getItem( 0 ) ); // default template

        nameTemplateCombo.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                expandNameTemplate();
            }
        } );
        SwtValidationGroup.setComponentName( nameTemplateCombo, Messages.projectNameTemplateComposite_projectName_name );
    }

    private void createProjectNameControls()
    {
        Label label = new Label( this, SWT.NONE );
        label.setText( Messages.projectNameTemplateComposite_projectName_label );
        label.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        addToWidthGroup( label );

        projectNameText = new Text( this, SWT.BORDER | SWT.READ_ONLY );
        projectNameText.setLayoutData( createInputData() );
        projectNameText.setData( "name", "projectNameText" ); //$NON-NLS-1$ //$NON-NLS-2$
        projectNameText.setEnabled( false );
        projectNameText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                projectName = projectNameText.getText();
            }
        } );
    }

    protected void expandNameTemplate()
    {
        projectNameText.setText( calculateNameTemplate() );
        getValidationGroup().performValidation();
    }

    protected String calculateNameTemplate()
    {
        String template = nameTemplateCombo.getText();
        if ( template.length() == 0 )
        {
            return "";
        }

        String name = template.replaceAll( GROUP_ID, groupId )//
        .replaceAll( ARTIFACT_ID, artifactId )//
        .replaceAll( VERSION, version == null ? "" : version ); //$NON-NLS-1$

        return name;
    }

    public void setCoordinates( String groupId, String artifactId, String version )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;

        expandNameTemplate();
    }

    public String getProjectName()
    {
        return projectName;
    }
}
