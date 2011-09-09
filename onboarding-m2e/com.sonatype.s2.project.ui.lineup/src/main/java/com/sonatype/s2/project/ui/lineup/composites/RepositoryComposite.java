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
package com.sonatype.s2.project.ui.lineup.composites;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.nexus.p2.rest.model.P2LineupRepositoryError;
import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.IP2LineupSourceRepository;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupSourceRepository;
import com.sonatype.s2.project.ui.internal.Dialog;
import com.sonatype.s2.project.ui.internal.Images;
import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.project.ui.lineup.dialogs.AddRepositoryDialog;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;

public class RepositoryComposite
    extends LineupComposite
{
    private static final Logger log = LoggerFactory.getLogger( RepositoryComposite.class );

    public static final String REPOSITORY_CONTROL = "repositoryTable"; //$NON-NLS-1$

    public static final String ADD_CONTROL = "addButton"; //$NON-NLS-1$

    public static final String REMOVE_CONTROL = "removeButton"; //$NON-NLS-1$

    public static final String UP_CONTROL = "upButton"; //$NON-NLS-1$

    public static final String DOWN_CONTROL = "downButton"; //$NON-NLS-1$

    public static final String VALIDATE_CONTROL = "validateButton"; //$NON-NLS-1$

    private Map<String, P2LineupRepositoryError> errors = new HashMap<String, P2LineupRepositoryError>();

    private String errorMessage;

    private int errorSeverity;

    private List<String> repoUrls = new ArrayList<String>();

    private TableViewer viewer;

    private Button addButton;

    private Button removeButton;

    private Button upButton;

    private Button downButton;

    private Link manageRepositoriesLink;

    public RepositoryComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                                FormToolkit toolkit )
    {
        super( parent, widthGroup, validationGroup, toolkit );

        GridLayout gridLayout = new GridLayout( 2, false );
        gridLayout.verticalSpacing = 1;
        setLayout( gridLayout );

        createViewer();
        createButtons();
        createLink();
        createMenu();
    }

    @SuppressWarnings( "unchecked" )
    private void createViewer()
    {
        final Table table;
        if ( isFormMode() )
        {
            table = getToolkit().createTable( this, SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.MULTI | SWT.V_SCROLL );
        }
        else
        {
            table = new Table( this, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.MULTI | SWT.V_SCROLL );
        }
        table.setData( "name", REPOSITORY_CONTROL ); //$NON-NLS-1$
        GridData gd = new GridData( SWT.FILL, SWT.FILL, true, true, 1, 5 );
        gd.heightHint = 100;
        gd.widthHint = isFormMode() ? 100 : 500;
        table.setLayoutData( gd );
        table.addMouseTrackListener( new MouseTrackAdapter()
        {
            public void mouseHover( MouseEvent e )
            {
                TableItem ti = table.getItem( new Point( e.x, e.y ) );
                if ( ti != null )
                {
                    P2LineupSourceRepository repo = (P2LineupSourceRepository) ti.getData();
                    P2LineupRepositoryError err = errors.get( repo.getUrl() );
                    if ( err != null )
                    {
                        table.setToolTipText( err.getErrorMessage() );
                        return;
                    }
                }
                table.setToolTipText( null );
            }
        } );

        createErrorFont( table.getFont() );

        viewer = new TableViewer( table );
        viewer.setLabelProvider( new RepoLabelProvider() );
        viewer.setContentProvider( new RepoContentProvider() );
        viewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                updateButtons();
            }
        } );

        getValidationGroup().add( viewer, new Validator<ISelection>()
        {
            public Class<ISelection> modelType()
            {
                return ISelection.class;
            }

            public void validate( Problems problems, String name, ISelection selection )
            {
                if ( !selection.isEmpty() )
                {
                    P2LineupSourceRepository repo =
                        (P2LineupSourceRepository) ( (IStructuredSelection) selection ).getFirstElement();
                    P2LineupRepositoryError err = errors.get( repo.getUrl() );
                    if ( err != null )
                    {
                        problems.add( err.getErrorMessage(), err.isWarning() ? Severity.WARNING : Severity.FATAL );
                        return;
                    }
                }
                if ( getLineupInfo().getLineup().getRepositories().size() == 0 )
                {
                    problems.add( Messages.repositoryComposite_repositoryRequired, Severity.FATAL );
                }
                else if ( errorMessage != null )
                {
                    problems.add( errorMessage, errorSeverity == IStatus.WARNING ? Severity.WARNING : Severity.FATAL );
                }
            }
        } );
    }

    private void createButtons()
    {
        addButton = createButton( Messages.repositoryComposite_add );
        addButton.setData( "name", ADD_CONTROL ); //$NON-NLS-1$
        addToWidthGroup( addButton );
        addButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                addRepository();
            }
        } );

        removeButton = createButton( Messages.repositoryComposite_remove );
        removeButton.setData( "name", REMOVE_CONTROL ); //$NON-NLS-1$
        addToWidthGroup( removeButton );
        removeButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                removeRepository();
            }
        } );

        upButton = createButton( Messages.repositoryComposite_moveUp );
        upButton.setData( "name", UP_CONTROL ); //$NON-NLS-1$
        addToWidthGroup( upButton );
        upButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                moveRepository( true );
            }
        } );

        downButton = createButton( Messages.repositoryComposite_moveDown );
        downButton.setData( "name", DOWN_CONTROL ); //$NON-NLS-1$
        addToWidthGroup( downButton );
        downButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                moveRepository( false );
            }
        } );

        new Label( this, SWT.NONE );
    }

    private void createLink()
    {
        manageRepositoriesLink = new Link( this, SWT.NONE );

        manageRepositoriesLink.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                String url = getManageRepositoriesURL();
                IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
                IWebBrowser browser;
                try
                {
                    browser =
                        support.isInternalWebBrowserAvailable() ? support.createBrowser( IWorkbenchBrowserSupport.AS_EDITOR
                                                                                             | IWorkbenchBrowserSupport.LOCATION_BAR
                                                                                             | IWorkbenchBrowserSupport.STATUS,
                                                                                         null, url, url )
                                        : support.getExternalBrowser();
                    browser.openURL( new URL( url ) );
                }
                catch ( PartInitException partInitException )
                {
                    log.error( partInitException.getMessage(), partInitException );
                }
                catch ( MalformedURLException malformedURLException )
                {
                    log.error( malformedURLException.getMessage(), malformedURLException );
                }
            }
        } );

        if ( !isFormMode() )
        {
            Button validateButton = createButton( "Validate" );
            validateButton.setData( "name", VALIDATE_CONTROL ); //$NON-NLS-1$
            addToWidthGroup( validateButton );
            validateButton.addSelectionListener( new SelectionAdapter()
            {
                @Override
                public void widgetSelected( SelectionEvent e )
                {
                    validateLineup();
                }
            } );
        }
    }

    private void createMenu()
    {
        final IAction copyAction = new Action( com.sonatype.s2.project.ui.internal.Messages.actions_copy_title )
        {
            @Override
            public void run()
            {
                StringBuilder sb = new StringBuilder();
                for ( TableItem item : viewer.getTable().getSelection() )
                {
                    if ( sb.length() > 0 )
                    {
                        sb.append( '\n' ); //$NON-NLS-1$
                    }
                    sb.append( item.getText() );
                }

                Clipboard clipboard = new Clipboard( getShell().getDisplay() );
                clipboard.setContents( new Object[] { sb.toString() },
                                       new TextTransfer[] { TextTransfer.getInstance() } );
            }
        };

        MenuManager menuMgr = new MenuManager(); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown( true );
        menuMgr.addMenuListener( new IMenuListener()
        {
            public void menuAboutToShow( IMenuManager manager )
            {
                if ( !viewer.getSelection().isEmpty() )
                {
                    manager.add( copyAction );
                }
            }
        } );
        Menu menu = menuMgr.createContextMenu( viewer.getTable() );
        viewer.getTable().setMenu( menu );
    }

    private void updateButtons()
    {
        assert viewer.getSelection() instanceof StructuredSelection;
        StructuredSelection selection = (StructuredSelection) viewer.getSelection();

        removeButton.setEnabled( selection.size() > 0 );

        boolean singleSelection = selection.size() == 1;
        int n = viewer.getTable().getSelectionIndex();
        upButton.setEnabled( singleSelection && n > 0 );
        downButton.setEnabled( singleSelection && n + 1 < viewer.getTable().getItemCount() );
    }

    @Override
    protected void update( NexusLineupPublishingInfo info )
    {
        viewer.setSelection( StructuredSelection.EMPTY );
        updateViewer();
        updateManageRepositoriesLink();
        updateButtons();
    }

    private void addRepository()
    {
        AddRepositoryDialog addRepositoryDialog = new AddRepositoryDialog( getShell(), getLineupInfo() );
        if ( addRepositoryDialog.open() != Dialog.OK )
        {
            return;
        }

        String sourceRepositoryUrl = addRepositoryDialog.getLocation().trim();
        // Trim trailing /
        if ( sourceRepositoryUrl.endsWith( "/" ) ) //$NON-NLS-1$
        {
            sourceRepositoryUrl = sourceRepositoryUrl.substring( 0, sourceRepositoryUrl.length() - 1 );
        }
        P2LineupSourceRepository sourceRepository = new P2LineupSourceRepository( sourceRepositoryUrl );
        P2Lineup lineup = getLineupInfo().getLineup();
        lineup.addRepository( sourceRepository );

        updateViewer();
        notifyLineupChangeListeners();
    }

    private void removeRepository()
    {
        ISelection selection = viewer.getSelection();
        assert selection instanceof IStructuredSelection;

        P2Lineup lineup = getLineupInfo().getLineup();
        for ( Object repo : ( (IStructuredSelection) selection ).toList() )
        {
            if ( repo instanceof IP2LineupSourceRepository )
            {
                lineup.removeRepository( (IP2LineupSourceRepository) repo );
            }
        }

        updateViewer();
        notifyLineupChangeListeners();
    }

    private void moveRepository( boolean up )
    {
        assert viewer.getSelection() instanceof StructuredSelection;
        Object o = ( (StructuredSelection) viewer.getSelection() ).getFirstElement();
        if ( o instanceof IP2LineupSourceRepository )
        {
            IP2LineupSourceRepository repo = (IP2LineupSourceRepository) o;
            P2Lineup lineup = getLineupInfo().getLineup();
            List<IP2LineupSourceRepository> repos = new ArrayList<IP2LineupSourceRepository>( lineup.getRepositories() );

            int n = repos.indexOf( repo );
            repos.remove( n );
            repos.add( n + ( up ? -1 : 1 ), repo );

            lineup.setRepositories( new LinkedHashSet<IP2LineupSourceRepository>( repos ) );

            updateViewer();
            notifyLineupChangeListeners();
            viewer.setSelection( new StructuredSelection( repo ), true );
        }
    }

    public void updateViewer()
    {
        viewer.setInput( getLineupInfo().getLineup() );
    }

    private void updateManageRepositoriesLink()
    {
        manageRepositoriesLink.setText( NLS.bind( "<A HREF=\"{1}\">{0}</A>", //$NON-NLS-1$
                                                  Messages.repositoryComposite_manageNexusRepositories,
                                                  getManageRepositoriesURL() ) );
    }

    private String getManageRepositoriesURL()
    {
        String url = getLineupInfo().getServerUrl();
        if ( url == null )
        {
            return ""; //$NON-NLS-1$
        }
        StringBuilder sb = new StringBuilder( url );
        if ( !url.endsWith( "/" ) ) //$NON-NLS-1$
        {
            sb.append( '/' );
        }
        sb.append( "index.html#view-repositories" ); //$NON-NLS-1$
        return sb.toString();
    }

    private class RepoLabelProvider
        extends LabelProvider
        implements IColorProvider, IFontProvider
    {
        public Image getImage( Object element )
        {
            P2LineupRepositoryError error = getError( element );
            if ( error != null )
            {
                if ( error.isWarning() )
                {
                    return Images.WARN_REPOSITORY;
                }
                return Images.ERROR_REPOSITORY;
            }
            return Images.REPOSITORY;
        }

        public String getText( Object element )
        {
            if ( element instanceof IP2LineupSourceRepository )
            {
                IP2LineupSourceRepository repo = (IP2LineupSourceRepository) element;
                return repo.getUrl();
            }
            return "<unexpected element> " + super.getText( element ); //$NON-NLS-1$
        }

        public Color getBackground( Object element )
        {
            return null;
        }

        public Color getForeground( Object element )
        {
            P2LineupRepositoryError error = getError( element );
            if ( error != null && !error.isWarning() )
            {
                return Display.getDefault().getSystemColor( SWT.COLOR_DARK_RED );
            }

            return null;
        }

        public Font getFont( Object element )
        {
            P2LineupRepositoryError error = getError( element );
            if ( error != null && !error.isWarning() )
            {
                return getErrorFont();
            }

            return null;
        }

        private P2LineupRepositoryError getError( Object element )
        {
            if ( element instanceof IP2LineupSourceRepository )
            {
                return errors.get( ( (IP2LineupSourceRepository) element ).getUrl() );
            }
            return null;
        }
    }

    private class RepoContentProvider
        implements IStructuredContentProvider
    {
        public Object[] getElements( Object inputElement )
        {
            List<IP2LineupSourceRepository> toRet = new ArrayList<IP2LineupSourceRepository>();
            if ( inputElement instanceof IP2Lineup )
            {
                repoUrls.clear();
                for ( IP2LineupSourceRepository u : ( (IP2Lineup) inputElement ).getRepositories() )
                {
                    toRet.add( u );
                    repoUrls.add( u.getUrl() );
                }
            }
            return toRet.toArray( new IP2LineupSourceRepository[0] );
        }

        public void dispose()
        {
        }

        public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
        {
        }
    }

    public void setErrors( List<P2LineupRepositoryError> repoErrors, String globalErrorMessage, int globalErrorSeverity )
    {
        errors.clear();
        this.errorMessage = /*repoErrors.isEmpty() ? null : */globalErrorMessage;
        this.errorSeverity = globalErrorSeverity;

        for ( P2LineupRepositoryError err : repoErrors )
        {
            if ( !repoUrls.contains( err.getRepositoryURL() ) )
            {
                log.error( "unknwon error repo=" + err.getRepositoryURL() ); //$NON-NLS-1$
            }
            errors.put( err.getRepositoryURL(), err );
        }
    }

    protected void validateLineup()
    {
        // client override for wizard use
    }

    public TableViewer getViewer()
    {
        return viewer;
    }
}
