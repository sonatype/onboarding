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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.model.IS2ProjectCatalog;
import com.sonatype.s2.project.model.IS2ProjectCatalogEntry;
import com.sonatype.s2.project.model.IS2ProjectFilter;
import com.sonatype.s2.project.model.S2ProjectFacade;
import com.sonatype.s2.project.ui.internal.Messages;
import com.sonatype.s2.project.ui.internal.views.ProjectCatalogTreeRenderer;

public class ProjectSelectorPage
    extends WizardPage
{
    private static Logger log = LoggerFactory.getLogger( ProjectSelectorPage.class );

    private TreeViewer viewer;

    private ProjectCatalogTreeRenderer renderer;

    public String catalogUrl;

    private IS2ProjectFilter projectFilter;

    public ProjectSelectorPage( String catalogUrl, IS2ProjectFilter projectFilter )
    {
        super( Messages.installationWizard_projectPage_title );
        this.projectFilter = projectFilter;
        setTitle( Messages.installationWizard_projectPage_title );
        setDescription( Messages.installationWizard_projectPage_description );
        setPageComplete( false );

        this.catalogUrl = catalogUrl;
    }

    public void createControl( Composite parent )
    {
        Composite container = new Composite( parent, SWT.NULL );
        container.setLayout( new GridLayout( 1, false ) );

        viewer = new TreeViewer( container, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE );
        GridData gd = new GridData( SWT.FILL, SWT.FILL, true, true );
        gd.heightHint = 200;
        gd.widthHint = 600;
        viewer.getTree().setLayoutData( gd );
        renderer = new ProjectCatalogTreeRenderer( viewer, projectFilter );
        viewer.setContentProvider( new ITreeContentProvider()
        {
            private IS2ProjectCatalog catalog = null;

            private boolean loadError = false;

            public Object[] getElements( Object inputElement )
            {
                if ( catalog == null && !loadError )
                {
                    final Exception[] exception = new Exception[] { null };
                    try
                    {
                        getContainer().run( true, true, new IRunnableWithProgress()
                        {
                            public void run( IProgressMonitor monitor )
                                throws InvocationTargetException, InterruptedException
                            {
                                String urlStr = S2ProjectFacade.getCatalogFileUrl( catalogUrl );

                                try
                                {
                                    InputStream is = S2IOFacade.openStream( urlStr, monitor );
                                    try
                                    {
                                        final IS2ProjectCatalog cat = S2ProjectFacade.loadProjectCatalog( is );
                                        S2ProjectFacade.applyCatalogUrl( cat, catalogUrl );

                                        IJobChangeListener jobListener = new JobChangeAdapter()
                                        {
                                            @Override
                                            public void done( IJobChangeEvent event )
                                            {
                                                viewer.getTree().getDisplay().asyncExec( new Runnable()
                                                {
                                                    public void run()
                                                    {
                                                        catalog = cat;
                                                        viewer.refresh();
                                                    }
                                                } );
                                            }
                                        };
                                        renderer.loadEntries( cat.getEntries() ).addJobChangeListener( jobListener );
                                    }
                                    finally
                                    {
                                        IOUtil.close( is );
                                    }
                                }
                                catch ( IOException e )
                                {
                                    exception[0] = e;
                                }
                                catch ( URISyntaxException e )
                                {
                                    exception[0] = e;
                                }
                            }
                        } );
                    }
                    catch ( InvocationTargetException e )
                    {
                        exception[0] = e;
                    }
                    catch ( InterruptedException e )
                    {
                        exception[0] = e;
                    }
                    if ( exception[0] != null )
                    {
                        String message =
                            NLS.bind( Messages.materializationWizard_errors_errorLoadingCatalog,
                                      exception[0].getMessage() );
                        log.error( message, exception[0] );
                        setErrorMessage( message );
                        loadError = true;
                    }
                }
                if ( catalog != null )
                {
                    List<IS2ProjectCatalogEntry> result = new ArrayList<IS2ProjectCatalogEntry>();
                    for ( IS2ProjectCatalogEntry entry : catalog.getEntries() )
                    {
                        if ( renderer.getProjectData( entry ) != null )
                        {
                            result.add( entry );
                        }
                    }
                    return result.toArray( new IS2ProjectCatalogEntry[0] );
                }
                else
                {
                    return new Object[] { Messages.loading };
                }
            }

            public boolean hasChildren( Object element )
            {
                return false;
            }

            public Object getParent( Object element )
            {
                return null;
            }

            public Object[] getChildren( Object parentElement )
            {
                return null;
            }

            public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
            {
            }

            public void dispose()
            {
            }

        } );
        viewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                setPageComplete( ( (IStructuredSelection) event.getSelection() ).getFirstElement() instanceof IS2ProjectCatalogEntry );
            }
        } );
        viewer.addDoubleClickListener( new IDoubleClickListener()
        {
            public void doubleClick( DoubleClickEvent event )
            {
                Object selection = ( (IStructuredSelection) event.getSelection() ).getFirstElement();
                viewer.expandToLevel( selection, 1 );
            }
        } );
        viewer.setInput( this );

        setControl( container );
    }

    @Override
    public IWizardPage getNextPage()
    {
        Object selection = ( (IStructuredSelection) viewer.getSelection() ).getFirstElement();
        if ( selection instanceof IS2ProjectCatalogEntry )
        {
            try
            {
                ( (AbstractMaterializationWizard) getWizard() ).setProject( renderer.getProjectData( (IS2ProjectCatalogEntry) selection ) );
                return super.getNextPage();
            }
            catch ( CoreException e )
            {
                setErrorMessage( e.getMessage() );
            }
        }

        return null;
    }

    @Override
    public boolean canFlipToNextPage()
    {
        // changed the default to not invoke getNextPage() here
        return isPageComplete();
    }
}
