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
package com.sonatype.s2.installer.internal.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;

public class SWTRunnableWithProgressWrapper
    implements IRunnableWithProgress
{

    private final Display display;

    private final IRunnableWithProgress operation;

    public SWTRunnableWithProgressWrapper( Display display, IRunnableWithProgress operation )
    {
        this.display = display;
        this.operation = operation;
    }

    public void run( IProgressMonitor monitor )
        throws InvocationTargetException, InterruptedException
    {
        monitor = new SWTProgressMonitorWrapper( display, monitor );

        operation.run( monitor );
    }

}
