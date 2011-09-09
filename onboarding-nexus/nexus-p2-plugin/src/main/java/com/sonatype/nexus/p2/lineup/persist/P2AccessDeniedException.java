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
package com.sonatype.nexus.p2.lineup.persist;

import org.sonatype.nexus.proxy.access.Action;

public class P2AccessDeniedException
    extends Exception
{
    private static final long serialVersionUID = 5838826242592448390L;

    public P2AccessDeniedException( P2Gav gav, Action action )
    {
        super( "Current user does not have access to " + action + " the P2 lineup: " + gav.toString() );
    }

    public P2AccessDeniedException( P2Gav gav, Action action, Throwable t )
    {
        super( "Current user does not have access to " + action + " the P2 lineup: " + gav.toString(), t );
    }
}
