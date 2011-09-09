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


public abstract class AbstractP2LineupException
    extends Exception
{
    private static final long serialVersionUID = 1455370083405339934L;

    protected AbstractP2LineupException( String message )
    {
        super( message );
    }

    public AbstractP2LineupException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
