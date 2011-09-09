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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.ui.lineup.Activator;
import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.project.ui.lineup.composites.RemoteValidator;
import com.sonatype.s2.project.ui.lineup.composites.RepositoryComposite;
import com.sonatype.s2.project.ui.lineup.composites.RootIUComposite;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;

public class IURepoPage
    extends WizardPage
{

    public static final String REPOSITORY_COMPOSITE = "repositoryComposite";

    public static final String ROOT_IU_COMPOSITE = "rootIUComposite";

    private Logger log = LoggerFactory.getLogger( IURepoPage.class );

    private NexusLineupPublishingInfo info;

    private SwtValidationGroup validationGroup;

    private WidthGroup widthGroup;

    private RootIUComposite rootIUComposite;

    private RepositoryComposite repositoryComposite;

    public IURepoPage( NexusLineupPublishingInfo info )
    {
        super( IURepoPage.class.getName() );

        this.info = info;
        validationGroup = SwtValidationGroup.create( SwtValidationUI.createUI( this ) );
        widthGroup = new WidthGroup();

        setDescription( Messages.iuRepoPage_description );
        setTitle( Messages.iuRepoPage_title );
    }

    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayout( new GridLayout() );
        composite.addControlListener( widthGroup );

        Label iuLabel = new Label( composite, SWT.NONE );
        iuLabel.setText( Messages.iuRepoPage_installableUnits );

        rootIUComposite = new RootIUComposite( composite, widthGroup, validationGroup, null )
        {
            @Override
            protected void addUnit()
            {
                super.addUnit();
                repositoryComposite.updateViewer();
                validationGroup.performValidation();
            };
        };
        rootIUComposite.setData( "name", ROOT_IU_COMPOSITE );
        rootIUComposite.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );
        rootIUComposite.setLineupInfo( info );

        Label repoLabel = new Label( composite, SWT.NONE );
        repoLabel.setText( Messages.iuRepoPage_p2Repositories );

        repositoryComposite = new RepositoryComposite( composite, widthGroup, validationGroup, null )
        {
            protected void validateLineup()
            {
                IURepoPage.this.validateLineup();
            };
        };
        repositoryComposite.setData( "name", REPOSITORY_COMPOSITE );
        repositoryComposite.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );
        repositoryComposite.setLineupInfo( info );

        setControl( composite );
    }

    @Override
    public void setVisible( boolean visible )
    {
        super.setVisible( visible );
        if ( visible )
        {
            ( (Wizard) getWizard() ).setWindowTitle( Messages.newLineupWizard_title );
            update();
        }
    }

    private void update()
    {
        rootIUComposite.update();
        repositoryComposite.update();
    }

    private boolean validateLineup()
    {
        String error = null;
        int severity = IMessageProvider.NONE;
        setErrorMessage( null );
        final IStatus status[] = new IStatus[1];
        try
        {
            getContainer().run( true, true, new IRunnableWithProgress()
            {
                public void run( IProgressMonitor monitor )
                    throws InvocationTargetException, InterruptedException
                {
                    try
                    {
                        status[0] =
                            new RemoteValidator( repositoryComposite, rootIUComposite ).validate( info, monitor );
                    }
                    catch ( RuntimeException e )
                    {
                        log.error( e.getMessage(), e );
                        status[0] = new Status( IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e );
                    }
                }
            } );
            if ( !status[0].isOK() )
            {
                error = status[0].getMessage();
                severity = status[0].getSeverity() == IStatus.ERROR ? IMessageProvider.ERROR : IMessageProvider.WARNING;
            }
        }
        catch ( InvocationTargetException e )
        {
            Throwable t = e.getTargetException();
            error = t.getMessage();
            severity = IMessageProvider.ERROR;
            log.error( error, t );
        }
        catch ( InterruptedException e )
        {
            error = e.getMessage();
            severity = IMessageProvider.ERROR;
            log.error( error, e );
        }
        setMessage( error, severity );
        IURepoPage.this.update();

        return severity != IMessageProvider.ERROR;
    };

    @Override
    public boolean canFlipToNextPage()
    {
        // subclassing pages can now do validation in getNextPage() and disallow page transitions if needed
        return isPageComplete();
    }

    @Override
    public IWizardPage getNextPage()
    {
        if ( validateLineup() )
        {
            return super.getNextPage();
        }
        else
        {
            return null;
        }
    }
}
