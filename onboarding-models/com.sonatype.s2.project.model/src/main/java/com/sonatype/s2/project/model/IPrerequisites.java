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
package com.sonatype.s2.project.model;

public interface IPrerequisites
{

    /**
     * Gets the minimum amount of heap memory required for the Eclipse JVM to successfully handle the project. The
     * syntax of the value follows the {{-Xmx}} argument, i.e. a trailing 'k' or 'K' indicates kilobytes, a trailing 'm'
     * or 'M' megabytes and otherwise the value denotes bytes.
     * 
     * @return The minimum amount of required heap memory.
     */
    public String getRequiredMemory();

    public void setRequiredMemory( String memory );
}
