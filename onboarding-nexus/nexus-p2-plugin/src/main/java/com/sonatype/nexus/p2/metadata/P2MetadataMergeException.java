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
package com.sonatype.nexus.p2.metadata;

public class P2MetadataMergeException
    extends Exception
{

    private static final long serialVersionUID = -2678683087210844545L;

    public P2MetadataMergeException( String message )
    {
        super( message );
    }

}
