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
package com.sonatype.s2.installer.internal;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;

public class S2InstallerErrorHandler
{
    private static boolean initialized = false;

    private static Logger log;

    private static String logFile;

    public static synchronized void initialize( Logger log_, String logFile_ )
    {
        log = log_;
        logFile = logFile_;

        initialized = true;
    }

    public static void handleError( String title, Throwable error )
    {
        if ( !initialized )
        {
            throw new RuntimeException( "S2InstallerErrorHandler was not initialized" );
        }

        if ( title == null )
        {
            title = error.getMessage();
        }

        // Log the error as is before doing any fancy stuff
        if ( log != null )
        {
            log.error( title, error );
        }
        else
        {
            System.err.println( title );
            error.printStackTrace();
        }

        if ( error instanceof InvocationTargetException )
        {
            error = error.getCause();
        }

        IStatus status;
        if ( error instanceof CoreException )
        {
            status = ( (CoreException) error ).getStatus();
        }
        else
        {
            status = new Status( IStatus.ERROR, Activator.PLUGIN_ID, error.getMessage(), error );
        }

        handleError( title, status );
    }

    public static void handleError( String title, IStatus status )
    {
        if ( !initialized )
        {
            throw new RuntimeException( "S2InstallerErrorHandler was not initialized" );
        }

        if ( log != null )
        {
            log.error( status.toString() );
        }
        else
        {
            System.err.println( status );
        }

        Shell shell = Display.getDefault().getActiveShell();

        ErrorDialog dialog = new ErrorDialog( shell, title, null /* message */, status, IStatus.ERROR )
        {
            @Override
            protected Control createDialogArea( Composite parent )
            {
                Composite c = (Composite) super.createDialogArea( parent );

                final Text logText = new Text( c, SWT.NONE | SWT.WRAP | SWT.READ_ONLY );
                logText.setLayoutData( new GridData( SWT.FILL, SWT.BOTTOM, true, false, 2, 1 ) );
                logText.setText( "For more details, see the log file: " + logFile );
                logText.setBackground( logText.getDisplay().getSystemColor( SWT.COLOR_WIDGET_BACKGROUND ) );

                // hide the blinking cursor
                logText.addFocusListener( new FocusListener()
                {
                    public void focusGained( FocusEvent e )
                    {
                        logText.removeFocusListener( this );
                        Button ok = getButton( IDialogConstants.OK_ID );
                        if ( ok != null )
                        {
                            ok.setFocus();
                        }
                    }

                    public void focusLost( FocusEvent e )
                    {
                    }
                } );

                return c;
            }
        };
        dialog.open();
    }
}
