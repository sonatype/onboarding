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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.ui.ValidationListener;
import org.netbeans.validation.api.ui.ValidationUI;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.codebase.composites.CodebaseGAVComposite;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;

public class LineupToCodebasePage
    extends WizardPage
{
    private SwtValidationGroup validationGroup;

    private WidthGroup widthGroup;

    private Button projectButton;

    private Button publishButton;

    private Button codebaseButton;

    private CodebaseGAVComposite gavComposite;

    private NexusLineupPublishingInfo info;

    private IS2Project project;

    protected LineupToCodebasePage( NexusLineupPublishingInfo info, IS2Project project )
    {
        super( LineupToCodebasePage.class.getName() );
        this.info = info;
        this.project = project;

        validationGroup = SwtValidationGroup.create( SwtValidationUI.createUI( this ) );
        widthGroup = new WidthGroup();

        setDescription( Messages.lineupToCodebasePage_description );
        setTitle( Messages.lineupToCodebasePage_title );
    }

    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayout( new GridLayout() );
        composite.addControlListener( widthGroup );

        SelectionListener selectionListener = new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                update();
            }
        };

        projectButton = new Button( composite, SWT.CHECK );
        projectButton.setText( Messages.lineupToCodebasePage_createLocalProject );
        projectButton.addSelectionListener( selectionListener );

        publishButton = new Button( composite, SWT.CHECK );
        publishButton.setText( Messages.lineupToCodebasePage_publish );
        publishButton.setLayoutData( new GridData( SWT.FILL, SWT.TOP, false, false ) );
        publishButton.setSelection( true );
        publishButton.addSelectionListener( selectionListener );

        validationGroup.addItem( new CheckboxValidationListener(), false );

        codebaseButton = new Button( composite, SWT.CHECK );
        codebaseButton.setText( Messages.lineupToCodebasePage_createCodebase );
        codebaseButton.setSelection( true );
        GridData codebaseData = new GridData( SWT.LEFT, SWT.TOP, false, false );
        codebaseData.horizontalIndent = 10;
        codebaseButton.setLayoutData( codebaseData );
        codebaseButton.addSelectionListener( selectionListener );

        gavComposite = new CodebaseGAVComposite( composite, widthGroup, validationGroup, null );
        GridData gavData = new GridData( SWT.FILL, SWT.TOP, true, false );
        gavData.horizontalIndent = 20;
        gavData.verticalAlignment = 10;
        gavComposite.setLayoutData( gavData );
        gavComposite.setProject( project );

        setControl( composite );
    }

    private void update()
    {
        codebaseButton.setEnabled( publishButton.getSelection() );

        boolean b = isCodebaseNeeded();
        gavComposite.setEnabled( b );
        gavComposite.setVisible( b );
        validationGroup.performValidation();

        updateTitle();
        getContainer().updateButtons();
    }

    private void updateTitle()
    {
        ( (Wizard) getWizard() ).setWindowTitle( isCodebaseNeeded() ? Messages.newCodebaseWizard_title : "" );
    }

    @Override
    public void setVisible( boolean visible )
    {
        super.setVisible( visible );
        if ( visible )
        {
            publishButton.setText( NLS.bind( Messages.lineupToCodebasePage_publishTo, info.getServerUrl() ) );
            if ( project.getGroupId() == null )
            {
                project.setGroupId( info.getLineup().getGroupId() );
                project.setVersion( info.getLineup().getVersion() );
                gavComposite.update();
            }
            updateTitle();
        }
    }

    protected boolean isLocal()
    {
        return projectButton.getSelection();
    }

    protected boolean isPublishingNeeded()
    {
        return publishButton.getSelection();
    }

    protected boolean isCodebaseNeeded()
    {
        return publishButton.getSelection() && codebaseButton.getSelection();
    }

    @Override
    public IWizardPage getNextPage()
    {
        return isCodebaseNeeded() ? super.getNextPage() : null;
    }

    private class CheckboxValidationListener
        extends ValidationListener<LineupToCodebasePage>
        implements SelectionListener
    {
        protected CheckboxValidationListener()
        {
            super( LineupToCodebasePage.class, ValidationUI.NO_OP, LineupToCodebasePage.this );
            projectButton.addSelectionListener( this );
            publishButton.addSelectionListener( this );
        }

        @Override
        protected void performValidation( Problems problems )
        {
            if ( !( projectButton.getSelection() || publishButton.getSelection() ) )
            {
                problems.add( Messages.lineupToCodebasePage_noActionSelected, Severity.FATAL );
            }
        }

        public void widgetSelected( SelectionEvent e )
        {
            performValidation();
        }

        public void widgetDefaultSelected( SelectionEvent e )
        {
        }
    }
}
