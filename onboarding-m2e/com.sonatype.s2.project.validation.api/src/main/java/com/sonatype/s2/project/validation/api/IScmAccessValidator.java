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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public interface IScmAccessValidator
{

    String EXTENSION_POINT_ID = "com.sonatype.s2.project.validation.IScmAccessValidator";

    int getPriority();

    /**
     * @return true if this validator can be used to validate scm type.
     */
    boolean accept( String type );

    /**
     * @return true if this validator can be used to validate the specified location.
     */
    boolean accept( IScmAccessData data );

    /**
     * Validates access to the specified source repository.
     */
    IStatus validate( IScmAccessData data, IProgressMonitor monitor );

}
