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


public class NoSuchP2LineupException
    extends AbstractP2LineupException
{
    private static final long serialVersionUID = -389190717787565643L;

    public NoSuchP2LineupException( P2Gav gav )
    {
        super( "P2 Lineup: '" + gav + "' could not be found." );
    }

    public NoSuchP2LineupException( P2Gav gav, Throwable cause )
    {
        super( "P2 Lineup: '" + gav + "' could not be found.", cause );
    }
}
