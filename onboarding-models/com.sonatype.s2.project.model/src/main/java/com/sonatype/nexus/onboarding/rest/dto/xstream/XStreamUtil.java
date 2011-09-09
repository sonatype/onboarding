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
package com.sonatype.nexus.onboarding.rest.dto.xstream;

import com.sonatype.nexus.onboarding.rest.dto.CatalogDTO;
import com.sonatype.nexus.onboarding.rest.dto.CatalogEntryDTO;
import com.sonatype.nexus.onboarding.rest.dto.CatalogEntryRequest;
import com.sonatype.nexus.onboarding.rest.dto.CatalogRequest;
import com.sonatype.nexus.onboarding.rest.dto.CatalogResponse;
import com.sonatype.s2.securityrealm.model.S2SecurityRealm;
import com.sonatype.s2.securityrealm.model.S2SecurityRealmURLAssoc;
import com.thoughtworks.xstream.XStream;

/**
 * Utility class to initialize xstream and unmarshal an object.
 * 
 * @author velo
 */
public class XStreamUtil
{
    public static XStream initializeXStream( XStream xstream )
    {
        xstream.processAnnotations( CatalogEntryDTO.class );
        xstream.processAnnotations( CatalogEntryRequest.class );
        xstream.processAnnotations( CatalogDTO.class );
        xstream.processAnnotations( CatalogRequest.class );
        xstream.processAnnotations( CatalogResponse.class );
        
        xstream.processAnnotations( S2SecurityRealm.class );
        xstream.processAnnotations( S2SecurityRealmURLAssoc.class );

        return xstream;
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T unmarshal( String data, XStream xstream, T expectedType )
    {
        return (T) xstream.fromXML( data, expectedType );
    }
}
