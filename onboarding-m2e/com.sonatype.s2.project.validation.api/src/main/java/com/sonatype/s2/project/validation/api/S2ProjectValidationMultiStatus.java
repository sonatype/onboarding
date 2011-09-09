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
import org.eclipse.core.runtime.MultiStatus;

public class S2ProjectValidationMultiStatus
    extends MultiStatus
    implements IS2ProjectValidationStatus
{
    private final IS2ProjectValidator validator;

    public IS2ProjectValidator getValidator()
    {
        return validator;
    }

    public S2ProjectValidationMultiStatus( IS2ProjectValidator validator, int code,
                                           IStatus[] newChildren, String message,
                                           Throwable exception )
    {
        super( validator.getPluginId(), code, newChildren, message, exception );
        this.validator = validator;
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
