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

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.maven.ide.eclipse.ui.common.authentication.RealmUrlCollector;
import org.maven.ide.eclipse.ui.common.composites.ListEditorComposite;
import org.maven.ide.eclipse.ui.common.composites.ListEditorContentProvider;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.maven.ide.eclipse.ui.common.validation.SonatypeValidators;
import org.netbeans.validation.api.Problems;

import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.codebase.composites.SCMLocationComposite;
import com.sonatype.s2.project.ui.codebase.composites.SourceTreeInfoComposite;
import com.sonatype.s2.project.ui.internal.Dialog;
import com.sonatype.s2.project.ui.internal.Images;

public class SourceTreePage
    extends AbstractCodebaseEditorPage
{
    private IS2Module module;

    private SourceTreeInfoComposite sourceTreeInfoComposite;

    private SCMLocationComposite scmLocationComposite;

    private ListEditorComposite<String> rootsComposite;

    private ListEditorComposite<String> profilesComposite;

    private ListEditorComposite<String> feedsComposite;

    // the pattern copied from maven's DefaultModelValidator
    private static final Pattern ID_REGEX = Pattern.compile( "[A-Za-z0-9_\\-.]+" );

    public SourceTreePage( CodebaseDescriptorEditor editor, IS2Module module )
    {
        super( editor, module.getName() );
        this.module = module;
    }

    @Override
    protected void createFormContent( IManagedForm managedForm )
    {
        FormToolkit toolkit = managedForm.getToolkit();
        ScrolledForm form = managedForm.getForm();
        toolkit.decorateFormHeading( form.getForm() );

        Composite body = form.getBody();
        body.setLayout( new GridLayout( 2, true ) );

        Composite left = toolkit.createComposite( body );
        left.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        GridLayout gridLeft = new GridLayout();
        gridLeft.marginWidth = 0;
        gridLeft.marginTop = -gridLeft.marginHeight;
        left.setLayout( gridLeft );

        WidthGroup leftWidthGroup = new WidthGroup();
        left.addControlListener( leftWidthGroup );

        createInfoSection( toolkit, left, leftWidthGroup );
        createScmSection( toolkit, left, leftWidthGroup );

        Composite right = toolkit.createComposite( body );
        right.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        GridLayout gridRight = new GridLayout();
        gridRight.marginWidth = 0;
        gridRight.marginTop = -gridRight.marginHeight;
        right.setLayout( gridRight );
        createRootsSection( toolkit, right );
        createProfilesSection( toolkit, right );

        createFeedsSection( toolkit, body );

        populateToolbar( toolkit, form );

        form.setText( Messages.sourceTreePage_title );
        toolkit.paintBordersFor( body );
        updatePage();
    }

    private void createInfoSection( FormToolkit toolkit, Composite body, WidthGroup widthGroup )
    {
        Section section = toolkit.createSection( body, Section.TITLE_BAR | Section.DESCRIPTION );
        sourceTreeInfoComposite = new SourceTreeInfoComposite( section, widthGroup, getValidationGroup(), toolkit )
        {
            @Override
            protected void saveModuleName( String name )
            {
                super.saveModuleName( name );
                if ( name != null )
                {
                    getCodebaseEditor().updateModule( getModule() );
                }
            }
        };
        sourceTreeInfoComposite.addCodebaseChangeListener( this );
        section.setClient( sourceTreeInfoComposite );
        section.setText( Messages.sourceTreeInfoComposite_title );
        section.setDescription( Messages.sourceTreeInfoComposite_description );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );

        toolkit.adapt( sourceTreeInfoComposite );
        toolkit.paintBordersFor( sourceTreeInfoComposite );
    }

    private void createScmSection( FormToolkit toolkit, Composite body, WidthGroup widthGroup )
    {
        Section section = toolkit.createSection( body, Section.TITLE_BAR | Section.DESCRIPTION );
        scmLocationComposite = new SCMLocationComposite( section, widthGroup, getValidationGroup(), toolkit )
        {
            @Override
            protected void validateScmAccess()
            {
                final Shell shell = getShell();
                Job job = new Job( Messages.scmLocationComposite_validating )
                {
                    @Override
                    protected IStatus run( IProgressMonitor monitor )
                    {
                        return runValidation( shell, monitor );
                    }
                };
                job.setUser( true );
                job.schedule();
            }
        };
        scmLocationComposite.addCodebaseChangeListener( this );
        scmLocationComposite.addRealmChangeListener( this );
        section.setClient( scmLocationComposite );
        section.setText( Messages.scmLocationComposite_title );
        section.setDescription( Messages.scmLocationComposite_description );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );

        toolkit.adapt( scmLocationComposite );
        toolkit.paintBordersFor( scmLocationComposite );
    }

    private void createRootsSection( FormToolkit toolkit, Composite body )
    {
        Section section = toolkit.createSection( body, Section.TITLE_BAR | Section.DESCRIPTION );
        rootsComposite = new ListEditorComposite<String>( section, SWT.NONE );
        section.setClient( rootsComposite );
        section.setText( Messages.projectEditor_modules_roots_title );
        section.setDescription( Messages.projectEditor_modules_roots_description );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );

        rootsComposite.setLabelProvider( new StringLabelProvider( Images.CATALOG_ENTRY ) );
        rootsComposite.setContentProvider( new ListEditorContentProvider<String>() );
        rootsComposite.setAddListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                InputDialog inputDialog =
                    new InputDialog( getSite().getShell(), Messages.projectEditor_modules_roots_newTitle,
                                     Messages.projectEditor_modules_roots_newMessage, "", new IInputValidator()
                                     {
                                         public String isValid( String newText )
                                         {
                                             if ( newText.length() == 0 )
                                             {
                                                 return Messages.projectEditor_errors_rootEmpty;
                                             }
                                             if ( module.getRoots().contains( newText ) )
                                             {
                                                 return Messages.projectEditor_errors_rootExists;
                                             }
                                             return null;
                                         }
                                     } );
                if ( inputDialog.open() == Dialog.OK )
                {
                    module.addRoot( inputDialog.getValue() );
                    rootsComposite.setInput( module.getRoots() );
                    setDirty( true );
                }
            }
        } );
        rootsComposite.setRemoveListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                // MECLIPSE-1047
                rootsComposite.getViewer().cancelEditing();
                List<String> selection = rootsComposite.getSelection();
                for ( String root : selection )
                {
                    module.removeRoot( root );
                }
                rootsComposite.setInput( module.getRoots() );
                setDirty( true );
            }
        } );
        rootsComposite.setCellModifier( new ICellModifier()
        {
            public void modify( Object element, String property, Object value )
            {
                String s = toNull( String.valueOf( value ) );
                int n = rootsComposite.getSelectionIndex();
                List<String> roots = module.getRoots();
                if ( roots.contains( String.valueOf( value ) ) )
                {
                    // duplicate value..
                    return;
                }
                if ( s != null && !s.equals( roots.get( n ) ) )
                {
                    roots.set( n, s );
                    if ( element instanceof Item )
                    {
                        Item item = (Item) element;
                        item.setData( s );
                        item.setText( s );
                    }
                    else
                    {
                        rootsComposite.setInput( roots );
                    }
                    setDirty( true );
                }
            }

            public Object getValue( Object element, String property )
            {
                return element;
            }

            public boolean canModify( Object element, String property )
            {
                return true;
            }
        } );
        toolkit.adapt( rootsComposite );
        toolkit.paintBordersFor( rootsComposite );
    }

    private void createProfilesSection( FormToolkit toolkit, Composite body )
    {
        Section section = toolkit.createSection( body, Section.TITLE_BAR | Section.DESCRIPTION );
        profilesComposite = new ListEditorComposite<String>( section, SWT.NONE );
        section.setClient( profilesComposite );
        section.setText( Messages.projectEditor_modules_profiles_title );
        section.setDescription( Messages.projectEditor_modules_profiles_description );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );

        profilesComposite.setLabelProvider( new StringLabelProvider( Images.CATALOG_ENTRY ) );
        profilesComposite.setContentProvider( new ListEditorContentProvider<String>() );
        profilesComposite.setAddListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                InputDialog inputDialog =
                    new InputDialog( getSite().getShell(), Messages.projectEditor_modules_profiles_newTitle,
                                     Messages.projectEditor_modules_profiles_newMessage, "", new IInputValidator()
                                     {

                                         public String isValid( String newText )
                                         {
                                             if ( newText.length() == 0 )
                                             {
                                                 return Messages.projectEditor_errors_profileEmpty;
                                             }
                                             if ( module.getProfiles().contains( newText ) )
                                             {
                                                 return Messages.projectEditor_errors_profileExists;
                                             }
                                             if ( !ID_REGEX.matcher( newText ).matches() )
                                             {
                                                 return Messages.projectEditor_errors_profileWrongChars;
                                             }
                                             return null;
                                         }
                                     } );
                if ( inputDialog.open() == Dialog.OK )
                {
                    module.addProfile( inputDialog.getValue() );
                    profilesComposite.setInput( module.getProfiles() );
                    setDirty( true );
                }
            }
        } );
        profilesComposite.setRemoveListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                // MECLIPSE-1047
                profilesComposite.getViewer().cancelEditing();

                List<String> selection = profilesComposite.getSelection();
                for ( String profile : selection )
                {
                    module.removeProfile( profile );
                }
                profilesComposite.setInput( module.getProfiles() );
                setDirty( true );
            }
        } );
        profilesComposite.setCellModifier( new ICellModifier()
        {
            public void modify( Object element, String property, Object value )
            {
                String s = toNull( String.valueOf( value ) );
                // MECLIPSE-945
                if ( !ID_REGEX.matcher( s ).matches() )
                {
                    return;
                }
                int n = profilesComposite.getSelectionIndex();
                List<String> profiles = module.getProfiles();
                if ( profiles.contains( String.valueOf( value ) ) )
                {
                    return;
                }

                if ( s != null && !s.equals( profiles.get( n ) ) )
                {
                    profiles.set( n, s );
                    if ( element instanceof Item )
                    {
                        Item item = (Item) element;
                        item.setData( s );
                        item.setText( s );
                    }
                    else
                    {
                        profilesComposite.setInput( profiles );
                    }
                    setDirty( true );
                }
            }

            public Object getValue( Object element, String property )
            {
                return element;
            }

            public boolean canModify( Object element, String property )
            {
                return true;
            }
        } );
        toolkit.adapt( profilesComposite );
        toolkit.paintBordersFor( profilesComposite );
    }

    private void createFeedsSection( FormToolkit toolkit, Composite body )
    {
        Section section = toolkit.createSection( body, Section.TITLE_BAR | Section.DESCRIPTION );
        feedsComposite = new ListEditorComposite<String>( section, SWT.NONE );
        section.setClient( feedsComposite );
        section.setText( Messages.projectEditor_modules_feeds_title );
        section.setDescription( Messages.projectEditor_modules_feeds_description );
        section.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true, 2, 1 ) );

        feedsComposite.setLabelProvider( new StringLabelProvider( Images.FEED ) );
        feedsComposite.setContentProvider( new ListEditorContentProvider<String>() );
        feedsComposite.setAddListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                InputDialog inputDialog =
                    new InputDialog( getSite().getShell(), Messages.projectEditor_modules_feeds_newTitle,
                                     Messages.projectEditor_modules_feeds_newMessage, "", new IInputValidator()
                                     {
                                         public String isValid( String newText )
                                         {
                                        	 Problems problems = new Problems();
                                        	 SonatypeValidators.NON_SCM_URL_MUST_BE_VALID.validate(problems, "Feed URL", newText);
                                        	 if (problems.getLeadProblem() != null) {
                                        		 return problems.getLeadProblem().getMessage();
                                        	 }
                                        	 return null;
                                         }
                                     } );
                if ( inputDialog.open() == Dialog.OK )
                {
                    module.addFeed( inputDialog.getValue() );
                    feedsComposite.setInput( module.getFeeds() );
                    setDirty( true );
                }
            }
        } );
        feedsComposite.setRemoveListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                // MECLIPSE-1047
                feedsComposite.getViewer().cancelEditing();

                List<String> selection = feedsComposite.getSelection();
                for ( String feed : selection )
                {
                    module.removeFeed( feed );
                }
                feedsComposite.setInput( module.getFeeds() );
                setDirty( true );
            }
        } );
        feedsComposite.setCellModifier( new ICellModifier()
        {
            public void modify( Object element, String property, Object value )
            {
                String s = toNull( String.valueOf( value ) );
                int n = feedsComposite.getSelectionIndex();
                List<String> feeds = module.getFeeds();

                if ( s != null && !s.equals( feeds.get( n ) ) )
                {
                    feeds.set( n, s );
                    if ( element instanceof Item )
                    {
                        Item item = (Item) element;
                        item.setData( s );
                        item.setText( s );
                    }
                    else
                    {
                        feedsComposite.setInput( feeds );
                    }
                    setDirty( true );
                }
            }

            public Object getValue( Object element, String property )
            {
                return element;
            }

            public boolean canModify( Object element, String property )
            {
                return true;
            }
        } );
        toolkit.adapt( feedsComposite );
        toolkit.paintBordersFor( feedsComposite );
    }

    public void setModule( IS2Module module )
    {
        this.module = module;
        updatePage();
    }

    @Override
    protected void update()
    {
        IS2Project project = getCodebaseEditor().getProject();
        if ( project == null || module == null || sourceTreeInfoComposite == null )
        {
            return;
        }

        sourceTreeInfoComposite.setModule( project, module );
        scmLocationComposite.setModule( project, module );

        rootsComposite.setInput( module.getRoots() );
        profilesComposite.setInput( module.getProfiles() );
        feedsComposite.setInput( module.getFeeds() );
    }

    private class StringLabelProvider
        extends LabelProvider
    {
        private Image image;

        private StringLabelProvider( Image image )
        {
            this.image = image;
        }

        @Override
        public Image getImage( Object element )
        {
            return image;
        }
    }

    public void saveRealms( RealmUrlCollector realmUrlCollector )
    {
        if ( scmLocationComposite != null )
        {
            scmLocationComposite.saveRealms( realmUrlCollector );
        }
    }
}
