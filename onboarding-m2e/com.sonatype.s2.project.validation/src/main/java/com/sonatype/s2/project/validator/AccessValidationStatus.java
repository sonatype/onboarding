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
package com.sonatype.s2.project.validator;

import org.eclipse.core.runtime.IStatus;

import com.sonatype.s2.project.model.IUrlLocation;
import com.sonatype.s2.project.validation.api.IS2ProjectValidator;
import com.sonatype.s2.project.validation.api.S2ProjectValidationMultiStatus;

/**
 * This status implementation holds a resource location, so it can be tracked back to the misconfigured realm.
 */
public class AccessValidationStatus
    extends S2ProjectValidationMultiStatus
{
    public AccessValidationStatus( IS2ProjectValidator validator, IStatus status, IUrlLocation location )
    {
        super( validator, status.getCode(), new IStatus[] { status }, status.getMessage(), status.getException() );
        this.location = location;
    }

    private IUrlLocation location;

    public IUrlLocation getLocation()
    {
        return location;
    }
}
