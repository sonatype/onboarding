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
package com.sonatype.s2.publisher.nexus;

import com.sonatype.s2.extractor.P2MetadataAdapter;
import com.sonatype.s2.p2lineup.model.P2Lineup;
import com.sonatype.s2.publisher.nexus.NexusLineupPublisher;

public class NexusLineupPublishingInfo
{
    private String serverUrl;

    private P2Lineup lineup;

    private final NexusLineupPublisher publisher = new NexusLineupPublisher();

    private final P2MetadataAdapter p2 = new P2MetadataAdapter();

    public NexusLineupPublishingInfo()
    {
        this( new P2Lineup() );
    }

    public NexusLineupPublishingInfo( P2Lineup lineup )
    {
        this.lineup = lineup;
    }

    public void setServerUrl( String serverUrl )
    {
        this.serverUrl = serverUrl;
    }

    public String getServerUrl()
    {
        return serverUrl;
    }

    public P2Lineup getLineup()
    {
        return lineup;
    }

    public NexusLineupPublisher getPublisher()
    {
        return publisher;
    }

    public P2MetadataAdapter getP2()
    {
        return p2;
    }

}
