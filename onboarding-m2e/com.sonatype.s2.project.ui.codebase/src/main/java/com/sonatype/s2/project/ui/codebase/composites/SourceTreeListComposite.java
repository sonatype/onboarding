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

import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.composites.ListEditorComposite;
import org.maven.ide.eclipse.ui.common.composites.ListEditorContentProvider;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;

import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.descriptor.S2Module;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.codebase.wizard.SourceTreeWizard;
import com.sonatype.s2.project.ui.internal.Dialog;
import com.sonatype.s2.project.ui.internal.Images;

@SuppressWarnings( "restriction" )
abstract public class SourceTreeListComposite
    extends CodebaseComposite
{
    private ListEditorComposite<IS2Module> modulesComposite;

    public SourceTreeListComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                                    FormToolkit toolkit )
    {
        super( parent, widthGroup, validationGroup, toolkit );

        GridLayout gridLayout = new GridLayout();
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.verticalSpacing = 0;
        setLayout( gridLayout );

        createModuleControls();
    }

    private void createModuleControls()
    {
        modulesComposite =
            new ListEditorComposite<IS2Module>( this, SWT.NONE, ListEditorComposite.ADD | ListEditorComposite.EDIT
                | ListEditorComposite.REMOVE | ( isFormMode() ? ListEditorComposite.FORM : 0 ) );
        modulesComposite.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );

        modulesComposite.setLabelProvider( new LabelProvider()
        {
            public String getText( Object element )
            {
                if ( element instanceof IS2Module )
                {
                    return ( (IS2Module) element ).getName();
                }
                return super.getText( element );
            };

            @Override
            public Image getImage( Object element )
            {
                if ( element instanceof IS2Module )
                {
                    return Images.SOURCE_TREE;
                }
                return super.getImage( element );
            }
        } );
        modulesComposite.setContentProvider( new ListEditorContentProvider<IS2Module>() );
        modulesComposite.setDoubleClickListener( new IDoubleClickListener()
        {
            public void doubleClick( DoubleClickEvent event )
            {
                editModule( modulesComposite.getSelectionIndex() );
            }
        } );
        modulesComposite.setEditListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                editModule( modulesComposite.getSelectionIndex() );
            }
        } );
        modulesComposite.setAddListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                IS2Module module = new S2Module();
                SourceTreeWizard sourceTreeWizard = new SourceTreeWizard( getProject(), module );
                sourceTreeWizard.setWindowTitle( Messages.sourceTreeListComposite_add_title );

                WizardDialog wizardDialog = new WizardDialog( getShell(), sourceTreeWizard );
                if ( wizardDialog.open() == Dialog.OK )
                {
                    addModule( module );
                    notifyCodebaseChangeListeners();
                }
            }
        } );
        modulesComposite.setRemoveListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                List<IS2Module> selection = modulesComposite.getSelection();
                if ( MessageDialog.openConfirm(
                                                getShell(),
                                                Messages.sourceTreeListComposite_delete_title,
                                                selection.size() == 1 ? NLS.bind(
                                                                                  Messages.sourceTreeListComposite_delete_message,
                                                                                  selection.get( 0 ).getName() )
                                                                : Messages.sourceTreeListComposite_delete_message2 ) )
                {
                    for ( IS2Module module : selection )
                    {
                        removeModule( module );
                    }
                    notifyCodebaseChangeListeners();
                }
            }
        } );
    }

    abstract protected void addModule( IS2Module module );

    abstract protected void editModule( int index );

    abstract protected void removeModule( IS2Module module );

    @Override
    protected void update( IS2Project project )
    {
        modulesComposite.setInput( project.getModules() );
    }

    public TableViewer getViewer()
    {
        return modulesComposite.getViewer();
    }
}
