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
package com.sonatype.s2.project.ui.lineup.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.maven.ide.eclipse.ui.common.editor.ValidatingFormPage;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;

import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.project.ui.lineup.composites.ILineupChangeListener;
import com.sonatype.s2.project.ui.lineup.composites.LineupGAVComposite;
import com.sonatype.s2.project.ui.lineup.composites.LineupInfoComposite;
import com.sonatype.s2.project.ui.lineup.composites.RemoteValidator;
import com.sonatype.s2.project.ui.lineup.composites.RepositoryComposite;
import com.sonatype.s2.project.ui.lineup.composites.RootIUComposite;
import com.sonatype.s2.project.ui.lineup.composites.RuntimeEnvironmentComposite;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;

public class LineupEditorPage
    extends ValidatingFormPage
    implements ILineupChangeListener
{
    private LineupEditor editor;

    private LineupGAVComposite lineupGAVComposite;

    private RootIUComposite rootIUComposite;

    private LineupInfoComposite lineupInfoComposite;

    private RuntimeEnvironmentComposite runtimeEnvironmentComposite;

    private RepositoryComposite repositoryComposite;

    public LineupEditorPage( LineupEditor editor )
    {
        super( editor, LineupEditorPage.class.getName(), null );
        this.editor = editor;
    }

    @Override
    protected void createFormContent( IManagedForm managedForm )
    {
        final FormToolkit toolkit = managedForm.getToolkit();
        ScrolledForm form = managedForm.getForm();
        form.setText( Messages.lineupEditorPage_title );
        toolkit.decorateFormHeading( form.getForm() );

        Composite body = form.getBody();
        body.setLayout( new GridLayout( 2, true ) );

        WidthGroup leftGroup = new WidthGroup();
        body.addControlListener( leftGroup );
        WidthGroup rightGroup = new WidthGroup();
        body.addControlListener( rightGroup );

        createCoordinatesSection( toolkit, body, leftGroup );
        createIUSection( toolkit, body, rightGroup );
        createInfoSection( toolkit, body, leftGroup );
        createRuntimeSection( toolkit, body, leftGroup );
        createRepositorySection( toolkit, body, rightGroup );

        populateToolbar( toolkit, form );

        toolkit.paintBordersFor( body );
        updatePage();
    }

    private void createCoordinatesSection( FormToolkit toolkit, Composite parent, WidthGroup widthGroup )
    {
        Section section = toolkit.createSection( parent, Section.TITLE_BAR | Section.DESCRIPTION );
        lineupGAVComposite = new LineupGAVComposite( section, widthGroup, getValidationGroup(), toolkit );
        lineupGAVComposite.addLineupChangeListener( this );
        section.setClient( lineupGAVComposite );
        section.setDescription( Messages.lineupGAVComposite_description );
        section.setText( Messages.lineupGAVComposite_title );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
    }

    private void createIUSection( FormToolkit toolkit, Composite parent, WidthGroup widthGroup )
    {
        Section section = toolkit.createSection( parent, Section.TITLE_BAR | Section.DESCRIPTION );
        rootIUComposite = new RootIUComposite( section, widthGroup, getValidationGroup(), toolkit )
        {
            protected void addUnit()
            {
                super.addUnit();
                repositoryComposite.updateViewer();
            };
        };
        rootIUComposite.addLineupChangeListener( this );
        section.setClient( rootIUComposite );
        section.setDescription( Messages.rootIUComposite_description );
        section.setText( Messages.rootIUComposite_title );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false, 1, 2 ) );
    }

    private void createInfoSection( FormToolkit toolkit, Composite parent, WidthGroup widthGroup )
    {
        Section section = toolkit.createSection( parent, Section.TITLE_BAR | Section.DESCRIPTION );
        lineupInfoComposite = new LineupInfoComposite( section, widthGroup, getValidationGroup(), toolkit );
        lineupInfoComposite.addLineupChangeListener( this );
        section.setClient( lineupInfoComposite );
        section.setDescription( Messages.lineupInfoComposite_description );
        section.setText( Messages.lineupInfoComposite_title );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
    }

    private void createRuntimeSection( FormToolkit toolkit, Composite parent, WidthGroup widthGroup )
    {
        Section section = toolkit.createSection( parent, Section.TITLE_BAR | Section.DESCRIPTION );
        runtimeEnvironmentComposite =
            new RuntimeEnvironmentComposite( section, widthGroup, getValidationGroup(), toolkit );
        runtimeEnvironmentComposite.addLineupChangeListener( this );
        section.setClient( runtimeEnvironmentComposite );
        section.setDescription( Messages.runtimeEnvironmentComposite_description );
        section.setText( Messages.runtimeEnvironmentComposite_title );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
    }

    private void createRepositorySection( FormToolkit toolkit, Composite parent, WidthGroup widthGroup )
    {
        Section section = toolkit.createSection( parent, Section.TITLE_BAR | Section.DESCRIPTION );
        repositoryComposite = new RepositoryComposite( section, widthGroup, getValidationGroup(), toolkit );
        repositoryComposite.addLineupChangeListener( this );
        section.setClient( repositoryComposite );
        section.setDescription( Messages.repositoryComposite_description );
        section.setText( Messages.repositoryComposite_title );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
    }

    public void lineupChanged( NexusLineupPublishingInfo info )
    {
        setDirty( true );
    }

    @Override
    protected void update()
    {
        if ( lineupGAVComposite == null )
        {
            return;
        }

        NexusLineupPublishingInfo info = editor.getLineupInfo();
        repositoryComposite.setLineupInfo( info );
        runtimeEnvironmentComposite.setLineupInfo( info );
        lineupInfoComposite.setLineupInfo( info );
        rootIUComposite.setLineupInfo( info );
        lineupGAVComposite.setLineupInfo( info );
        updateTitle();
    }

    void updateTitle()
    {
        NexusLineupPublishingInfo info = editor.getLineupInfo();
        String name = info.getLineup().getId();

        editor.setPartName( name == null ? Messages.lineupEditorPage_title : name );
    }

    public IStatus validateLineup( IProgressMonitor monitor )
    {
        final IStatus status =
            new RemoteValidator( repositoryComposite, rootIUComposite ).validate( editor.getLineupInfo(), monitor );
        Display.getDefault().asyncExec( new Runnable()
        {
            public void run()
            {
                if ( status.isOK() )
                {
                    int type = getManagedForm().getForm().getMessageType();
                    if ( type > IMessageProvider.NONE )
                    {
                        getManagedForm().getForm().setMessage( null, type );
                    }
                }
                else
                {
                    getManagedForm().getForm().setMessage( status.getMessage(),
                                                           status.getSeverity() == IStatus.ERROR ? IMessageProvider.ERROR
                                                                           : IMessageProvider.WARNING );
                }
            }
        } );
        return status;
    }
}
