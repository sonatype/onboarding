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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.maven.ide.eclipse.ui.common.authentication.RealmUrlCollector;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;

import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.prefs.PreferenceGroup;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.codebase.composites.CodebaseGAVComposite;
import com.sonatype.s2.project.ui.codebase.composites.CodebaseInfoComposite;
import com.sonatype.s2.project.ui.codebase.composites.EclipseInstallationComposite;
import com.sonatype.s2.project.ui.codebase.composites.EclipseWorkspaceComposite;
import com.sonatype.s2.project.ui.codebase.composites.SourceTreeListComposite;

public class CodebaseDetailsPage
    extends AbstractCodebaseEditorPage
{
    private IS2Project project;

    private CodebaseGAVComposite gavComposite;

    private CodebaseInfoComposite codebaseInfoComposite;

    private EclipseInstallationComposite eclipseInstallationComposite;

    private EclipseWorkspaceComposite eclipseWorkspaceComposite;

    private SourceTreeListComposite sourceTreeListComposite;

    public CodebaseDetailsPage( CodebaseDescriptorEditor editor )
    {
        super( editor, Messages.codebaseDetailsPage_title );
    }

    @Override
    protected void createFormContent( IManagedForm managedForm )
    {
        final FormToolkit toolkit = managedForm.getToolkit();
        ScrolledForm form = managedForm.getForm();
        form.setText( Messages.codebaseDetailsPage_header );
        toolkit.decorateFormHeading( form.getForm() );

        Composite body = form.getBody();
        body.setLayout( new GridLayout( 2, true ) );

        WidthGroup leftGroup = new WidthGroup();
        body.addControlListener( leftGroup );
        WidthGroup rightGroup = new WidthGroup();
        body.addControlListener( rightGroup );

        createGAVSection( toolkit, body, leftGroup );
        createModulesSection( toolkit, body );
        createInfoSection( toolkit, body, leftGroup );
        createEclipseSection( toolkit, body, rightGroup );
        createWorkspaceSection( toolkit, body, rightGroup );

        populateToolbar( toolkit, form );

        toolkit.paintBordersFor( body );
        updatePage();
    }

    private void createGAVSection( FormToolkit toolkit, Composite body, WidthGroup widthGroup )
    {
        Section section = toolkit.createSection( body, Section.TITLE_BAR | Section.DESCRIPTION );
        gavComposite = new CodebaseGAVComposite( section, widthGroup, getValidationGroup(), toolkit );
        gavComposite.addCodebaseChangeListener( this );
        section.setClient( gavComposite );
        section.setText( Messages.gavComposite_title );
        section.setDescription( Messages.gavComposite_description );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, false, false ) );
    }

    private void createInfoSection( FormToolkit toolkit, Composite body, WidthGroup widthGroup )
    {
        Section section = toolkit.createSection( body, Section.TITLE_BAR | Section.DESCRIPTION );
        codebaseInfoComposite = new CodebaseInfoComposite( section, widthGroup, getValidationGroup(), toolkit )
        {
            @Override
            protected void saveImage( Image image )
            {
                ICodebaseEditorInput input = getCodebaseEditor().getCodebaseEditorInput();
                if ( input instanceof WorkspaceCodebaseEditorInput )
                {
                    ( (WorkspaceCodebaseEditorInput) input ).setCodebaseImage( image );
                    updateCodebaseImage();
                }
            }
        };
        codebaseInfoComposite.addCodebaseChangeListener( this );
        section.setClient( codebaseInfoComposite );
        section.setText( Messages.codebaseInfoComposite_title );
        section.setDescription( Messages.codebaseInfoComposite_description );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false, 1, 2 ) );
    }

    private void createEclipseSection( FormToolkit toolkit, Composite body, WidthGroup widthGroup )
    {
        Section section = toolkit.createSection( body, Section.TITLE_BAR | Section.DESCRIPTION );
        eclipseInstallationComposite =
            new EclipseInstallationComposite( section, widthGroup, getValidationGroup(), toolkit,
                                              EclipseInstallationComposite.ENABLE_P2_LINEUP_CONTROLS );
        eclipseInstallationComposite.addCodebaseChangeListener( this );
        eclipseInstallationComposite.addRealmChangeListener( this );
        section.setClient( eclipseInstallationComposite );
        section.setText( Messages.eclipseInstallationComposite_title );
        section.setDescription( Messages.eclipseInstallationComposite_description );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, false, false ) );
    }

    private void createWorkspaceSection( FormToolkit toolkit, Composite body, WidthGroup widthGroup )
    {
        Section section = toolkit.createSection( body, Section.TITLE_BAR | Section.DESCRIPTION );
        eclipseWorkspaceComposite = new EclipseWorkspaceComposite( section, widthGroup, getValidationGroup(), toolkit )
        {
            @Override
            protected void saveEclipsePreferenceGroups( Collection<PreferenceGroup> groups )
            {
                getCodebaseEditor().getCodebaseEditorInput().setEclipsePreferenceGroups( groups );
            }

            @Override
            protected Collection<PreferenceGroup> getEclipsePreferenceGroups()
            {
                return getCodebaseEditor().getCodebaseEditorInput().getEclipsePreferenceGroups();
            }
        };
        eclipseWorkspaceComposite.addCodebaseChangeListener( this );
        eclipseWorkspaceComposite.addRealmChangeListener( this );
        section.setClient( eclipseWorkspaceComposite );
        section.setText( Messages.eclipseWorkspaceComposite_title );
        section.setDescription( Messages.eclipseWorkspaceComposite_description );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
    }

    private void createModulesSection( FormToolkit toolkit, Composite body )
    {
        Section section = toolkit.createSection( body, Section.TITLE_BAR | Section.DESCRIPTION );
        sourceTreeListComposite = new SourceTreeListComposite( section, null, getValidationGroup(), toolkit )
        {
            @Override
            protected void removeModule( IS2Module module )
            {
                getCodebaseEditor().deleteModule( module );
            }

            @Override
            protected void editModule( int index )
            {
                getCodebaseEditor().showModule( index );
            }

            @Override
            protected void addModule( IS2Module module )
            {
                getCodebaseEditor().addModule( module );
            }
        };
        sourceTreeListComposite.addCodebaseChangeListener( this );
        section.setClient( sourceTreeListComposite );
        section.setText( Messages.sourceTreeListComposite_title );
        section.setDescription( Messages.sourceTreeListComposite_description );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
    }

    @Override
    protected void update()
    {
        project = getCodebaseEditor().getProject();
        if ( project == null || gavComposite == null )
        {
            return;
        }

        gavComposite.setProject( project );
        codebaseInfoComposite.setProject( project );
        updateCodebaseImage();
        eclipseInstallationComposite.setProject( project );
        eclipseWorkspaceComposite.setProject( project );
        sourceTreeListComposite.setProject( project );
        updateTitle();
    }

    private void updateCodebaseImage()
    {
        Image image = getCodebaseEditor().getCodebaseEditorInput().getCodebaseImage();
        codebaseInfoComposite.setImage( image );
    }

    public void saveRealms( RealmUrlCollector realmUrlCollector )
    {
        eclipseInstallationComposite.saveRealms( realmUrlCollector );
        eclipseWorkspaceComposite.saveRealms( realmUrlCollector );
    }

    void updateTitle()
    {
        getCodebaseEditor().setPartName( project.getName() != null ? project.getName()
                                                         : Messages.codebaseDetailsPage_untitled );
    }
}
