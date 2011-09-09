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
package com.sonatype.nexus.proxy.p2.its.nxcm1923LineupUsesUpdateSiteLocal;

import com.sonatype.nexus.proxy.p2.its.AbstractNXCM1923LineupUsesUpdateSiteIT;

public class NXCM1923LineupUsesUpdateSiteLocalIT
    extends AbstractNXCM1923LineupUsesUpdateSiteIT
{
    @Override
    protected String getTestType()
    {
        return "local";
    }
}
