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
package com.sonatype.s2.project.validation.api;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class S2ProjectValidationStatus
    extends Status
    implements IS2ProjectValidationStatus
{
    private final IS2ProjectValidator validator;

    public IS2ProjectValidator getValidator()
    {
        return validator;
    }

    public S2ProjectValidationStatus( IS2ProjectValidator validator, int severity, String message )
    {
        this( validator, severity, Status.OK /* code */, message, null /* exception */);
    }

    public S2ProjectValidationStatus( IS2ProjectValidator validator, int severity, int code,
                                      String message, Throwable exception )
    {
        super( severity, validator.getPluginId(), code, message, exception );
        this.validator = validator;
    }

    public S2ProjectValidationStatus( IS2ProjectValidator validator, IStatus status )
    {
        super( status.getSeverity(), status.getPlugin(), status.getCode(), status.getMessage(), status.getException() );
        this.validator = validator;
    }

    public static IS2ProjectValidationStatus getOKStatus( IS2ProjectValidator validator )
    {
        return new S2ProjectValidationStatus( validator, Status.OK_STATUS );
    }

    public static IS2ProjectValidationStatus getOKStatus( IS2ProjectValidator validator, String message )
    {
        return new S2ProjectValidationStatus( validator, Status.OK, message );
    }

    @Override
    public String toString()
    {
        return "Validator " + getValidator().getClass().getCanonicalName() + ": " + super.toString();
    }

    @Override
    public void setCode( int code )
    {
        super.setCode( code );
    }
}
