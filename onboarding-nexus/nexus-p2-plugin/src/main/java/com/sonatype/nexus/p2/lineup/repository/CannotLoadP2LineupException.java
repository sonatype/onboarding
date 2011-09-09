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
package com.sonatype.nexus.p2.lineup.repository;

import org.sonatype.nexus.proxy.IllegalOperationException;

public class CannotLoadP2LineupException
    extends IllegalOperationException
{
    private static final long serialVersionUID = 4843118245681420894L;

    public CannotLoadP2LineupException( Throwable cause )
    {
        super( "Could not load the p2 lineup: " + cause.getMessage(), cause );
    }

    public CannotLoadP2LineupException( String message )
    {
        super( "Could not load the p2 lineup: " + message );
    }
}
