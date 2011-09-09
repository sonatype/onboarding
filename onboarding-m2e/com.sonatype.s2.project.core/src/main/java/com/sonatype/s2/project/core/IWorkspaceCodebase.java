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

import java.util.List;

import com.sonatype.s2.project.model.IS2Project;

public interface IWorkspaceCodebase
{
    public String getDescriptorUrl();

    public String getGroupId();

    public String getArtifactId();

    public String getVersion();

    public List<IWorkspaceSourceTree> getSourceTrees();

    public String getIsP2LineupUpToDate();
    
    public String getP2LineupLocation();
    
    public IWorkspaceCodebase getPending();

    public IS2Project getS2Project();
}
