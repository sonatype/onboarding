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
package com.sonatype.s2.publisher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class S2PublisherException
    extends CoreException
{
    private static final long serialVersionUID = -6576300875393216498L;

    public S2PublisherException( String message )
    {
        super( new Status( IStatus.ERROR, Activator.PLUGIN_ID, message ) );
    }

    public S2PublisherException( String message, Exception e )
    {
        super( new Status( IStatus.ERROR, Activator.PLUGIN_ID, message, e ) );
    }
}
