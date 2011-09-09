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
package com.sonatype.s2.project.model;

public class CodebaseHelper
{
    private static final String[] CODEBASE_URL_PREFIXES = { IS2Project.PROJECT_REPOSITORY_PATH + "/",
        "/service/local/repositories/" + IS2Project.PROJECT_REPOSITORY_ID + "/" };

    public static String getNexusServerUrlFromCodebaseUrl( String codebaseUrl )
    {
        if ( codebaseUrl == null )
        {
            throw new IllegalStateException( "The codebase URL is null" );
        }
        codebaseUrl = codebaseUrl.trim();
        if ( codebaseUrl.length() == 0 )
        {
            throw new IllegalStateException( "The codebase URL is empty" );
        }
        
        for ( String codebaseUrlPrefix : CODEBASE_URL_PREFIXES )
        {
            int at = codebaseUrl.indexOf( codebaseUrlPrefix );
            if ( at > 0 )
            {
                return codebaseUrl.substring( 0, at );
            }
        }

        throw new IllegalArgumentException( "The URL '" + codebaseUrl + "' is not a codebase descriptor URL" );
    }
}
