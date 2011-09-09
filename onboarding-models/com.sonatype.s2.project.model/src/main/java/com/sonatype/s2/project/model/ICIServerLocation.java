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

import java.util.List;

public interface ICIServerLocation
    extends IResourceLocation
{

    /**
     * list of watched hudson job names
     * @return list of job names, can be empty but not null
     */
    public List<String> getJobs();
}
