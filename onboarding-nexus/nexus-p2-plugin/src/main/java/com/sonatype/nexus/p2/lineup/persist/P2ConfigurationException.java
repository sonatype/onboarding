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

public class P2ConfigurationException
    extends AbstractP2LineupException
{
    private static final long serialVersionUID = -1470496516399276560L;

    public P2ConfigurationException( String message )
    {
        super( message );
    }

    public P2ConfigurationException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
