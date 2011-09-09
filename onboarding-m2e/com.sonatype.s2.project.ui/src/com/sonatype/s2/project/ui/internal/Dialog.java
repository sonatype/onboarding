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
package com.sonatype.s2.project.ui.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

public class Dialog
    extends MessageDialog
{
    public Dialog( Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage,
                   int dialogImageType, String[] dialogButtonLabels, int defaultIndex )
    {
        super( parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels,
               defaultIndex );
    }

    public static void openCoreError( Shell shell, String title, CoreException e )
    {
        openStatusError( shell, title, e.getStatus() );
    }

    public static void openStatusError( Shell shell, String title, IStatus status )
    {
        StringBuffer sb = new StringBuffer();
        addStatus( sb, status, "" );
        openError( shell, title, sb.toString() );
    }
    
    private static void addStatus( StringBuffer sb, IStatus status, String indent ) {
        sb.append( indent );
        sb.append( status.getMessage() );
        sb.append( '\n' );
        if ( status.isMultiStatus() ) {
            for ( IStatus childStatus : status.getChildren() ) {
                addStatus(sb, childStatus, indent + "  " );
            }
        }
    }
}
