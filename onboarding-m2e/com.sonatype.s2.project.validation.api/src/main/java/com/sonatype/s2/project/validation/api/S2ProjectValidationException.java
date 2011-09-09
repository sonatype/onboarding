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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.sonatype.s2.project.validation.api.internal.S2ProjectValidationApiPlugin;

public class S2ProjectValidationException
    extends CoreException
{
    private static final long serialVersionUID = 6197321015922872048L;

    public S2ProjectValidationException( IStatus status )
    {
        super( status );
    }

    public S2ProjectValidationException( String message )
    {
        this( message, null );
    }

    public S2ProjectValidationException( String message, Throwable cause )
    {
        this( new Status( IStatus.ERROR, S2ProjectValidationApiPlugin.PLUGIN_ID, message, cause ) );
    }

    public S2ProjectValidationException( CoreException exception )
    {
        super( exception.getStatus() );
    }
}
