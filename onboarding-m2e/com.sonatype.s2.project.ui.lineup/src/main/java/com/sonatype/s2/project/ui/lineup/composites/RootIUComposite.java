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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;

import com.sonatype.nexus.p2.rest.model.P2LineupUnresolvedInstallableUnit;
import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.IP2LineupInstallableUnit;
import com.sonatype.s2.p2lineup.model.IP2LineupSourceRepository;
import com.sonatype.s2.p2lineup.model.IP2LineupTargetEnvironment;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.project.ui.internal.Dialog;
import com.sonatype.s2.project.ui.internal.Images;
import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.project.ui.lineup.dialogs.AddInstallableUnitDialog;
import com.sonatype.s2.project.ui.lineup.dialogs.EnvironmentDialog;
import com.sonatype.s2.publisher.S2PublisherConstants;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;

public class RootIUComposite
    extends LineupComposite
{
    public static final String IU_CONTROL = "rootIUTable"; //$NON-NLS-1$

    public static final String ADD_CONTROL = "addButton"; //$NON-NLS-1$

    public static final String EDIT_CONTROL = "editButton"; //$NON-NLS-1$

    public static final String REMOVE_CONTROL = "removeButton"; //$NON-NLS-1$

    private Map<String, P2LineupUnresolvedInstallableUnit> errors =
        new HashMap<String, P2LineupUnresolvedInstallableUnit>();

    private List<String> rootIUIds = new ArrayList<String>();

    // list of IUs that are not root IUs but we got it back from validation
    private List<IP2LineupInstallableUnit> additionalErrorUnits = new ArrayList<IP2LineupInstallableUnit>();

    private String errorMessage;

    private int errorSeverity;

    private TableViewer viewer;

    private Button addButton;

    private Button editButton;

    private Button removeButton;

    public RootIUComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                            FormToolkit toolkit )
    {
        super( parent, widthGroup, validationGroup, toolkit );

        GridLayout gridLayout = new GridLayout( 2, false );
        gridLayout.verticalSpacing = 1;
        setLayout( gridLayout );

        createViewer();
        createButtons();
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
        table.setData( "name", IU_CONTROL ); //$NON-NLS-1$
        GridData gd = new GridData( SWT.FILL, SWT.FILL, true, true, 1, 4 );
        gd.heightHint = 200;
        gd.widthHint = isFormMode() ? 100 : 500;
        table.setLayoutData( gd );
        table.setHeaderVisible( true );
        table.addMouseTrackListener( new MouseTrackAdapter()
        {
            public void mouseHover( MouseEvent e )
            {
                TableItem ti = table.getItem( new Point( e.x, e.y ) );
                if ( ti != null )
                {
                    IP2LineupInstallableUnit unit = (IP2LineupInstallableUnit) ti.getData();
                    P2LineupUnresolvedInstallableUnit err = errors.get( unit.getId() );
                    if ( err != null )
                    {
                        table.setToolTipText( "" + err.getErrorMessage() ); //$NON-NLS-1$
                        return;
                    }
                }
                table.setToolTipText( null );
            }
        } );

        createErrorFont( table.getFont() );

        viewer = new TableViewer( table );

        TableViewerColumn nameColumn = new TableViewerColumn( viewer, SWT.NONE );
        nameColumn.getColumn().setResizable( true );
        nameColumn.getColumn().setText( Messages.rootIUComposite_nameColumn );
        nameColumn.getColumn().setWidth( 350 );

        TableViewerColumn versionColumn = new TableViewerColumn( viewer, SWT.NONE );
        versionColumn.getColumn().setResizable( true );
        versionColumn.getColumn().setText( Messages.rootIUComposite_versionColumn );
        versionColumn.getColumn().setWidth( 150 );

        final IUViewerLabelProvider labelProvider = new IUViewerLabelProvider();
        viewer.setLabelProvider( labelProvider );
        viewer.setContentProvider( new IUContentProvider() );
        viewer.setComparator( new ViewerComparator()
        {
            public int compare( Viewer viewer, Object e1, Object e2 )
            {
                if ( e1 == null )
                {
                    return e2 == null ? 0 : -1;
                }
                else if ( e2 == null )
                {
                    return 1;
                }

                IP2LineupInstallableUnit u1 = (IP2LineupInstallableUnit) e1;
                IP2LineupInstallableUnit u2 = (IP2LineupInstallableUnit) e2;
                boolean error1 = errors.containsKey( u1.getId() );
                boolean error2 = errors.containsKey( u2.getId() );
                if ( error1 && !error2 )
                {
                    return -1;
                }
                if ( !error1 && error2 )
                {
                    return 1;
                }
                return labelProvider.getText( u1 ).compareTo( labelProvider.getText( u2 ) );
            }
        } );
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
                    IP2LineupInstallableUnit unit =
                        (IP2LineupInstallableUnit) ( (IStructuredSelection) selection ).getFirstElement();
                    P2LineupUnresolvedInstallableUnit err = errors.get( unit.getId() );
                    if ( err != null )
                    {
                        problems.add( err.getErrorMessage(), err.isWarning() ? Severity.WARNING : Severity.FATAL );
                        return;
                    }
                }
                if ( getLineupInfo().getLineup().getRootInstallableUnits().size() == 0 )
                {
                    problems.add( Messages.rootIUComposite_iuRequired, Severity.FATAL );
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
        addButton = createButton( Messages.rootIUComposite_add );
        addButton.setData( "name", ADD_CONTROL ); //$NON-NLS-1$
        addToWidthGroup( addButton );
        addButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                addUnit();
            }
        } );

        editButton = createButton( Messages.rootIUComposite_edit );
        editButton.setData( "name", EDIT_CONTROL ); //$NON-NLS-1$
        addToWidthGroup( editButton );
        editButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                editUnit();
            }
        } );

        removeButton = createButton( Messages.rootIUComposite_remove );
        removeButton.setData( "name", REMOVE_CONTROL ); //$NON-NLS-1$
        addToWidthGroup( removeButton );
        removeButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                removeUnits();
            }
        } );
    }

    private void updateButtons()
    {
        assert viewer.getSelection() instanceof StructuredSelection;

        StructuredSelection selection = (StructuredSelection) viewer.getSelection();

        boolean hasAddError = false;
        for ( Object ob : selection.toArray() )
        {
            if ( additionalErrorUnits.contains( ob ) )
            {
                hasAddError = true;
                break;
            }
        }
        removeButton.setEnabled( ( selection.size() == 1 && !hasAddError ) || selection.size() > 1 );
        editButton.setEnabled( selection.size() == 1 && !hasAddError );
    }

    @Override
    protected void update( NexusLineupPublishingInfo info )
    {
        viewer.setSelection( StructuredSelection.EMPTY );
        updateViewer();
        updateButtons();
    }

    protected void addUnit()
    {
        AddInstallableUnitDialog dialog = new AddInstallableUnitDialog( getShell(), getLineupInfo() );
        if ( dialog.open() != Dialog.OK )
        {
            return;
        }

        IP2LineupInstallableUnit[] ius = dialog.getInstallableUnits();
        P2Lineup lineup = getLineupInfo().getLineup();
        for ( IP2LineupInstallableUnit unit : ius )
        {
            lineup.addRootInstallableUnit( unit );
        }
        IP2LineupSourceRepository[] repos = dialog.getRepositories();
        for ( IP2LineupSourceRepository repo : repos )
        {
            String sourceRepositoryUrl = repo.getUrl().trim();
            // Trim trailing /
            if ( sourceRepositoryUrl.endsWith( "/" ) ) //$NON-NLS-1$
            {
                sourceRepositoryUrl = sourceRepositoryUrl.substring( 0, sourceRepositoryUrl.length() - 1 );
            }
            repo.setUrl( sourceRepositoryUrl );
            lineup.addRepository( repo );
        }
        updateViewer();
        notifyLineupChangeListeners();
    }

    private void editUnit()
    {
        ISelection selection = viewer.getSelection();
        assert selection instanceof IStructuredSelection;
        if ( !selection.isEmpty() )
        {
            Object first = ( (IStructuredSelection) selection ).getFirstElement();
            if ( first instanceof IP2LineupInstallableUnit )
            {
                IP2LineupInstallableUnit unit = (IP2LineupInstallableUnit) first;
                EnvironmentDialog dialog =
                    new EnvironmentDialog( getShell(), unit.getId(), unit.getTargetEnvironments() );
                if ( dialog.open() == Dialog.OK )
                {
                    unit.setTargetEnvironments( dialog.getSelection() );
                    notifyLineupChangeListeners();
                }
            }
        }
    }

    private void removeUnits()
    {
        ISelection selection = viewer.getSelection();
        assert selection instanceof IStructuredSelection;
        P2Lineup lineup = getLineupInfo().getLineup();
        for ( Object unit : ( (IStructuredSelection) selection ).toList() )
        {
            if ( unit instanceof IP2LineupInstallableUnit && !additionalErrorUnits.contains( unit ) )
            {
                IP2LineupInstallableUnit iu = (IP2LineupInstallableUnit) unit;
                if ( S2PublisherConstants.PROJECT_MATERIALIZER_IU_ID.equals( iu.getId() ) )
                {
                    if ( MessageDialog.openQuestion( getShell(), Messages.rootIUComposite_confirmDelete_title,
                                                     Messages.rootIUComposite_confirmDelete_message ) )
                    {
                        lineup.removeRootInstallableUnit( (IP2LineupInstallableUnit) unit );
                    }
                }
                else
                {
                    lineup.removeRootInstallableUnit( (IP2LineupInstallableUnit) unit );
                }
            }
        }
        updateViewer();
        notifyLineupChangeListeners();
    }

    public void updateViewer()
    {
        viewer.setInput( getLineupInfo().getLineup() );
    }

    private class IUViewerLabelProvider
        extends LabelProvider
        implements ITableLabelProvider, ITableColorProvider, ITableFontProvider
    {
        public Image getColumnImage( Object element, int col )
        {
            if ( col == 0 )
            {
                P2LineupUnresolvedInstallableUnit error = getError( element );
                if ( error != null )
                {
                    if ( error.isWarning() )
                    {
                        return Images.WARN_INSTALLABLE_UNIT;
                    }
                    return Images.ERROR_INSTALLABLE_UNIT;
                }
                return Images.INSTALLABLE_UNIT;
            }
            return super.getImage( element );
        }

        public String getColumnText( Object element, int col )
        {
            if ( element instanceof IP2LineupInstallableUnit )
            {
                IP2LineupInstallableUnit iu = (IP2LineupInstallableUnit) element;

                if ( col == 0 )
                {
                    String iuName = iu.getName();
                    if ( iuName != null && iuName.trim().length() > 0 )
                    {
                        return iuName;
                    }
                    // Empty IU name, return the id instead
                    return iu.getId();
                }
                return iu.getVersion();
            }
            return "<unexpected element> " + super.getText( element ); //$NON-NLS-1$
        }

        public Color getBackground( Object element, int col )
        {
            return null;
        }

        public Color getForeground( Object element, int col )
        {
            P2LineupUnresolvedInstallableUnit error = getError( element );
            if ( error != null && !error.isWarning() )
            {
                return Display.getDefault().getSystemColor( SWT.COLOR_DARK_RED );
            }

            return null;
        }

        public Font getFont( Object element, int columnIndex )
        {
            P2LineupUnresolvedInstallableUnit error = getError( element );
            if ( error != null && !error.isWarning() )
            {
                return getErrorFont();
            }

            return null;
        }

        private P2LineupUnresolvedInstallableUnit getError( Object element )
        {
            if ( element instanceof IP2LineupInstallableUnit )
            {
                return errors.get( ( (IP2LineupInstallableUnit) element ).getId() );
            }
            return null;
        }
    }

    private class IUContentProvider
        implements IStructuredContentProvider
    {
        public Object[] getElements( Object inputElement )
        {
            List<IP2LineupInstallableUnit> toRet = new ArrayList<IP2LineupInstallableUnit>();
            if ( inputElement instanceof IP2Lineup )
            {
                rootIUIds.clear();
                for ( IP2LineupInstallableUnit u : ( (IP2Lineup) inputElement ).getRootInstallableUnits() )
                {
                    toRet.add( u );
                    rootIUIds.add( u.getId() );
                }
            }
            if ( additionalErrorUnits.size() > 0 )
            {
                toRet.addAll( additionalErrorUnits );
            }
            return toRet.toArray( new IP2LineupInstallableUnit[0] );
        }

        public void dispose()
        {
        }

        public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
        {
        }
    }

    public void setErrors( List<P2LineupUnresolvedInstallableUnit> iuErrors, String globalErrorMessage,
                           int globalErrorSeverity )
    {
        errors.clear();
        additionalErrorUnits.clear();
        this.errorMessage = /*iuErrors.isEmpty() ? null : */globalErrorMessage;
        this.errorSeverity = globalErrorSeverity;

        for ( P2LineupUnresolvedInstallableUnit err : iuErrors )
        {
            if ( !rootIUIds.contains( err.getInstallableUnitId() ) )
            {
                additionalErrorUnits.add( new ErrorUnit( err ) );
            }
            errors.put( err.getInstallableUnitId(), err );
        }
    }

    private class ErrorUnit
        implements IP2LineupInstallableUnit
    {

        private P2LineupUnresolvedInstallableUnit error;

        public ErrorUnit( P2LineupUnresolvedInstallableUnit err )
        {
            error = err;
        }

        public String getName()
        {
            return error.getInstallableUnitId();
        }

        public String getId()
        {
            return error.getInstallableUnitId();
        }

        public String getVersion()
        {
            return error.getInstallableUnitVersion();
        }

        public Set<IP2LineupTargetEnvironment> getTargetEnvironments()
        {
            return new LinkedHashSet<IP2LineupTargetEnvironment>();
        }

        public void setTargetEnvironments( Set<IP2LineupTargetEnvironment> list )
        {
        }
    }

    public TableViewer getViewer()
    {
        return viewer;
    }
}
