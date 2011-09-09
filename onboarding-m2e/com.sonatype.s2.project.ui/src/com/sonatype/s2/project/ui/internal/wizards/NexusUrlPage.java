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
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.ErrorHandlingUtils;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.ui.internal.Messages;
import com.sonatype.s2.project.ui.internal.composites.NexusUrlComposite;

public class NexusUrlPage
    extends WizardPage
{
    private final static Logger log = LoggerFactory.getLogger( NexusUrlPage.class );

    private SwtValidationGroup validationGroup;

    private WidthGroup widthGroup;

    private NexusUrlComposite nexusUrlComposite;

    private String nexusUrl;

    private int urlInputStyle;

    public NexusUrlPage()
    {
        this( null /* nexusUrl */, NexusUrlComposite.ALLOW_ANONYMOUS );
    }

    public NexusUrlPage( String nexusUrl, int urlInputStyle )
    {
        super( NexusUrlPage.class.getName() );
        this.urlInputStyle = urlInputStyle;
        this.nexusUrl = nexusUrl;

        validationGroup = SwtValidationGroup.create( SwtValidationUI.createUI( this ) );
        widthGroup = new WidthGroup();
    }

    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayout( new GridLayout() );
        composite.addControlListener( widthGroup );

        createNexusUrlComposite( composite );

        setControl( composite );

    }

    protected NexusUrlComposite createNexusUrlComposite( Composite parent )
    {
        nexusUrlComposite = new NexusUrlComposite( parent, widthGroup, validationGroup, nexusUrl, urlInputStyle );
        nexusUrlComposite.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );
        return nexusUrlComposite;
    }

    protected WidthGroup getWidthGroup()
    {
        return widthGroup;
    }

    protected SwtValidationGroup getValidationGroup()
    {
        return validationGroup;
    }

    public String getNexusUrl()
    {
        return nexusUrlComposite.getUrl();
    }

    protected NexusUrlComposite getNexusUrlComposite()
    {
        return nexusUrlComposite;
    }

    @Override
    public boolean canFlipToNextPage()
    {
        // subclassing pages can now do validation in getNextPage() and disallow page transitions if needed
        return isPageComplete();
    }

    @Override
    public IWizardPage getNextPage()
    {
        if ( checkNexus() )
        {
            return super.getNextPage();
        }
        else
        {
            return null;
        }
    }

    private boolean checkNexus()
    {
        Throwable exception = null;
        final IStatus[] status = new IStatus[1];

        nexusUrl = nexusUrlComposite.getUrl();

        try
        {
            getContainer().run( true, true, new IRunnableWithProgress()
            {

                public void run( IProgressMonitor monitor )
                    throws InvocationTargetException, InterruptedException
                {
                    monitor.beginTask( Messages.nexusUrlComposite_validating, 2 );

                    status[0] = checkNexus( nexusUrl, monitor );

                    monitor.done();
                }
            } );
        }
        catch ( InvocationTargetException e )
        {
            exception = e.getTargetException();
        }
        catch ( InterruptedException e )
        {
            exception = e;
        }

        String message = Messages.nexusUrlComposite_validationFailed;
        if ( status[0] != null )
        {
            if ( status[0].isOK() )
            {
                setMessage( null, ERROR );
                return true;
            }
            String msg = status[0].getException() != null ? convertExceptionToUIText( status[0].getException() ) : null;
            message = msg != null ? msg : status[0].getMessage();
        }
        else if ( exception != null )
        {
            message = convertExceptionToUIText( exception );
            log.error( message, exception );
        }

        setMessage( message, IMessageProvider.ERROR );

        return false;
    }

    protected String getMessageForUnauthorizedException()
    {
        return null;
    }

    protected String getMessageForForbiddenException()
    {
        return null;
    }

    protected String getMessageForNotFoundException()
    {
        return null;
    }

    protected String convertExceptionToUIText( Throwable e )
    {
        String message =
            ErrorHandlingUtils.convertNexusIOExceptionToUIText( e, getMessageForUnauthorizedException(),
                                                                   getMessageForForbiddenException(),
                                                                   getMessageForNotFoundException() );
        if ( message == null )
        {
            return e.getMessage();
        }
        return message;
    }

    public IStatus checkNexus( String url, IProgressMonitor monitor )
    {
        IStatus status = nexusUrlComposite.checkNexus( monitor );

        if ( status.isOK() )
        {
            try
            {
                nexusUrlComposite.saveNexusUrl( monitor );
            }
            catch ( CoreException e )
            {
                status = e.getStatus();
            }
        }

        return status;
    }
}
