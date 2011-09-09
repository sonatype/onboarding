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

public interface IEclipseInstallationLocation
{

    /**
     * Gets the directory to the local Eclipse installation.
     * 
     * @return The directory to the local Eclipse installation or {@code null} if not set.
     */
    public String getDirectory();

    /**
     * Indicates whether the user may change the location given by the project descriptor.
     * 
     * @return {@code true} if the user may change the location, {@code false} otherwise.
     */
    public boolean isCustomizable();

}
