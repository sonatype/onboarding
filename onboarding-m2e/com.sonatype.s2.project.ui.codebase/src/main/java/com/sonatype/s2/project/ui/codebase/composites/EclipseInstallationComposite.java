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

import static org.maven.ide.eclipse.ui.common.FormUtils.nvl;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.authentication.IRealmChangeListener;
import org.maven.ide.eclipse.ui.common.authentication.RealmComposite;
import org.maven.ide.eclipse.ui.common.authentication.RealmUrlCollector;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.maven.ide.eclipse.ui.common.validation.SonatypeValidators;

import com.sonatype.s2.project.model.IEclipseInstallationLocation;
import com.sonatype.s2.project.model.IP2LineupLocation;
import com.sonatype.s2.project.model.IPrerequisites;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.descriptor.EclipseInstallationLocation;
import com.sonatype.s2.project.model.descriptor.P2LineupLocation;
import com.sonatype.s2.project.model.descriptor.Prerequisites;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.internal.Dialog;
import com.sonatype.s2.project.ui.lineup.wizard.SelectLineupWizard;

@SuppressWarnings( "restriction" )
public class EclipseInstallationComposite
    extends CodebaseComposite
{
    public static final int ENABLE_P2_LINEUP_CONTROLS = 1;

    private Text p2LineupLocationText;

    private Button p2LineupLocationButton;

    private RealmComposite p2LineupLocationRealmComposite;

    private Text eclipsePathText;

    private Button eclipsePathCheckbox;

    private Text requiredMemoryText;

    public EclipseInstallationComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                                         FormToolkit toolkit, int style )
    {
        super( parent, widthGroup, validationGroup, toolkit );

        setLayout( new GridLayout( 3, false ) );

        if ( ( style & ENABLE_P2_LINEUP_CONTROLS ) == ENABLE_P2_LINEUP_CONTROLS )
        {
            createLineupLocationControls();
        }
        createEclipsePathControls();
        createRequiredMemoryControls();
    }

    private void createLineupLocationControls()
    {
        createLabel( Messages.eclipseInstallationComposite_lineup_label );

        p2LineupLocationText = createText( "p2LineupLocationText", Messages.eclipseInstallationComposite_lineup_name ); //$NON-NLS-1$
        p2LineupLocationText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                saveP2LineupLocation();
            }
        } );
        addToValidationGroup( p2LineupLocationText, SonatypeValidators.EMPTY_OR_URL );

        p2LineupLocationButton = createButton( Messages.eclipseInstallationComposite_lineup_select );
        p2LineupLocationButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                String currentValue = p2LineupLocationText.getText();
                String lineupValue = null;
                if ( currentValue != null && currentValue.contains( "/content/" ) ) //$NON-NLS-1$
                {
                    lineupValue = currentValue.substring(currentValue.indexOf( "/content/repositories/nx-p2lineup/" ) + "/content/repositories/nx-p2lineup/".length());
                    if (lineupValue.endsWith("/"))
                    {                    	
                    	lineupValue = lineupValue.substring(0, lineupValue.length() - 1);
                    }
                    currentValue = currentValue.substring( 0, currentValue.indexOf( "/content/" ) ); //$NON-NLS-1$
                }
                else
                {
                    currentValue = null;
                }
                SelectLineupWizard wizard = new SelectLineupWizard( currentValue, lineupValue );
                WizardDialog dialog = new WizardDialog( getShell(), wizard ) {
					@Override
					protected void createButtonsForButtonBar(Composite parent) {
						super.createButtonsForButtonBar(parent);
						Button btn = getButton(IDialogConstants.FINISH_ID);
						btn.setText(IDialogConstants.OK_LABEL);
					}
                	
                };
                if ( dialog.open() == Dialog.OK )
                {
                    p2LineupLocationText.setText( wizard.getLineupUrl() );
                    saveP2LineupLocation();
                }
            }
        } );

        Label realmLabel = createLabel( Messages.eclipseInstallationComposite_securityRealm );
        GridData realmLabelData = (GridData) realmLabel.getLayoutData();
        realmLabelData.horizontalIndent = INPUT_INDENT;

        p2LineupLocationRealmComposite =
            new RealmComposite( this, p2LineupLocationText, getValidationGroup(), getToolkit() );
        p2LineupLocationRealmComposite.setLayoutData( createInputData( 2, 1 ) );
        p2LineupLocationRealmComposite.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                notifyCodebaseChangeListeners();
            }
        } );
    }

    private void saveP2LineupLocation()
    {
        String url = p2LineupLocationText.getText().trim();
        if ( url.length() == 0 )
        {
            getProject().setP2LineupLocation( null );
        }
        else
        {
            P2LineupLocation location = new P2LineupLocation();
            location.setUrl( url );
            getProject().setP2LineupLocation( location );
        }

        notifyCodebaseChangeListeners();
    }

    private void createEclipsePathControls()
    {
        createLabel( Messages.eclipseInstallationComposite_path_label );

        eclipsePathText = createText( SWT.NONE, 2, 1, "eclipsePathText", null ); //$NON-NLS-1$
        eclipsePathText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                saveEclipseInstallPath();
            }
        } );

        eclipsePathCheckbox =
            createCheckbox( Messages.eclipseInstallationComposite_path_checkbox, 3, 1, "eclipsePathCheckbox" ); //$NON-NLS-1$
        eclipsePathCheckbox.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                saveEclipseInstallPath();
            }
        } );
        eclipsePathCheckbox.setEnabled( false );
        eclipsePathCheckbox.setSelection( true );
    }

    private void saveEclipseInstallPath()
    {
        String path = eclipsePathText.getText().trim();
        if ( path.length() == 0 )
        {
            eclipsePathCheckbox.setEnabled( false );
            getProject().setEclipseInstallationLocation( null );
        }
        else
        {
            eclipsePathCheckbox.setEnabled( true );
            EclipseInstallationLocation location = new EclipseInstallationLocation();
            location.setDirectory( path );
            location.setCustomizable( eclipsePathCheckbox.getSelection() );
            getProject().setEclipseInstallationLocation( location );
        }
        notifyCodebaseChangeListeners();
    }

    private void createRequiredMemoryControls()
    {
        createLabel( Messages.eclipseInstallationComposite_memory_label );

        requiredMemoryText = createText( "requiredMemoryText", null ); //$NON-NLS-1$
        requiredMemoryText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                String requiredMemory = requiredMemoryText.getText().trim();
                if ( requiredMemory.length() == 0 )
                {
                    getProject().setPrerequisites( null );
                }
                else
                {
                    IPrerequisites prerequisites = new Prerequisites();
                    prerequisites.setRequiredMemory( requiredMemory + 'M' );
                    getProject().setPrerequisites( prerequisites );
                }
                notifyCodebaseChangeListeners();
            }
        } );
        GridData gd = (GridData) requiredMemoryText.getLayoutData();
        gd.grabExcessHorizontalSpace = false;
        gd.horizontalAlignment = SWT.LEFT;
        gd.widthHint = 50;

        requiredMemoryText.addVerifyListener( new VerifyListener()
        {
            public void verifyText( VerifyEvent e )
            {
                e.doit = Character.isISOControl( e.character ) || Character.isDigit( e.character );
            }
        } );
    }

    @Override
    protected void update( IS2Project project )
    {
        IPrerequisites prerequisites = project.getPrerequisites();
        requiredMemoryText.setText( prerequisites == null ? "" : prerequisites.getRequiredMemory().replaceAll( "\\D", //$NON-NLS-1$ //$NON-NLS-2$
                                                                                                               "" ) ); //$NON-NLS-1$

        IEclipseInstallationLocation eclipseLocation = project.getEclipseInstallationLocation();
        if ( eclipseLocation == null )
        {
            eclipsePathText.setText( "" ); //$NON-NLS-1$
            eclipsePathCheckbox.setSelection( true );
            eclipsePathCheckbox.setEnabled( false );
        }
        else
        {
            eclipsePathText.setText( eclipseLocation.getDirectory() );
            eclipsePathCheckbox.setSelection( eclipseLocation.isCustomizable() );
            eclipsePathCheckbox.setEnabled( true );
        }

        if ( p2LineupLocationText != null )
        {

            IP2LineupLocation p2LineupLocation = project.getP2LineupLocation();
            if ( p2LineupLocation != null )
            {
                p2LineupLocationText.setText( nvl( p2LineupLocation.getUrl() ) );
            }
            else
            {
                p2LineupLocationText.setText( "" ); //$NON-NLS-1$
            }
        }
    }

    public void addRealmChangeListener( IRealmChangeListener listener )
    {
        if ( p2LineupLocationRealmComposite != null )
        {
            p2LineupLocationRealmComposite.addRealmChangeListener( listener );
        }
    }

    public void saveRealms( RealmUrlCollector realmUrlCollector )
    {
        if ( p2LineupLocationRealmComposite != null )
        {
            realmUrlCollector.collect( p2LineupLocationRealmComposite );
        }
    }
}
