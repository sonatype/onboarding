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
package com.sonatype.nexus.proxy.p2.its;

public class P2ITException
    extends Exception
{

    private static final long serialVersionUID = 1L;

    private int code;

    public int getCode()
    {
        return code;
    }

    public P2ITException( int code, StringBuffer buf )
    {
        super( "P2 return code was " + code + ":\n" + buf );
        this.code = code;
    }

}
