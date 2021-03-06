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
package com.sonatype.s2.utils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class BackgroundProcessingQueue
    extends GenericBackgroundProcessingQueue<BackgroundProcessingQueue.Request>
{

    public BackgroundProcessingQueue( String name )
    {
        super( name );
    }

    public static abstract class Request
    {
        public abstract void run( IProgressMonitor monitor )
            throws CoreException;
    }

    @Override
    protected void process( Request request, IProgressMonitor monitor )
        throws CoreException
    {
        request.run( monitor );
    }
}
