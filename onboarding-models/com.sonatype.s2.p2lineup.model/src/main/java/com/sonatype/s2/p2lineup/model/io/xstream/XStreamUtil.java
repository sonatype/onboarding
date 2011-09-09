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
package com.sonatype.s2.p2lineup.model.io.xstream;

import com.sonatype.nexus.p2.rest.model.P2LineupError;
import com.sonatype.nexus.p2.rest.model.P2LineupErrorResponse;
import com.sonatype.nexus.p2.rest.model.P2LineupListResponse;
import com.sonatype.nexus.p2.rest.model.P2LineupRepositoryError;
import com.sonatype.nexus.p2.rest.model.P2LineupSummaryDto;
import com.sonatype.nexus.p2.rest.model.P2LineupUnresolvedInstallableUnit;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupInstallableUnit;
import com.sonatype.s2.p2lineup.model.P2LineupSourceRepository;
import com.sonatype.s2.p2lineup.model.P2LineupTargetEnvironment;
import com.thoughtworks.xstream.XStream;

/**
 * Utility class to initialize xstream and unmarshal an object.
 * 
 * @author bdemers
 */
public class XStreamUtil
{

    public static XStream initializeXStream( XStream xstream )
    {
        xstream.processAnnotations( P2Lineup.class );
        xstream.processAnnotations( P2LineupInstallableUnit.class );
        xstream.processAnnotations( P2LineupSourceRepository.class );
        xstream.processAnnotations( P2LineupTargetEnvironment.class );

        xstream.processAnnotations( P2LineupErrorResponse.class );
        xstream.processAnnotations( P2LineupError.class );
        xstream.processAnnotations( P2LineupRepositoryError.class );
        xstream.processAnnotations( P2LineupUnresolvedInstallableUnit.class );

        xstream.processAnnotations( P2LineupListResponse.class );
        xstream.processAnnotations( P2LineupSummaryDto.class );

        xstream.omitField( P2Lineup.class, "modelEncoding" );
        
        return xstream;
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T unmarshal( String data, XStream xstream, T expectedType )
    {
        return (T) xstream.fromXML( data, expectedType );
    }

}
