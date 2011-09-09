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
package com.sonatype.s2.publisher.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.ui.common.ErrorHandlingUtils;

import com.sonatype.s2.project.ui.internal.wizards.NexusUrlPage;
import com.sonatype.s2.publisher.Activator;
import com.sonatype.s2.publisher.IS2Publisher;
import com.sonatype.s2.publisher.S2PublishRequest;
import com.sonatype.s2.publisher.internal.Messages;

public class PublishWizard
    extends Wizard
{
    private NexusUrlPage finalPage;

    private S2PublishRequest request;

    private IS2Publisher publisher;

    private String title;

    public PublishWizard( String title, IS2Publisher publisher, S2PublishRequest request )
    {
        setWindowTitle( title );
        setNeedsProgressMonitor( true );

        this.publisher = publisher;
        this.request = request;
        this.title = title;
    }

    @Override
    public void addPages()
    {
        finalPage = new NexusUrlPage();
        finalPage.setTitle( title );
        finalPage.setDescription( Messages.PublishWizardPage_description );
        addPage( finalPage );
    }

    @Override
    public boolean performFinish()
    {
        request.setNexusBaseUrl( finalPage.getNexusUrl() );

        final IStatus[] status = new IStatus[1];
        try
        {
            getContainer().run( true, true, new IRunnableWithProgress()
            {
                public void run( IProgressMonitor monitor )
                    throws InvocationTargetException, InterruptedException
                {
                    try
                    {
                        String url = finalPage.getNexusUrl();
                        status[0] = finalPage.checkNexus( url, monitor );
                        if ( status[0].isOK() )
                        {
                            publisher.publish( request, monitor );
                        }
                    }
                    catch ( IOException e )
                    {
                        status[0] = toStatus( e );
                    }
                    catch ( URISyntaxException e )
                    {
                        status[0] = toStatus( e );
                    }
                    catch ( CoreException e )
                    {
                        status[0] = toStatus( e );
                    }
                }
            } );
        }
        catch ( Exception e )
        {
            handle( e );

            return false;
        }
        if ( status[0].isOK() )
        {
            return true;
        }
        else
        {
            if ( status[0].isMultiStatus() && status[0].getChildren().length > 0 )
            {
                finalPage.setErrorMessage( status[0].getChildren()[0].getMessage() );
            }
            else
            {
                finalPage.setErrorMessage( status[0].getMessage() );
            }
            StatusManager.getManager().handle( status[0], StatusManager.LOG );
            return false;
        }

    }

    IStatus toStatus( Exception e )
    {
        String message =
            ErrorHandlingUtils.convertNexusIOExceptionToUIText( e, Messages.PublishWizard_error_auth,
                                                                NLS.bind( Messages.PublishWizard_error_forbidden,
                                                                          publisher.getName() ),
                                                                NLS.bind( Messages.PublishWizard_error_notfound,
                                                                          publisher.getName() ) );
        if ( message == null )
        {
            if ( e instanceof CoreException )
            {
                return ( (CoreException) e ).getStatus();
            }
            message = e.getMessage();
        }
        return new Status( IStatus.ERROR, Activator.PLUGIN_ID, message, e );
    }

    protected void handle( Exception e )
    {
        Throwable cause = e;

        if ( e instanceof InvocationTargetException )
        {
            cause = e.getCause();
        }

        IStatus status;

        if ( cause instanceof CoreException )
        {
            status = ( (CoreException) cause ).getStatus();
        }
        else
        {
            status =
                new Status( IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind( Messages.publishWizard_error,
                                                                          cause.getMessage() ), cause );
        }

        StatusManager.getManager().handle( status, StatusManager.LOG | StatusManager.BLOCK );
    }

    public void init( IWorkbench workbench, IStructuredSelection selection )
    {
        // TODO Auto-generated method stub
    }
}
