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
package com.sonatype.s2.project.core.ide;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.sonatype.s2.project.core.internal.S2ProjectPlugin;
import com.sonatype.s2.project.model.IP2LineupLocation;
import com.sonatype.s2.project.validation.api.S2ProjectValidationException;

public interface IIDEUpdater
{
    public final String UP_TO_DATE = "UP_TO_DATE";

    public final String NOT_UP_TO_DATE = "NOT_UP_TO_DATE";

    public final String UNKNOWN = "UNKNOWN";

    public final String NOT_LINEUP_MANAGED = "NOT_LINEUP_MANAGED";
    
    public final String ERROR = "ERROR";
    
    public static final IStatus NOT_LINEUP_CREATED_STATUS = new Status( IStatus.OK, S2ProjectPlugin.PLUGIN_ID,
                                                                        "The IDE was not created from a lineup." );

    String isUpToDate( IP2LineupLocation location, IProgressMonitor monitor );

    public IStatus performUpdate( String toUpdateTo, IProgressMonitor monitor );

    boolean isLineupManaged( IProgressMonitor monitor )
        throws S2ProjectValidationException;
}
