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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.internal.Activator;
import com.sonatype.s2.project.ui.internal.Messages;
import com.sonatype.s2.project.ui.internal.composites.ValidationStatusComposite;
import com.sonatype.s2.project.validation.api.IS2ProjectValidationStatus;
import com.sonatype.s2.project.validation.api.S2ProjectValidationContext;
import com.sonatype.s2.project.validator.ValidationFacade;

public class ProjectValidationPage
    extends WizardPage
{
    private Logger log = LoggerFactory.getLogger( ProjectValidationPage.class );
    private ValidationStatusComposite statusViewer;

    protected IS2Project project;

    private IStatus topStatus;

    private boolean remediationPossible = true;

    private int errors = 0;

    private Button ignoreErrorsButton;

    private Button remediateButton;

    private S2ProjectValidationContext validationContext;

    public ProjectValidationPage( IS2Project project, S2ProjectValidationContext validationContext, IStatus topStatus )
    {
        super( Messages.materializationWizard_validationPage_title );
        this.project = project;
        this.validationContext = validationContext;
        this.topStatus = topStatus;

        setTitle( Messages.materializationWizard_validationPage_title );
        setDescription( Messages.materializationWizard_validationPage_description );
        setPageComplete( false );
    }

    public void createControl( Composite parent )
    {
        createViewer( parent );

        setControl( statusViewer );
    }

    private void createIgnoreButton( Composite container )
    {
        ignoreErrorsButton = new Button( container, SWT.CHECK );
        ignoreErrorsButton.setText( Messages.materializationWizard_validationPage_ignoreValidationResults );
        ignoreErrorsButton.setEnabled( false );
        ignoreErrorsButton.setLayoutData( new GridData( SWT.FILL, SWT.BOTTOM, false, false ) );
        ignoreErrorsButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                if ( ignoreErrorsButton.getSelection() )
                {
                    // clear the errors if the user chooses to ignore them
                    setPageComplete( true );
                }
                else
                {
                    if ( getErrorMessage() != null )
                    {
                        setPageComplete( false );
                    }
                }
            }
        } );
    }

    private void createRemediationControls( Composite container )
    {
        remediateButton = new Button( container, SWT.NONE );
        remediateButton.setText( Messages.materializationWizard_validationPage_remediateButton );
        remediateButton.setEnabled( false );
        remediateButton.setLayoutData( new GridData( SWT.FILL, SWT.TOP, false, false ) );
        remediateButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                final Object selection = statusViewer.getSelection().getFirstElement();
                if ( selection instanceof IS2ProjectValidationStatus )
                {
                    try
                    {
                        final IStatus[] status = new IStatus[1];
                        getContainer().run( true, true, new IRunnableWithProgress()
                        {
                            public void run( final IProgressMonitor monitor )
                                throws InvocationTargetException, InterruptedException
                            {
                                Display.getDefault().syncExec( new Runnable()
                                {

                                    public void run()
                                    {
                                        status[0] =
                                            ( (IS2ProjectValidationStatus) selection ).getValidator().remediate( false,
                                                                                                                 monitor );
                                    }
                                } );
                            }
                        } );
                        if ( status[0] != null && status[0].isOK() )
                        {
                            internalValidate();
                        }
                    }
                    catch ( InvocationTargetException ex )
                    {
                        String msg =
                            NLS.bind( Messages.materializationWizard_validationPage_errors_couldNotRemediate,
                                      ex.getMessage() );
                        log.error( msg, ex );
                        setErrorMessage( msg );
                    }
                    catch ( InterruptedException ex )
                    {
                        String msg =
                            NLS.bind( Messages.materializationWizard_validationPage_errors_remediationInterrupted,
                                      ex.getMessage() );
                        log.error( msg, ex );
                        setErrorMessage( msg );
                    }
                }
            }
        } );
    }

    private void createViewer( Composite container )
    {
        statusViewer = new ValidationStatusComposite( container )
        {
            @Override
            protected void createButtons( Composite parent )
            {
                createRemediationControls( parent );
                super.createButtons( parent );
                createIgnoreButton( parent );
            }
        };

        statusViewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                Object selection = statusViewer.getSelection().getFirstElement();
                if ( selection instanceof IStatus )
                {
                    remediateButton.setEnabled( selection instanceof IS2ProjectValidationStatus
                        && ( (IS2ProjectValidationStatus) selection ).getValidator().canRemediate( false ).isOK() );
                }
            }
        } );

        statusViewer.setInput( topStatus );
    }

    @Override
    public void setVisible( boolean visible )
    {
        super.setVisible( visible );
        if ( visible && project != null )
        {
            if ( topStatus == null )
            {
                internalValidate();
            }
            else
            {
                checkForErrors();
            }
        }
    }

    public IStatus validate()
    {
        topStatus = null;
        if ( project == null )

        {
            return null;
        }
        try
        {
            final IStatus[] status = new IStatus[1];
            getContainer().run( true, true, new IRunnableWithProgress()
            {
                public void run( IProgressMonitor monitor )
                    throws InvocationTargetException, InterruptedException
                {
                    monitor.setTaskName( Messages.materializationWizard_validationPage_validatingProjectRequirements );

                    try
                    {
                        status[0] =
                            ValidationFacade.getInstance().validate( project, validationContext, monitor, false );
                    }
                    catch ( CoreException e )
                    {
                        status[0] = e.getStatus();
                    }
                }
            } );

            topStatus = status[0];
        }
        catch ( InvocationTargetException ex )
        {
            String msg =
                NLS.bind( Messages.materializationWizard_validationPage_errors_couldNotValidateProject, ex.getMessage() );
            log.error( msg, ex );
            topStatus = new Status( IStatus.ERROR, Activator.PLUGIN_ID, msg, ex );
        }
        catch ( InterruptedException ex )
        {
            String msg =
                NLS.bind( Messages.materializationWizard_validationPage_errors_projectValidationCanceled,
                          ex.getMessage() );
            log.error( msg, ex );
            topStatus = new Status( IStatus.ERROR, Activator.PLUGIN_ID, msg, ex );
        }

        return topStatus;
    }

    private void internalValidate()
    {
        topStatus = null;
        statusViewer.setInput( null );
        remediateButton.setEnabled( false );
        setErrorMessage( null );

        validate();

        checkForErrors();
    }

    private void checkForErrors()
    {
        boolean enableIgnoreErrors = false;
        remediationPossible = true;
        errors = 0;

        if ( !topStatus.isOK() )
        {
            checkStatus( topStatus );
        }

        if ( errors == 0 )
        {
            setMessage( Messages.materializationWizard_validationPage_validationSuccessful );
            setPageComplete( true );
        }
        else
        {
            if ( topStatus.isMultiStatus() )
            {
                setErrorMessage( remediationPossible ? Messages.materializationWizard_validationPage_errors_validationIncomplete
                                : Messages.materializationWizard_validationPage_errors_validationKaputt );
                enableIgnoreErrors = true;
            }
            else
            {
                setErrorMessage( topStatus.getMessage() );
            }
            setPageComplete( false );
        }

        statusViewer.setInput( topStatus );
        statusViewer.expandAll();
        ignoreErrorsButton.setEnabled( enableIgnoreErrors );
    }

    private void checkStatus( IStatus status )
    {
        if ( status.isMultiStatus() )
        {
            for ( IStatus child : status.getChildren() )
            {
                checkStatus( child );
            }
        }
        else if ( status.getSeverity() == IStatus.ERROR )
        {
            errors++;
            if ( status instanceof IS2ProjectValidationStatus )
            {
                IS2ProjectValidationStatus s2Status = (IS2ProjectValidationStatus) status;
                if ( s2Status.getValidator().canRemediate( false ).isOK() )
                {
                    // don't give up just yet
                    return;
                }
            }
            remediationPossible = false;
        }
    }
}
