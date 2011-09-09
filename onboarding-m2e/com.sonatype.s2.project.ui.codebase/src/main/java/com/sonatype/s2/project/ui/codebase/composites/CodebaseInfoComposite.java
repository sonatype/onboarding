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

import static org.maven.ide.eclipse.ui.common.FormUtils.nvl;
import static org.maven.ide.eclipse.ui.common.FormUtils.toNull;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.maven.ide.eclipse.ui.common.validation.SonatypeValidators;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.codebase.CodebaseImage;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.internal.Images;

public class CodebaseInfoComposite
    extends CodebaseComposite
{
    private Text projectNameText;

    private Text descriptionText;

    private Label imageLabel;

    private Text imageText;

    private Button imageBrowseButton;

    private Text homepageText;

    private Text documentationText;

    public CodebaseInfoComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                                  FormToolkit toolkit )
    {
        super( parent, widthGroup, validationGroup, toolkit );

        setLayout( new GridLayout( 3, false ) );

        createProjectNameControls();
        createDescriptionControls();
        createImageControls();
        createHomepageControls();
        createDocumentationControls();
    }

    private void createProjectNameControls()
    {
        createLabel( Messages.codebaseInfoComposite_projectName_label );

        projectNameText =
            createText( SWT.NONE, 2, 1, "projectNameText", Messages.codebaseInfoComposite_projectName_name ); //$NON-NLS-1$
        projectNameText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                getProject().setName( toNull( projectNameText ) );
                notifyCodebaseChangeListeners();
            }
        } );
        addToValidationGroup( projectNameText, StringValidators.REQUIRE_NON_EMPTY_STRING );
    }

    private void createDescriptionControls()
    {
        createLabel( Messages.codebaseInfoComposite_description_label );

        descriptionText = createText( SWT.MULTI | SWT.WRAP | SWT.V_SCROLL, 2, 2, "descriptionText", null ); //$NON-NLS-1$  
        descriptionText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                getProject().setDescription( toNull( descriptionText ) );
                notifyCodebaseChangeListeners();
            }
        } );
        GridData descriptionTextData = (GridData) descriptionText.getLayoutData();
        descriptionTextData.grabExcessVerticalSpace = true;
        descriptionTextData.heightHint = 100;
        descriptionTextData.minimumHeight = 100;
        descriptionTextData.verticalAlignment = SWT.FILL;
    }

    private void createImageControls()
    {
        imageLabel = new Label( this, SWT.NONE );
        imageLabel.setLayoutData( new GridData( SWT.LEFT, SWT.BOTTOM, false, false ) );
        imageLabel.setImage( Images.DEFAULT_PROJECT_IMAGE );

        createLabel( Messages.codebaseInfoComposite_image_label );

        imageText = createText( SWT.READ_ONLY, 1, 1, null, null );
        imageText.setEnabled( false );

        imageBrowseButton = createButton( Messages.codebaseInfoComposite_image_browse );
        imageBrowseButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                FileDialog fd = new FileDialog( getShell(), SWT.OPEN );
                fd.setFilterExtensions( new String[] { "*.gif;*.jpg;*.jpeg;*.png" } ); //$NON-NLS-1$
                fd.setText( Messages.codebaseInfoComposite_image_dialog );

                String filename = fd.open();
                if ( filename != null )
                {
                    IPath imagePath = new Path( filename );
                    Image image = CodebaseImage.getImage( imagePath );
                    saveImage( image );
                    imageText.setText( imagePath.toString() );
                    notifyCodebaseChangeListeners();
                }
            }
        } );
        imageBrowseButton.setEnabled( false );
    }

    private void createHomepageControls()
    {
        createLabel( Messages.codebaseInfoComposite_homepage_label );

        homepageText = createText( SWT.NONE, 2, 1, "homepageText", Messages.codebaseInfoComposite_homepage_name ); //$NON-NLS-1$
        homepageText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                getProject().setHomeUrl( toNull( homepageText ) );
                notifyCodebaseChangeListeners();
            }
        } );
        addToValidationGroup( homepageText, SonatypeValidators.EMPTY_OR_URL );
    }

    private void createDocumentationControls()
    {
        createLabel( Messages.codebaseInfoComposite_documentation_label );

        documentationText =
            createText( SWT.NONE, 2, 1, "documentationText", Messages.codebaseInfoComposite_documentation_name ); //$NON-NLS-1$
        documentationText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                getProject().setDocsUrl( toNull( documentationText ) );
                notifyCodebaseChangeListeners();
            }
        } );
        addToValidationGroup( documentationText, SonatypeValidators.EMPTY_OR_URL );
    }

    @Override
    protected void update( IS2Project project )
    {
        projectNameText.setText( nvl( project.getName() ) );
        descriptionText.setText( nvl( project.getDescription() ) );
        imageBrowseButton.setEnabled( true );
        homepageText.setText( nvl( project.getHomeUrl() ) );
        documentationText.setText( nvl( project.getDocsUrl() ) );
    }

    public void setImage( Image image )
    {
        imageLabel.setImage( image == null ? Images.DEFAULT_PROJECT_IMAGE : image );
        if ( imageText.getText().length() == 0 && image != null )
        {
            // only show the default image filename if there's an image, and the filename has not been modified
            imageText.setText( IS2Project.PROJECT_ICON_FILENAME );
        }
    }

    protected void saveImage( Image image )
    {
        // client override
    }
}
