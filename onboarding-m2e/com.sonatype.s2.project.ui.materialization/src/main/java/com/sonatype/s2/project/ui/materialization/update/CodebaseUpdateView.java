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
package com.sonatype.s2.project.ui.materialization.update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ViewPluginAction;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IS2CodebaseChangeEventListener;
import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.S2CodebaseChangeEvent;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.internal.update.AbstractSourceTreeOperation;
import com.sonatype.s2.project.core.internal.update.DetermineCodebaseUpdateStatusOperation;
import com.sonatype.s2.project.ui.materialization.Activator;
import com.sonatype.s2.project.ui.materialization.ITeamProviderUI;
import com.sonatype.s2.project.ui.materialization.Images;
import com.sonatype.s2.project.ui.materialization.Messages;
import com.sonatype.s2.project.ui.materialization.update.spi.CodebaseViewNodeProvider;
import com.sonatype.s2.project.ui.materialization.wizards.S2ProjectMaterializationWizard;

@SuppressWarnings( "restriction" )
public class CodebaseUpdateView
    extends ViewPart
    implements IS2CodebaseChangeEventListener
{
    protected static final Logger log = LoggerFactory.getLogger( CodebaseUpdateView.class );

    public static final String ID = "com.sonatype.s2.project.ui.materialization.update.CodebaseUpdateView"; //$NON-NLS-1$

    public static final String EXTENSION_POINT_ID =
        "com.sonatype.s2.project.ui.materialization.update.CodebaseUpdateViewAction"; //$NON-NLS-1$

    private static final String ELEMENT_ACTION = "actionContribution"; //$NON-NLS-1$

    private static final String ELEMENT_ICON = "icon"; //$NON-NLS-1$

    private static final String ELEMENT_LABEL = "label"; //$NON-NLS-1$

    private static final String ELEMENT_TOOLTIP = "tooltip"; //$NON-NLS-1$

    private IWorkspaceCodebase codebase;

    private Composite stackParent;

    private StackLayout stackLayout;

    private Composite noCodebaseComposite;

    private TreeViewer viewer;

    private Tree tree;

    private List<IAction> contributedActions;

    private Action refreshAction;

    private Action updateAllAction;

    private Action updateAction;

    private Action synchronizeAction;

    private Action helpAction;

    private Action copyUrlAction;

    private boolean disposed;

    private ListenerList listeners;
    
    private boolean showStatus = false;

    public CodebaseUpdateView()
    {
        this.listeners = new ListenerList();
    }

    @Override
    public void dispose()
    {
        super.dispose();
        S2ProjectCore.getInstance().removeWorkspaceCodebaseChangeListener( this );
        disposed = true;
    }

    /**
     * Create contents of the view part.
     * 
     * @param parent
     */
    @Override
    public void createPartControl( Composite parent )
    {
        stackLayout = new StackLayout();
        stackParent = parent;
        stackParent.setLayout( stackLayout );

        createViewer();
        createNoCodebaseComposite();

        createActions();
        initializeToolBar();
        initializeMenu();

        update();
        S2ProjectCore.getInstance().addWorkspaceCodebaseChangeListener( this );
    }

    private void createViewer()
    {
        viewer = new TreeViewer( stackParent, SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL );
        tree = viewer.getTree();
        //
        // tree.addControlListener( new ControlAdapter()
        // {
        // @Override
        // public void controlResized( ControlEvent e )
        // {
        // int width = tree.getSize().x / 2;
        // nameColumn.getColumn().setWidth( width );
        // statusColumn.getColumn().setWidth( width );
        // // urlColumn.getColumn().setWidth( width );
        // }
        // } );

        viewer.setContentProvider( new ITreeContentProvider()
        {
            public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
            {
            }

            public void dispose()
            {
            }

            public boolean hasChildren( Object element )
            {
                return element instanceof CodebaseViewNode && ( (CodebaseViewNode) element ).hasChildren();
            }

            public Object getParent( Object element )
            {
                return null;
            }

            public Object[] getElements( Object inputElement )
            {
                if ( inputElement instanceof IWorkspaceCodebase )
                {
                    return createContent( (IWorkspaceCodebase) inputElement );
                }
                return null;
            }

            public Object[] getChildren( Object parentElement )
            {
                if ( parentElement instanceof CodebaseViewNode )
                {
                    return ( (CodebaseViewNode) parentElement ).getChildren();
                }
                return null;
            }
        } );

        viewer.setLabelProvider( new Prov() );

        viewer.addDoubleClickListener( new IDoubleClickListener()
        {
            public void doubleClick( DoubleClickEvent event )
            {
                helpAction.run();
            }
        } );

        tree.addMouseTrackListener( new MouseTrackAdapter()
        {
            public void mouseHover( MouseEvent e )
            {
                TreeItem ti = tree.getItem( new Point( e.x, e.y ) );
                if ( ti != null )
                {
                    CodebaseViewNode node = (CodebaseViewNode) ti.getData();
                    String tooltip = node.getTooltip();
                    if ( tooltip != null && tooltip.length() > 0  && showStatus)
                    {
                        tree.setToolTipText( tooltip );
                        return;
                    }
                }
                tree.setToolTipText( null );
            }
        } );
    }

    private void createNoCodebaseComposite()
    {
        noCodebaseComposite = new Composite( stackParent, SWT.NONE );
        GridLayout gridLayout = new GridLayout();
        gridLayout.marginHeight = 10;
        gridLayout.marginWidth = 10;
        noCodebaseComposite.setLayout( gridLayout );

        Color backgroundColor = noCodebaseComposite.getDisplay().getSystemColor( SWT.COLOR_LIST_BACKGROUND );
        noCodebaseComposite.setBackground( backgroundColor );

        Label label = new Label( noCodebaseComposite, SWT.WRAP );
        label.setBackground( backgroundColor );
        label.setText( Messages.codebaseUpdateView_noCodebasesFound );
        GridData gd = new GridData( SWT.FILL, SWT.TOP, true, false );
        gd.widthHint = 10;
        label.setLayoutData( gd );

        Link link = new Link( noCodebaseComposite, SWT.NONE );
        link.setBackground( backgroundColor );
        link.setText( NLS.bind( Messages.codebaseUpdateView_linkTemplate, Messages.codebaseUpdateView_materializeNow ) );

        link.addSelectionListener( new SelectionAdapter()
        {
            public void widgetSelected( SelectionEvent e )
            {
                IWorkbenchPartSite site = getSite();
                IWorkbenchWindow window = site.getWorkbenchWindow();
                S2ProjectMaterializationWizard wizard = new S2ProjectMaterializationWizard();
                WizardDialog dialog = new WizardDialog( window.getShell(), wizard );
                dialog.open();
            }
        } );

    }

    private void update()
    {
        List<IWorkspaceCodebase> codebases = S2ProjectCore.getInstance().getWorkspaceCodebases();

        if ( codebases.size() > 0 )
        {
            stackLayout.topControl = viewer.getControl();
            viewer.setInput( codebases.get( 0 ) );
            viewer.expandAll();
        }
        else
        {
            stackLayout.topControl = noCodebaseComposite;

        }
        stackParent.layout();

    }

    /**
     * Create the actions.
     */
    private void createActions()
    {
        loadActionExtensions( EXTENSION_POINT_ID );

        refreshAction = new Action( Messages.codebaseUpdateView_checkAction_title )
        {
            @Override
            public void run()
            {
                doRefresh();
            }
        };
        refreshAction.setImageDescriptor( Images.CHECK_FOR_UPDATES );
        refreshAction.setToolTipText( Messages.codebaseUpdateView_checkAction_tooltip );

        updateAllAction = new Action( Messages.codebaseUpdateView_updateAllAction_title )
        {
            @Override
            public void run()
            {
                new CodebaseUpdateJob( getWorkspaceCodebase() ).schedule();
            }
        };
        updateAllAction.setImageDescriptor( Images.UPDATE_DESCRIPTOR );
        updateAllAction.setToolTipText( Messages.codebaseUpdateView_updateAllAction_tooltip );
        updateAllAction.setEnabled(false);

        updateAction = new Action( Messages.codebaseUpdateView_updateAction_title )
        {
            @Override
            public void run()
            {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                List<CodebaseViewNode> completedNodes = new ArrayList<CodebaseViewNode>();
                for ( Object o : selection.toList() )
                {
                    if ( o instanceof CodebaseViewNode )
                    {
                        CodebaseViewNode node = (CodebaseViewNode) o;
                        boolean nested = false;
                        for ( CodebaseViewNode completed : completedNodes )
                        {
                            if ( completed.contains( node ) )
                            {
                                nested = true;
                                break;
                            }
                        }
                        if ( !nested )
                        {
                            completedNodes.add( node );
                            node.update();
                        }
                    }
                }
            }
        };
        updateAction.setImageDescriptor( Images.UPDATE_DESCRIPTOR );
        updateAction.setToolTipText( Messages.codebaseUpdateView_updateAction_tooltip );

        synchronizeAction = new Action( Messages.codebaseUpdateView_synchronizeAction_title )
        {
            @Override
            public void run()
            {
                Object selection = ( (IStructuredSelection) viewer.getSelection() ).getFirstElement();
                if ( selection != null && selection instanceof SourceTreeNode )
                {
                    IWorkspaceSourceTree tree = ( (SourceTreeNode) selection ).getWorkspaceSourceTree();

                    final ITeamProviderUI providerUI = getTeamProviderUI( tree );
                    if ( providerUI != null )
                    {
                        providerUI.synchronize( tree );
                    }
                }
            }
        };
        synchronizeAction.setImageDescriptor( Images.SYNCHRONIZE );
        synchronizeAction.setToolTipText( Messages.codebaseUpdateView_synchronizeAction_tooltip );

        helpAction = new Action( Messages.codebaseUpdateView_helpAction_title )
        {
            @Override
            public void run()
            {
                Object selection = ( (IStructuredSelection) viewer.getSelection() ).getFirstElement();
                if ( selection != null && selection instanceof SourceTreeNode )
                {
                    SourceTreeNode node = (SourceTreeNode) selection;
                    final String help = node.getWorkspaceSourceTree().getStatusHelp();
                    if ( help != null && showStatus)
                    {
                        final PopupDialog d =
                            new PopupDialog( null, PopupDialog.INFOPOPUP_SHELLSTYLE | SWT.MODELESS, true, false, false,
                                             false, false, " " + node.getName() + " - " + node.getStatus(), null ) //$NON-NLS-1$
                            {
                                @Override
                                protected Control createDialogArea( Composite parent )
                                {
                                    Composite dialogArea = new Composite( parent, SWT.NONE );
                                    dialogArea.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );
                                    dialogArea.setLayout( new GridLayout() );

                                    Text text = new Text( dialogArea, SWT.READ_ONLY | SWT.WRAP );
                                    text.setLayoutData( new GridData( SWT.FILL, SWT.FILL, false, false ) );
                                    text.setText( help );

                                    return dialogArea;
                                }

                                @Override
                                protected Point getInitialLocation( Point initialSize )
                                {
                                	//TODO mkleint: what would be the better location for popup action? popup invoked ui is misplaced...
                                    return Display.getCurrent().getCursorLocation();
                                }
                            };

                        d.open();
                        d.getShell().addShellListener( new ShellAdapter()
                        {
                            @Override
                            public void shellDeactivated( ShellEvent e )
                            {
                                d.close();
                            }
                        } );
                    }
                }
            }
        };
        helpAction.setImageDescriptor( PlatformUI.getWorkbench().getSharedImages().getImageDescriptor( ISharedImages.IMG_LCL_LINKTO_HELP ) );
        helpAction.setToolTipText( Messages.codebaseUpdateView_helpAction_tooltip );

        copyUrlAction = new Action( Messages.codebaseUpdateView_copyAction_title )
        {
            @Override
            public void run()
            {
                Object selection = ( (IStructuredSelection) viewer.getSelection() ).getFirstElement();
                if ( selection != null && selection instanceof CodebaseViewNode )
                {
                    String url = ( (CodebaseViewNode) selection ).getUrl();

                    Clipboard clipboard = new Clipboard( getSite().getShell().getDisplay() );
                    clipboard.setContents( new Object[] { url }, new TextTransfer[] { TextTransfer.getInstance() } );
                }
            }
        };
        copyUrlAction.setImageDescriptor( PlatformUI.getWorkbench().getSharedImages().getImageDescriptor( ISharedImages.IMG_TOOL_COPY ) );
        copyUrlAction.setToolTipText( Messages.codebaseUpdateView_copyAction_tooltip );
    }

    /**
     * Initialize the toolbar.
     */
    private void initializeToolBar()
    {
        IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();

        contributeActions( toolbarManager );
        toolbarManager.add( new Separator() );

        ActionContributionItem refreshActionItem = new ActionContributionItem( refreshAction );
        refreshActionItem.setMode( ActionContributionItem.MODE_FORCE_TEXT );
        toolbarManager.add( refreshActionItem );

        ActionContributionItem updateActionItem = new ActionContributionItem( updateAllAction );
        updateActionItem.setMode( ActionContributionItem.MODE_FORCE_TEXT );
        toolbarManager.add( updateActionItem );
    }

    /**
     * Initialize the menu.
     */
    private void initializeMenu()
    {
        MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown( true );
        menuMgr.addMenuListener( new IMenuListener()
        {
            public void menuAboutToShow( IMenuManager manager )
            {
                populateContextMenu( manager );
            }
        } );

        Menu menu = menuMgr.createContextMenu( viewer.getControl() );
        viewer.getControl().setMenu( menu );
        getSite().registerContextMenu( menuMgr, viewer );
    }

    private void populateContextMenu( IMenuManager menuManager )
    {
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();

        if ( selection.isEmpty() )
        {
            return;
        }

        boolean updateAvailable = true;
        for ( Object o : selection.toList() )
        {
            if ( o instanceof CodebaseViewNode && !( (CodebaseViewNode) o ).isUpdateAvailable() )
            {
                updateAvailable = false;
                break;
            }
        }

        updateAction.setEnabled( updateAvailable );
        menuManager.add( updateAction );

        if ( selection.size() == 1 )
        {
            CodebaseViewNode node = (CodebaseViewNode) selection.getFirstElement();

            if ( node instanceof SourceTreeNode )
            {
                IWorkspaceSourceTree tree = ( (SourceTreeNode) node ).getWorkspaceSourceTree();

                Collection<IProject> projects = AbstractSourceTreeOperation.getWorkspaceProjects( tree ).values();

                final ITeamProviderUI providerUI = getTeamProviderUI( tree );
                if ( providerUI != null && !projects.isEmpty() )
                {
                    menuManager.add( synchronizeAction );
                }

                if ( tree.getStatusHelp() != null && showStatus)
                {
                    menuManager.add( new Separator() );
                    menuManager.add( helpAction );
                }
            }

            if ( node.getUrl() != null )
            {
                menuManager.add( new Separator() );
                menuManager.add( copyUrlAction );
            }
        }
    }

    @Override
    public void setFocus()
    {
    }

    private static volatile int checkingForUpdateStatusCount = 0;

    private void doRefresh()
    {
    	if (codebase == null)
    	{
    		return;
    	}
        log.debug( "Refresh of codebase update status requested" );
        Job jb = new Job( Messages.codebaseUpdateView_refreshJob )
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                checkingForUpdateStatusCount++;
                try
                {
                    if ( checkingForUpdateStatusCount != 1 )
                    {
                        log.debug( "Refresh of codebase update status already in progress" );
                        return Status.OK_STATUS;
                    }
                    return doRun( monitor );
                }
                finally
                {
                    checkingForUpdateStatusCount--;
                }
            }

            private IStatus doRun( IProgressMonitor monitor )
            {
                Display.getDefault().syncExec( new Runnable()
                {
                    public void run()
                    {
                        showStatus = false;
                        viewer.refresh();
                        viewer.expandAll();
                    }
                } );
                try
                {
                    // MECLIPSE-1839
                    if ( CodebaseAccessChecker.checkAccess( codebase.getDescriptorUrl(), monitor ) )
                    {
                        // access check was cancelled.
                        return Status.OK_STATUS;
                    }
                    DetermineCodebaseUpdateStatusOperation op = new DetermineCodebaseUpdateStatusOperation( codebase );
                    op.run( monitor );
                }
                catch ( CoreException e )
                {
                    return e.getStatus();
                }
                Display.getDefault().syncExec( new Runnable()
                {

                    public void run()
                    {
                        showStatus = true;
                        if ( !disposed )
                        {
                            update();
                            // mkleint: does DetermineCUSO really change workspaceCodebase?
                            notifyWorkspaceCodebaseChangeListeners();
                        }
                    }
                } );
                return Status.OK_STATUS;
            }

        };
        jb.schedule();
    }

    private ITeamProviderUI getTeamProviderUI( IWorkspaceSourceTree tree )
    {
        String scmUrl = tree.getScmUrl();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint extensionPoint =
            registry.getExtensionPoint( "com.sonatype.s2.project.ui.materialization.teamProviderUIs" ); //$NON-NLS-1$
        if ( extensionPoint != null )
        {
            for ( IExtension extension : extensionPoint.getExtensions() )
            {
                for ( IConfigurationElement element : extension.getConfigurationElements() )
                {
                    if ( "provider".equals( element.getName() ) ) //$NON-NLS-1$
                    {
                        String type = "scm:" + element.getAttribute( "type" ); //$NON-NLS-1$ //$NON-NLS-2$
                        if ( scmUrl.startsWith( type ) )
                        {
                            try
                            {
                                return (ITeamProviderUI) element.createExecutableExtension( "class" ); //$NON-NLS-1$
                            }
                            catch ( CoreException e )
                            {
                                log.debug( Messages.codebaseUpdateView_errorCreatingTeamProvider, e );
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public void codebaseChanged( S2CodebaseChangeEvent event )
    {
        Display.getDefault().asyncExec( new Runnable()
        {
            public void run()
            {
                update();
            }
        } );
    }

    public void addWorkspaceCodebaseChangeListener( IS2CodebaseChangeEventListener listener )
    {
        assert listener instanceof IS2CodebaseChangeEventListener;
        listeners.add( listener );
    }

    public void removeWorkspaceCodebaseChangeListener( IS2CodebaseChangeEventListener listener )
    {
        listeners.remove( listener );
    }

    /**
     * Simulates the update action so the UI behaves consistently on those events that don't actually trigger an update
     * event in the core.
     */
    protected void notifyWorkspaceCodebaseChangeListeners()
    {
        S2CodebaseChangeEvent event = new S2CodebaseChangeEvent( codebase, codebase );
        for ( Object listener : listeners.getListeners() )
        {
            ( (IS2CodebaseChangeEventListener) listener ).codebaseChanged( event );
        }
    }

    private CodebaseViewNode[] createContent( IWorkspaceCodebase workspaceCodebase )
    {
        codebase = workspaceCodebase;
        boolean enableUpdateAll = true;
        if ( workspaceCodebase.getPending() == null )
        {
//MECLIPSE-1855            doRefresh();
        	enableUpdateAll = false;
        }
        else
        {
            for ( IWorkspaceSourceTree tree : workspaceCodebase.getPending().getSourceTrees() )
            {
                if ( IWorkspaceSourceTree.STATUS_NOT_SUPPORTED.equals( tree.getStatus() ) )
                {
                    enableUpdateAll = false;
                    break;
                }
            }
        }
        updateAllAction.setEnabled( enableUpdateAll );
        List<CodebaseViewNode> content = new ArrayList<CodebaseViewNode>();
        content.add( new EclipseNode( workspaceCodebase ) );
        content.add( new WorkspaceNode( workspaceCodebase ) );
        content.addAll( createExtensionNodes() );
        return content.toArray( new CodebaseViewNode[0] );
    }

    private void loadActionExtensions( String extensionId )
    {
        contributedActions = new ArrayList<IAction>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint( extensionId );
        if ( configuratorsExtensionPoint != null )
        {
            IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
            for ( IExtension extension : configuratorExtensions )
            {
                IConfigurationElement[] elements = extension.getConfigurationElements();
                for ( IConfigurationElement element : elements )
                {
                    if ( element.getName().equals( ELEMENT_ACTION ) )
                    {
                        ViewPluginAction action = new ViewPluginAction( element, this, null, IAction.AS_PUSH_BUTTON );
                        action.setText( element.getAttribute( ELEMENT_LABEL ) );
                        action.setToolTipText( element.getAttribute( ELEMENT_TOOLTIP ) );

                        String iconPath = element.getAttribute( ELEMENT_ICON );
                        ImageRegistry imageRegistry = Activator.getDefault().getImageRegistry();
                        ImageDescriptor imageDescriptor = imageRegistry.getDescriptor( iconPath );
                        if ( imageDescriptor == null )
                        {
                            imageDescriptor =
                                AbstractUIPlugin.imageDescriptorFromPlugin( extension.getContributor().getName(),
                                                                            Activator.IMAGE_PATH + iconPath );
                            imageRegistry.put( iconPath, imageDescriptor );
                        }
                        action.setImageDescriptor( imageDescriptor );
                        contributedActions.add( action );

                        // if ( getEditorInput() instanceof IFileEditorInput )
                        // {
                        // IFile file = ( (IFileEditorInput) getEditorInput() ).getFile();
                        // action.selectionChanged( new StructuredSelection( file ) );
                        // }
                    }
                }
            }
        }
    }

    private static final String NODE_EXT_POINT =
        "com.sonatype.s2.project.ui.materialization.update.spi.CodebaseViewNodeProvider"; //$NON-NLS-1$

    private static final String NODE_ELEM_PROVIDER = "provider"; //$NON-NLS-1$

    // TODO we currently ignore the fact that a job provider could be added at runtime..
    // no listening on changes so far..
    List<ExtensionCodebaseViewNode> createExtensionNodes()
    {
        ArrayList<ExtensionCodebaseViewNode> nodes = new ArrayList<ExtensionCodebaseViewNode>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint extensionPoint = registry.getExtensionPoint( NODE_EXT_POINT );
        if ( extensionPoint != null )
        {
            for ( IExtension extension : extensionPoint.getExtensions() )
            {
                for ( IConfigurationElement element : extension.getConfigurationElements() )
                {
                    if ( NODE_ELEM_PROVIDER.equals( element.getName() ) )
                    {
                        try
                        {
                            CodebaseViewNodeProvider prov =
                                (CodebaseViewNodeProvider) element.createExecutableExtension( "class" ); //$NON-NLS-1$
                            prov.setWorkspaceCodebase( getWorkspaceCodebase() );
                            nodes.add( new ExtensionCodebaseViewNode( getWorkspaceCodebase(), prov ) );
                        }
                        catch ( CoreException e )
                        {
                            log.error( "Could not create node provider", e ); //$NON-NLS-1$
                        }
                    }
                }
            }
        }

        return nodes;
    }

    private void contributeActions( IToolBarManager toolbarManager )
    {
        if ( contributedActions != null )
        {
            for ( IAction action : contributedActions )
            {
                ActionContributionItem item = new ActionContributionItem( action );
                toolbarManager.add( item );
            }
        }
    }

    public IWorkspaceCodebase getWorkspaceCodebase()
    {
        return codebase;
    }

    public static void open()
        throws PartInitException
    {
        IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        workbenchPage.showView( ID );
    }
    
    private class Prov extends LabelProvider implements IFontProvider {
    	
    	private Font unsupportedFont;

		public Prov() {
            FontData[] data = tree.getFont().getFontData();
            FontData[] newData = new FontData[data.length];

            for ( int i = data.length - 1; i >= 0; i-- )
            {
                newData[i] = new FontData( data[i].getName(), data[i].getHeight(), SWT.BOLD );
            }

            unsupportedFont = new Font( Display.getCurrent(), newData );
    	}
    
        @Override
		public void dispose() {
			super.dispose();
			unsupportedFont.dispose();
		}

		@Override
        public Image getImage( Object element )
        {
            if ( element instanceof CodebaseViewNode )
            {
                CodebaseViewNode node = (CodebaseViewNode) element;
                return node.getImage(showStatus);
            }
            return super.getImage( element );
        }

        @Override
        public String getText( Object element )
        {
            if ( element instanceof CodebaseViewNode )
            {
                CodebaseViewNode node = (CodebaseViewNode) element;
                String name = node.getName();
                String status = showStatus ? node.getStatus() : null;
                return status == null ? name : NLS.bind( Messages.codebaseUpdateView_statusFormat, name, status );
            }
            return super.getText( element );
        }

		public Font getFont(Object element) {
            if ( element instanceof CodebaseViewNode )
            {
                CodebaseViewNode node = (CodebaseViewNode) element;
                if (showStatus && node.isUpdateUnsupported()) {
                	return unsupportedFont;
                }
            	
            }
			// TODO Auto-generated method stub
			return null;
		}
    } 
}
