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
package com.sonatype.s2.project.ui.internal.views;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.graphics.Image;

import com.sonatype.s2.project.ui.internal.Images;
import com.sonatype.s2.project.ui.internal.Messages;
import com.sonatype.s2.project.ui.internal.wizards.TableLabelProvider;
import com.sonatype.s2.project.validation.api.IS2ProjectValidationStatus;

public class ValidationStatusLabelProvider
    extends TableLabelProvider
{
    public final static int MESSAGE_COLUMN = 0;

    public final static int VALIDATOR_COLUMN = 1;

    public Image getColumnImage( Object element, int columnIndex )
    {
        if ( columnIndex == MESSAGE_COLUMN )
        {
            int severity = -1;
            if ( element instanceof IStatus )
            {
                severity = ( (IStatus) element ).getSeverity();
            }
            switch ( severity )
            {
                case IStatus.OK:
                    return Images.STATUS_OK;
                case IStatus.ERROR:
                    return Images.STATUS_ERROR;
                case IStatus.WARNING:
                    return Images.STATUS_WARNING;
                case IStatus.INFO:
                    return Images.STATUS_INFO;
            }
        }
        return null;
    }

    public String getColumnText( Object element, int columnIndex )
    {
        if ( element instanceof IStatus )
        {
            IStatus status = (IStatus) element;
            switch ( columnIndex )
            {
                case MESSAGE_COLUMN:
                    return extractMessage( status ).replaceAll( "[\r\n]", " " );
                case VALIDATOR_COLUMN:
                    return status instanceof IS2ProjectValidationStatus ? ( (IS2ProjectValidationStatus) status ).getValidator().getName()
                                    : "";
            }
        }

        return null;
    }

    public String extractMessage( IStatus status )
    {
        String s = status.getMessage();
        if ( s == null )
        {
            s = "";
        }

        if ( s.length() == 0 )
        {
            switch ( status.getSeverity() )
            {
                case IStatus.OK:
                    return Messages.status_ok;
                case IStatus.ERROR:
                    return Messages.status_error;
                case IStatus.WARNING:
                    return Messages.status_warning;
                case IStatus.INFO:
                    return Messages.status_information;
            }
        }
        return s;
    }
}
