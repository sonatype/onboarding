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
package com.sonatype.nexus.p2.facade.internal;


public class P2FacadeInternalException
    extends RuntimeException
{
    private static final long serialVersionUID = 5738694984841518793L;

    public P2FacadeInternalException( String message )
    {
        super( message );
    }

    public P2FacadeInternalException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
