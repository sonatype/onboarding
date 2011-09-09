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
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;

import com.sonatype.s2.p2lineup.model.IP2LineupP2Advice;
import com.sonatype.s2.p2lineup.model.IP2LineupTargetEnvironment;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupP2Advice;
import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.publisher.Activator;
import com.sonatype.s2.publisher.S2PublisherConstants;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;

public class RuntimeEnvironmentComposite
    extends LineupComposite
{
    public static final String RUNTIME_ENVIRONMENT_CONTROL = "runtimeEnvironmentTable"; //$NON-NLS-1$

    public static final String HEAP_SIZE_CONTROL = "heapSizeText"; //$NON-NLS-1$

    private static final String LINEUP_PLATFORMS = "LineupInfoWizardPage.selected"; //$NON-NLS-1$

    private static final String HEAP_ADVICE_PREFIX = "configure=addJvmArg(jvmArg:-Xmx"; //$NON-NLS-1$

    private static final String HEAP_ADVICE_SUFFIX = "m)"; //$NON-NLS-1$

    private CheckboxTableViewer viewer;

    private Text heapSizeText;

    public RuntimeEnvironmentComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                                        FormToolkit toolkit )
    {
        super( parent, widthGroup, validationGroup, toolkit );

        setLayout( new GridLayout( 2, false ) );

        createEnvironmentControls();
        createHeapControls();
    }

    @SuppressWarnings( "unchecked" )
    private void createEnvironmentControls()
    {
        Label label = createLabel( Messages.runtimeEnvironmentComposite_targetEnvironments_label );
        GridData labelData = (GridData) label.getLayoutData();
        labelData.verticalAlignment = SWT.TOP;

        Table table;
        if ( isFormMode() )
        {
            table = getToolkit().createTable( this, SWT.CHECK | SWT.FULL_SELECTION | SWT.V_SCROLL );
        }
        else
        {
            table = new Table( this, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION | SWT.V_SCROLL );
        }
        table.setData( "name", RUNTIME_ENVIRONMENT_CONTROL ); //$NON-NLS-1$
        viewer = new CheckboxTableViewer( table );
        GridData viewerData = createInputData();
        viewerData.heightHint = 100;
        viewerData.verticalAlignment = SWT.TOP;
        table.setLayoutData( viewerData );
        viewer.setLabelProvider( new EnvironmentLabelProvider() );
        viewer.setContentProvider( new IStructuredContentProvider()
        {
            public Object[] getElements( Object inputElement )
            {
                return getAllEnvironments().toArray();
            }

            public void dispose()
            {
            }

            public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
            {
            }
        } );
        viewer.addCheckStateListener( new ICheckStateListener()
        {
            public void checkStateChanged( CheckStateChangedEvent event )
            {
                P2Lineup lineup = getLineupInfo().getLineup();
                lineup.getTargetEnvironments().clear();
                for ( Object o : viewer.getCheckedElements() )
                {
                    assert o instanceof IP2LineupTargetEnvironment;
                    lineup.addTargetEnvironment( (IP2LineupTargetEnvironment) o );
                }

                notifyLineupChangeListeners();
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
                if ( viewer.getCheckedElements().length == 0 )
                {
                    problems.add( Messages.runtimeEnvironmentComposite_targetEnvironments_selectionRequired, Severity.FATAL );
                }
            }
        } );
    }

    private void createHeapControls()
    {
        createLabel( Messages.runtimeEnvironmentComposite_heapSize_label );

        heapSizeText = createText( HEAP_SIZE_CONTROL, null ); //$NON-NLS-1$
        heapSizeText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                String mem = heapSizeText.getText().trim();

                List<String> list = new ArrayList<String>();

                if ( mem.length() > 0 )
                {
                    list.add( HEAP_ADVICE_PREFIX + mem + HEAP_ADVICE_SUFFIX );
                }

                P2LineupP2Advice advice = null;
                if ( list.size() > 0 )
                {
                    advice = new P2LineupP2Advice();
                    advice.setAdvices( list );
                    advice.setTouchpointId( S2PublisherConstants.TOUCHPOINT_ID );
                    advice.setTouchpointVersion( S2PublisherConstants.TOUCHPOINT_VERSION );
                }
                getLineupInfo().getLineup().setP2Advice( advice );

                notifyLineupChangeListeners();
            }
        } );
        GridData gd = (GridData) heapSizeText.getLayoutData();
        gd.grabExcessHorizontalSpace = false;
        gd.horizontalAlignment = SWT.LEFT;
        gd.widthHint = 50;

        heapSizeText.addVerifyListener( new VerifyListener()
        {
            public void verifyText( VerifyEvent e )
            {
                e.doit = Character.isISOControl( e.character ) || Character.isDigit( e.character );
            }
        } );
    }

    @Override
    protected void update( NexusLineupPublishingInfo info )
    {
        viewer.setInput( info );

        updateEnvironments( info );
        updateHeap( info );
    }

    private void updateEnvironments( NexusLineupPublishingInfo info )
    {
        List<IP2LineupTargetEnvironment> selected =
            new ArrayList<IP2LineupTargetEnvironment>( getLineupInfo().getLineup().getTargetEnvironments() );

        if ( selected.isEmpty() && !isFormMode() )
        {
            // only restore previous selection in wizard mode
            List<IP2LineupTargetEnvironment> all = getAllEnvironments();

            IDialogSettings pluginSettings = Activator.getDefault().getDialogSettings();
            String[] selectedIndexes = pluginSettings.getArray( LINEUP_PLATFORMS );
            if ( selectedIndexes != null )
            {
                for ( String index : selectedIndexes )
                {
                    int ind = Integer.parseInt( index );
                    if ( ind > all.size() - 1 || ind < 0 )
                    {
                        continue;
                    }
                    IP2LineupTargetEnvironment env = all.get( ind );
                    selected.add( env );
                }
            }
            else
            {
                selected.addAll( all );
            }
        }
        viewer.setCheckedElements( selected.toArray() );
    }

    private void updateHeap( NexusLineupPublishingInfo info )
    {
        IP2LineupP2Advice advice = getLineupInfo().getLineup().getP2Advice();
        if ( advice != null )
        {
            for ( String s : advice.getAdvices() )
            {
                if ( s.startsWith( HEAP_ADVICE_PREFIX ) )
                {
                    heapSizeText.setText( s.substring( HEAP_ADVICE_PREFIX.length(),
                                                       s.length() - HEAP_ADVICE_SUFFIX.length() ) );
                    break;
                }
            }
        }
    }

    private List<IP2LineupTargetEnvironment> getAllEnvironments()
    {
        return EnvironmentLabelProvider.getSupportedEnvironments();
    }

    @Override
    public void dispose()
    {
        if ( !isFormMode() )
        {
            // only save selection in wizard mode
            IDialogSettings pluginSettings = Activator.getDefault().getDialogSettings();
            Collection<IP2LineupTargetEnvironment> selected = getLineupInfo().getLineup().getTargetEnvironments();
            if ( !selected.isEmpty() )
            {
                List<IP2LineupTargetEnvironment> all = getAllEnvironments();
                String[] indexes = new String[selected.size()];
                int i = 0;
                for ( IP2LineupTargetEnvironment env : selected )
                {
                    indexes[i++] = String.valueOf( all.indexOf( env ) );
                }
                pluginSettings.put( LINEUP_PLATFORMS, indexes );
            }
        }

        super.dispose();
    }
}
