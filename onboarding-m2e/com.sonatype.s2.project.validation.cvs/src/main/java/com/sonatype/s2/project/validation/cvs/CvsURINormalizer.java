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
package com.sonatype.s2.project.validation.cvs;

import java.net.URI;
import java.net.URISyntaxException;

import org.maven.ide.eclipse.authentication.IURINormalizer;

import com.sonatype.s2.project.validation.cvs.internal.CVSURI;

public class CvsURINormalizer
    implements IURINormalizer
{
    private static final String CVS_SCHEME = "cvs";
    public boolean accept( String sUri )
        throws URISyntaxException
    {
        if ( sUri == null )
        {
            return false;
        }
        return sUri.startsWith( CVS_SCHEME ) || sUri.startsWith( CvsAccessValidator.CVS_SCM_ID );
    }

    public String normalize( String sUri )
        throws URISyntaxException
    {
        if ( !accept( sUri ) )
            return sUri;
        if (sUri.startsWith( CvsAccessValidator.CVS_SCM_ID ))
            sUri = sUri.substring( CvsAccessValidator.CVS_SCM_ID.length() );
        CVSURI uri = CVSURI.fromUri( new URI( sUri ) );

        return new URI( CVS_SCHEME, "//" + uri.getRepositoryName(), null ).toString();
    }

}
