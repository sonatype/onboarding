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

public interface IS2ProjectValidationStatus
    extends IStatus
{
    /**
     * @return the validator that returned this status from its validate method. Must not be null.
     */
    IS2ProjectValidator getValidator();

    /**
     * Sets the status code.
     * 
     * @param code the plug-in-specific status code, or <code>OK</code>
     */
    void setCode( int code );
}
