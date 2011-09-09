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
package com.sonatype.s2.project.ui.lineup.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.extractor.P2InstallationDiscoveryResult;
import com.sonatype.s2.p2lineup.model.IP2LineupInstallableUnit;
import com.sonatype.s2.p2lineup.model.IP2LineupSourceRepository;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupInstallableUnit;
import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.project.ui.lineup.composites.RuntimeEnvironmentComposite;
import com.sonatype.s2.publisher.S2PublisherConstants;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;

public class RuntimeEnvironmentPage
    extends WizardPage
{
    private Logger log = LoggerFactory.getLogger( RuntimeEnvironmentPage.class );

    private RuntimeEnvironmentComposite runtimeEnvironmentComposite;

    private Button importButton;

    private NexusLineupPublishingInfo info;

    private SwtValidationGroup validationGroup;

    private WidthGroup widthGroup;

    private boolean importDone = false;

    public RuntimeEnvironmentPage( NexusLineupPublishingInfo info )
    {
        super( RuntimeEnvironmentPage.class.getName() );

        this.info = info;
        validationGroup = SwtValidationGroup.create( SwtValidationUI.createUI( this ) );
        widthGroup = new WidthGroup();

        setDescription( Messages.runtimeEnvironmentComposite_description );
        setTitle( Messages.runtimeEnvironmentComposite_title );
    }

    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        GridLayout layout = new GridLayout();
        composite.setLayout( layout );
        composite.addControlListener( widthGroup );

        runtimeEnvironmentComposite = new RuntimeEnvironmentComposite( composite, widthGroup, validationGroup, null );
        runtimeEnvironmentComposite.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );
        runtimeEnvironmentComposite.setLineupInfo( info );

        importButton = new Button( composite, SWT.CHECK );
        importButton.setText( Messages.runtimeEnvironmentPage_importFromEclipse_checkbox );
        GridData gd = new GridData( SWT.LEFT, SWT.TOP, false, false );
        gd.horizontalIndent = layout.marginWidth;
        importButton.setLayoutData( gd );
        importButton.setSelection( true );

        setControl( composite );
    }

    public boolean isImporting()
    {
        return importButton.getSelection() && !importDone;
    }

    public boolean performImport()
    {
        if ( importDone )
        {
            return true;
        }

        try
        {
            final Exception[] wrappedEx = new Exception[1];
            getContainer().run( true, true, new IRunnableWithProgress()
            {

                public void run( IProgressMonitor monitor )
                    throws InvocationTargetException, InterruptedException
                {
                    monitor.beginTask( Messages.runtimeEnvironmentPage_importJob, 2 );
                    monitor.worked( 1 );

                    P2InstallationDiscoveryResult result;
                    try
                    {
                        result = info.getP2().discoverInstallation( monitor );
                        P2Lineup lineup = info.getLineup();

                        // add installable units required to be able to use the lineup
                        // for project materialization. users will be able to remove these
                        lineup.addRootInstallableUnit( newIU( Messages.runtimeEnvironmentPage_materializationIU,
                                                              S2PublisherConstants.PROJECT_MATERIALIZER_IU_ID, "0.0.0" ) ); //$NON-NLS-1$

                        for ( IP2LineupInstallableUnit iu : result.getRootIUs() )
                        {
                            lineup.addRootInstallableUnit( iu );
                        }

                        for ( IP2LineupSourceRepository repo : result.getSourceRepositories() )
                        {
                            lineup.addRepository( repo );
                        }

                    }
                    catch ( CoreException e )
                    {
                        wrappedEx[0] = e;
                    }
                    finally
                    {
                        monitor.done();
                        importDone = true;
                    }

                }
            } );
            if ( wrappedEx[0] != null )
            {
                String message = NLS.bind( Messages.runtimeEnvironmentPage_importFailed, wrappedEx[0].getMessage() );
                log.error( message, wrappedEx[0] );
                setErrorMessage( wrappedEx[0].getMessage() != null ? wrappedEx[0].getMessage() : message );
                importDone = false;
                return false;
            }
            else
            {
                return true;
            }
        }
        catch ( Exception e )
        {
            String message = NLS.bind( Messages.runtimeEnvironmentPage_importFailed, e.getMessage() );
            log.error( message, e );
            importDone = false;
            setErrorMessage( e.getMessage() != null ? e.getMessage() : message );
            return false;
        }
    }

    private static IP2LineupInstallableUnit newIU( String name, String id, String version )
    {
        P2LineupInstallableUnit iu = new P2LineupInstallableUnit();
        iu.setId( id );
        iu.setVersion( version );
        iu.setName( name );
        return iu;
    }

    @Override
    public IWizardPage getNextPage()
    {
        if ( !isImporting() || performImport() )
        {
            return super.getNextPage();
        }
        else
        {
            return null;
        }
    }

    @Override
    public boolean canFlipToNextPage()
    {
        // subclassing pages can now do validation in getNextPage() and dissallow page transitions if needed
        return isPageComplete();
    }
}
