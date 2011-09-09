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
package com.sonatype.s2.project.core;

import org.eclipse.core.runtime.IStatus;

import com.sonatype.s2.project.model.IS2ProjectCatalog;

public interface IS2ProjectCatalogRegistryEntry
{
    public boolean isLoaded();

    public String getUrl();

    public IS2ProjectCatalog getCatalog();

    public IStatus getStatus();
}
