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

import java.io.InputStream;
import java.io.OutputStream;

import com.sonatype.nexus.p2.rest.model.P2LineupErrorResponse;
import com.sonatype.nexus.p2.rest.model.P2LineupSummaryDto;
import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.thoughtworks.xstream.XStream;

public class P2LineupXstreamIO
{
    public P2Lineup readLineup( InputStream is )
    {
        XStream xs = newInitializedXstream();

        return (P2Lineup) xs.fromXML( is );
    }

    public void writeLineup( IP2Lineup lineup, OutputStream os )
    {
        XStream xs = newInitializedXstream();
        xs.toXML( lineup, os );
    }

    public String writeErrorResponse( P2LineupErrorResponse errorResponse )
    {
        XStream xs = newInitializedXstream();

        return xs.toXML( errorResponse );
    }

    public P2LineupErrorResponse readErrorResponse( InputStream is )
    {
        XStream xs = newInitializedXstream();

        return (P2LineupErrorResponse) xs.fromXML( is );
    }

    public P2LineupSummaryDto readLineupSummary( InputStream is )
    {
        XStream xs = newInitializedXstream();

        return (P2LineupSummaryDto) xs.fromXML( is );
    }

    private static XStream newInitializedXstream()
    {
        XStream xs = new XStream();
        XStreamUtil.initializeXStream( xs );
        // use the models classloader
        xs.setClassLoader( P2LineupXstreamIO.class.getClassLoader() );
        return xs;
    }
}
