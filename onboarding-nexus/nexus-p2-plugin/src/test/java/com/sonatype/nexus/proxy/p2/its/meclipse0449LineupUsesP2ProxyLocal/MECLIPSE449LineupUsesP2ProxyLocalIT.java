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
package com.sonatype.nexus.proxy.p2.its.meclipse0449LineupUsesP2ProxyLocal;

import com.sonatype.nexus.proxy.p2.its.AbstractMECLIPSE449LineupUsesP2ProxyIT;

/**
 * Some artifact repositories contain packed artifacts but cannot handle them correctly (i.e. they don't have rules for
 * packed format). This IT tests that if the same artifact is in both packed and unpacked formats in the repositories,
 * the p2 lineup resolution and the installation from the p2 lineup does not fail.
 */
public class MECLIPSE449LineupUsesP2ProxyLocalIT
    extends AbstractMECLIPSE449LineupUsesP2ProxyIT
{
    @Override
    protected String getTestType()
    {
        return "local";
    }
}
