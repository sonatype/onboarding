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
package com.sonatype.s2.project.ui.catalog.view;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputComposite;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IS2ProjectCatalogRegistry;
import com.sonatype.s2.project.core.IS2ProjectCatalogRegistryEntry;
import com.sonatype.s2.project.core.IS2ProjectCatalogRegistryListener;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.model.IS2ProjectCatalogEntry;
import com.sonatype.s2.project.ui.catalog.Messages;
import com.sonatype.s2.project.ui.codebase.editor.CodebaseDescriptorEditor;
import com.sonatype.s2.project.ui.codebase.editor.RemoteCodebaseEditorInput;
import com.sonatype.s2.project.ui.internal.Images;
import com.sonatype.s2.project.ui.internal.views.ProjectCatalogTreeRenderer;
import com.sonatype.s2.project.ui.materialization.actions.MaterializeAction;

public class ProjectCatalogView
    extends ViewPart
    implements IS2ProjectCatalogRegistryListener
{
    private static final Logger log = LoggerFactory.getLogger( ProjectCatalogView.class );

    private static final int PROJECT_SWITCH_DELAY = 300;

    private static final int PROJECT_DOWNLOAD_DELAY = 700;

    protected IS2ProjectCatalogRegistry catalogRegistry;

    protected Composite stackPanel;

    protected StackLayout stackLayout;

    protected Composite emptyPanel;

    protected TreeViewer viewer;

    protected ProjectCatalogContentProvider provider;

    protected ProjectCatalogTreeRenderer renderer;

    private Map<String, RemoteCodebaseEditorInput> projects;

    private AtomicReference<Job> projectJob = new AtomicReference<Job>();

    protected DrillDownAdapter drillDownAdapter;

    private IAction addAction;

    private IAction collapseAllAction;

    private IAction viewAction;

    private IAction editAction;

    private IAction refreshAction;

    private IAction removeAction;

    private IAction materializeAction;

    private IAction copyUrlAction;

    private IAction showCredentialsAction;

    public ProjectCatalogView()
    {
        catalogRegistry = S2ProjectCore.getInstance().getProjectCatalogRegistry();
        catalogRegistry.addListener( this );
        projects = new HashMap<String, RemoteCodebaseEditorInput>();
    }

    @Override
    public void dispose()
    {
        catalogRegistry.removeListener( this );
        super.dispose();
    }

    @Override
    public void createPartControl( Composite parent )
    {
        stackPanel = new Composite( parent, SWT.NONE );
        stackLayout = new StackLayout();
        stackPanel.setLayout( stackLayout );

        createViewer();
        createEmptyPanel();

        createActions();
        createContextMenu();
        populateToolbar();

        switchToViewer();
    }

    private void switchToEmpty()
    {
        stackLayout.topControl = emptyPanel;
        stackPanel.layout();
        updateActions();
    }

    private void switchToViewer()
    {
        stackLayout.topControl = viewer.getControl();
        stackPanel.layout();
        updateActions();
    }

    private void createViewer()
    {
        viewer = new TreeViewer( stackPanel, SWT.FULL_SELECTION | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL );
        provider = new ProjectCatalogContentProvider( catalogRegistry, viewer )
        {
            @Override
            protected void loadCallback( int size )
            {
                if ( size > 0 )
                {
                    switchToViewer();
                    for ( final IS2ProjectCatalogRegistryEntry registryEntry : entries )
                    {
                        IJobChangeListener jobListener = new JobChangeAdapter()
                        {
                            @Override
                            public void done( IJobChangeEvent event )
                            {
                                viewer.getTree().getDisplay().asyncExec( new Runnable()
                                {
                                    public void run()
                                    {
                                        viewer.collapseToLevel( registryEntry, -1 );
                                    }
                                } );
                            }
                        };
                        if ( registryEntry.isLoaded() )
                        {
                            renderer.loadEntries( registryEntry.getCatalog().getEntries() ).addJobChangeListener(
                                                                                                                  jobListener );
                        }
                    }
                }
                else
                {
                    switchToEmpty();
                }
            }
        };
        renderer = new ProjectCatalogTreeRenderer( viewer, null /* projectFilter */)
        {
            @Override
            public String getText( Object obj )
            {
                if ( obj instanceof IS2ProjectCatalogRegistryEntry )
                {
                    IS2ProjectCatalogRegistryEntry entry = (IS2ProjectCatalogRegistryEntry) obj;
                    if ( entry.isLoaded() )
                    {
                        obj = entry.getCatalog();
                    }
                    else
                    {
                        return entry.getUrl();
                    }
                }

                return super.getText( obj );
            }

            @Override
            protected void paintItem( Event event )
            {
                if ( event.item.getData() instanceof IS2ProjectCatalogRegistryEntry )
                {
                    IS2ProjectCatalogRegistryEntry catalogEntry = (IS2ProjectCatalogRegistryEntry) event.item.getData();
                    Image image = Images.DEFAULT_FOLDER_IMAGE;
                    String text = catalogEntry.isLoaded() ? catalogEntry.getCatalog().getName() : Messages.loading;
                    String description =
                        catalogEntry.isLoaded() ? NLS.bind( Messages.catalogView_numberOfEntries,
                                                            catalogEntry.getCatalog().getEntries().size() )
                                        : catalogEntry.getUrl();

                    drawItem( event, image, text, description );
                }
                else
                {
                    super.paintItem( event );
                }
            }
        };
        viewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                updateActions();
                showProjectOverview( false );
            }
        } );
        viewer.addDoubleClickListener( new IDoubleClickListener()
        {
            public void doubleClick( DoubleClickEvent event )
            {
                Object selection = ( (IStructuredSelection) event.getSelection() ).getFirstElement();
                if ( selection instanceof IS2ProjectCatalogRegistryEntry )
                {
                    viewer.expandToLevel( selection, 1 );
                }
                else if ( selection instanceof IS2ProjectCatalogEntry )
                {
                    showProjectOverview( false );
                }
            }
        } );

        viewer.setInput( getViewSite() );
        drillDownAdapter = new DrillDownAdapter( viewer );
    }

    private void createEmptyPanel()
    {
        FormToolkit toolkit = new FormToolkit( stackPanel.getDisplay() );

        emptyPanel = toolkit.createComposite( stackPanel );
        emptyPanel.setLayout( new GridLayout() );
        toolkit.createLabel( emptyPanel, Messages.catalogView_empty_message );
        HyperlinkAdapter hyperlinkAdapter = new HyperlinkAdapter()
        {
            @Override
            public void linkActivated( HyperlinkEvent e )
            {
                addAction.run();
            }
        };
        toolkit.createHyperlink( emptyPanel, Messages.catalogView_empty_addNew, SWT.NONE ).addHyperlinkListener(
                                                                                                                 hyperlinkAdapter );
        if ( catalogRegistry.hasDefaultCatalogs() )
        {
            HyperlinkAdapter hyperlink = new HyperlinkAdapter()
            {
                @Override
                public void linkActivated( HyperlinkEvent e )
                {
                    catalogRegistry.addDefaultCatalogs();
                }
            };
            toolkit.createHyperlink( emptyPanel, Messages.catalogView_empty_loadDefault, SWT.NONE ).addHyperlinkListener(
                                                                                                                          hyperlink );
        }

        new FormToolkit( stackPanel.getDisplay() ).adapt( emptyPanel );
    }

    @Override
    public void setFocus()
    {
        viewer.getControl().setFocus();
    }

    private void createActions()
    {
        addAction = new Action( Messages.catalogView_actions_add_title )
        {
            public void run()
            {
                UrlInputDialog dlg =
                    new UrlInputDialog( getSite().getShell(), Messages.catalogView_actions_add_dialogTitle,
                                        Messages.catalogContentProvider_catalogUrlLabel );
                if ( dlg.open() == Window.OK )
                {
                    catalogRegistry.addCatalog( dlg.getUrl() );
                }
            }
        };
        addAction.setToolTipText( Messages.catalogView_actions_add_tooltip );
        addAction.setImageDescriptor( Images.ADD_CATALOG_DESCRIPTOR );

        collapseAllAction = new Action( Messages.actions_collapseAll_title )
        {
            public void run()
            {
                viewer.collapseAll();
            }
        };
        collapseAllAction.setToolTipText( Messages.actions_collapseAll_tooltip );
        collapseAllAction.setImageDescriptor( PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                                                                                                              ISharedImages.IMG_ELCL_COLLAPSEALL ) );
        viewAction = new Action( Messages.catalogView_actions_viewProject_title )
        {
            public void run()
            {
                showProjectOverview( false );
            }
        };
        viewAction.setToolTipText( Messages.catalogView_actions_viewProject_tooltip );

        editAction = new Action( Messages.catalogView_actions_editProject_title )
        {
            public void run()
            {
                showProjectOverview( true );
            }
        };
        editAction.setToolTipText( Messages.catalogView_actions_editProject_tooltip );

        refreshAction = new Action( Messages.catalogView_actions_reload_title )
        {
            public void run()
            {
                reloadView();
            }
        };
        refreshAction.setToolTipText( Messages.catalogView_actions_reload_tooltip );
        refreshAction.setImageDescriptor( Images.REFRESH_DESCRIPTOR );

        removeAction = new Action( Messages.catalogView_actions_remove_title )
        {
            public void run()
            {
                Object selection = ( (IStructuredSelection) viewer.getSelection() ).getFirstElement();
                if ( selection instanceof IS2ProjectCatalogRegistryEntry )
                {
                    if ( MessageDialog.openConfirm( getSite().getShell(),
                                                    Messages.catalogView_actions_remove_dialogTitle,
                                                    Messages.catalogView_actions_remove_message ) )
                    {
                        catalogRegistry.removeCatalog( ( (IS2ProjectCatalogRegistryEntry) selection ).getUrl() );
                    }
                }
            }
        };
        removeAction.setToolTipText( Messages.catalogView_actions_remove_tooltip );
        removeAction.setImageDescriptor( PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                                                                                                         ISharedImages.IMG_ELCL_REMOVE ) );

        materializeAction = new MaterializeAction()
        {
            public void run()
            {
                Object selection = ( (IStructuredSelection) viewer.getSelection() ).getFirstElement();
                if ( selection instanceof IS2ProjectCatalogEntry )
                {
                    setCatalogEntry( (IS2ProjectCatalogEntry) selection );
                    super.run();
                }
            }
        };

        copyUrlAction = new Action( Messages.catalogView_actions_copyUrl_title )
        {
            public void run()
            {
                Object selection = ( (IStructuredSelection) viewer.getSelection() ).getFirstElement();
                if ( selection != null && selection instanceof IS2ProjectCatalogEntry )
                {
                    IS2ProjectCatalogEntry catalogEntry = (IS2ProjectCatalogEntry) selection;
                    String url;
                    try
                    {
                        url = catalogRegistry.getEffectiveDescriptorUrl( catalogEntry );
                        Clipboard clipboard = new Clipboard( getSite().getShell().getDisplay() );
                        clipboard.setContents( new Object[] { url }, new TextTransfer[] { TextTransfer.getInstance() } );
                    }
                    catch ( CoreException e )
                    {
                        log.error( Messages.catalogView_actions_copyUrl_error, e );
                    }
                }
            }
        };
        copyUrlAction.setToolTipText( Messages.catalogView_actions_copyUrl_tooltip );
        copyUrlAction.setImageDescriptor( PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                                                                                                          ISharedImages.IMG_TOOL_COPY ) );

        showCredentialsAction = new Action( Messages.catalogView_actions_editCredentials_title )
        {
            public void run()
            {
                Object selection = ( (IStructuredSelection) viewer.getSelection() ).getFirstElement();
                if ( selection instanceof IS2ProjectCatalogRegistryEntry )
                {
                    IS2ProjectCatalogRegistryEntry registryEntry = (IS2ProjectCatalogRegistryEntry) selection;
                    new UrlInputDialog( getSite().getShell(), Messages.catalogView_actions_editCredentials_dialogTitle,
                                        Messages.catalogContentProvider_catalogUrlLabel, registryEntry.getUrl() ).open();
                }
            }
        };
        showCredentialsAction.setToolTipText( Messages.catalogView_actions_editCredentials_tooltip );
        showCredentialsAction.setImageDescriptor( Images.LOCK_DESCRIPTOR );
    }

    private void createContextMenu()
    {
        MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown( true );
        menuMgr.addMenuListener( new IMenuListener()
        {
            public void menuAboutToShow( IMenuManager manager )
            {
                stopProjectJob();
                populateContextMenu( manager );
            }
        } );

        Menu menu = menuMgr.createContextMenu( viewer.getControl() );
        viewer.getControl().setMenu( menu );
        getSite().registerContextMenu( menuMgr, viewer );
    }

    protected void populateContextMenu( IMenuManager menuManager )
    {
        Object selection = ( (IStructuredSelection) viewer.getSelection() ).getFirstElement();
        if ( selection instanceof IS2ProjectCatalogEntry )
        {
            menuManager.add( viewAction );
            // menuManager.add( editAction );
            menuManager.add( new Separator() );
        }
        menuManager.add( addAction );
        if ( selection instanceof IS2ProjectCatalogRegistryEntry )
        {
            menuManager.add( removeAction );
            menuManager.add( showCredentialsAction );
        }
        menuManager.add( new Separator() );
        if ( selection instanceof IS2ProjectCatalogEntry )
        {
            menuManager.add( materializeAction );
            menuManager.add( copyUrlAction );
            menuManager.add( new Separator() );
        }
        menuManager.add( collapseAllAction );
        menuManager.add( refreshAction );
        menuManager.add( new Separator() );
        drillDownAdapter.addNavigationActions( menuManager );
        menuManager.add( new Separator( IWorkbenchActionConstants.MB_ADDITIONS ) );
    }

    private void populateToolbar()
    {
        IActionBars bars = getViewSite().getActionBars();
        IMenuManager menuManager = bars.getMenuManager();
        menuManager.add( collapseAllAction );
        menuManager.add( refreshAction );

        IToolBarManager toolbarManager = bars.getToolBarManager();
        toolbarManager.add( new Separator() );
        toolbarManager.add( addAction );
        toolbarManager.add( removeAction );
        toolbarManager.add( showCredentialsAction );
        toolbarManager.add( new Separator() );
        toolbarManager.add( collapseAllAction );
        toolbarManager.add( refreshAction );
        toolbarManager.add( new Separator() );
        drillDownAdapter.addNavigationActions( toolbarManager );
    }

    public void catalogAdded( final IS2ProjectCatalogRegistryEntry entry )
    {
        refreshView();
        if ( !entry.isLoaded() )
        {
            if ( entry.getStatus().getException() instanceof IOException )
            {
                catalogRegistry.removeCatalog( entry.getUrl() );

                PlatformUI.getWorkbench().getDisplay().asyncExec( new Runnable()
                {
                    public void run()
                    {
                        UrlInputDialog dlg =
                            new UrlInputDialog( getSite().getShell(), Messages.catalogView_actions_add_error,
                                                Messages.catalogContentProvider_catalogUrlLabel, entry.getUrl(),
                                                UrlInputComposite.ALLOW_ANONYMOUS );
                        dlg.setErrorText( entry.getStatus().getException().getMessage() );

                        if ( dlg.open() == Window.OK )
                        {
                            catalogRegistry.addCatalog( dlg.getUrl() );
                        }
                    }
                } );
            }
            else
            {
                StatusManager.getManager().handle( entry.getStatus(), StatusManager.SHOW | StatusManager.LOG );
            }
        }
    }

    public void catalogRemoved( IS2ProjectCatalogRegistryEntry entry )
    {
        refreshView();
    }

    protected void refreshView()
    {
        PlatformUI.getWorkbench().getDisplay().syncExec( new Runnable()
        {
            public void run()
            {
                renderer.clear();
                provider.refresh();
                viewer.refresh();
            }
        } );
    }

    protected void reloadView()
    {
        PlatformUI.getWorkbench().getDisplay().syncExec( new Runnable()
        {
            public void run()
            {
                catalogRegistry.purge();
                projects.clear();
                renderer.clear();
                provider.reset();
                viewer.refresh();
            }
        } );
    }

    protected void showProjectOverview( final boolean edit )
    {
        stopProjectJob();

        Object selection = ( (IStructuredSelection) viewer.getSelection() ).getFirstElement();
        if ( selection != null && selection instanceof IS2ProjectCatalogEntry )
        {
            final IS2ProjectCatalogEntry catalogEntry = (IS2ProjectCatalogEntry) selection;
            final String url = catalogEntry.getEffectiveDescriptorUrl();

            final RemoteCodebaseEditorInput input = projects.get( url );
            if ( input == null )
            {
                loadProjectDescriptor( catalogEntry, edit );
            }
            else
            {
                Job job =
                    new UIJob( NLS.bind( Messages.catalogView_jobs_loadingProjectDetails, catalogEntry.getName() ) )
                    {
                        @Override
                        public IStatus runInUIThread( IProgressMonitor monitor )
                        {
                            return openProjectEditor( input, edit );
                        }
                    };
                job.schedule( PROJECT_SWITCH_DELAY );
                projectJob.set( job );
            }
        }
    }

    private void stopProjectJob()
    {
        Job job = projectJob.getAndSet( null );
        if ( job != null )
        {
            job.cancel();
        }
    }

    private void loadProjectDescriptor( final IS2ProjectCatalogEntry catalogEntry, final boolean edit )
    {
        final String url = catalogEntry.getEffectiveDescriptorUrl();
        Job job = new Job( NLS.bind( Messages.catalogView_jobs_loadingProjectDetails, catalogEntry.getName() ) )
        {
            @Override
            public IStatus run( IProgressMonitor monitor )
            {
                try
                {
                    final RemoteCodebaseEditorInput input =
                        new RemoteCodebaseEditorInput( renderer.getProjectData( catalogEntry ) );
                    input.load( monitor );

                    final IStatus[] status = new IStatus[] { Status.OK_STATUS };
                    PlatformUI.getWorkbench().getDisplay().syncExec( new Runnable()
                    {
                        public void run()
                        {
                            projects.put( url, input );
                            status[0] = openProjectEditor( input, edit );
                        }
                    } );
                    return status[0];
                }
                catch ( CoreException e )
                {
                    return e.getStatus();
                }
                finally
                {
                    projectJob.compareAndSet( this, null );
                }
            }
        };
        job.schedule( PROJECT_DOWNLOAD_DELAY );
        projectJob.set( job );
    }

    private IStatus openProjectEditor( IEditorInput input, boolean edit )
    {
        if ( edit ) {
            CodebaseDescriptorEditor.openEditor( input );
        }
        else {
            ProjectDescriptorViewer.openEditor( input );
        }

        setFocus();
        return Status.OK_STATUS;
    }

    private void updateActions()
    {
        boolean enable = stackLayout.topControl != emptyPanel;
        Object selection = ( (IStructuredSelection) viewer.getSelection() ).getFirstElement();

        materializeAction.setEnabled( enable && selection instanceof IS2ProjectCatalogEntry );
        removeAction.setEnabled( enable && selection instanceof IS2ProjectCatalogRegistryEntry );
        showCredentialsAction.setEnabled( enable && selection instanceof IS2ProjectCatalogRegistryEntry );
        collapseAllAction.setEnabled( enable );
        refreshAction.setEnabled( enable );
    }
}
