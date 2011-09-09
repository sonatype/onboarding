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
package com.sonatype.nexus.p2.proxy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.proxy.mirror.DefaultDownloadMirrors;
import org.sonatype.nexus.proxy.mirror.DownloadMirrorSelector;
import org.sonatype.nexus.proxy.repository.Mirror;

class P2ProxyDownloadMirrors
    extends DefaultDownloadMirrors
{

    public P2ProxyDownloadMirrors()
    {
        super( null );
    }

    private Map<String, Mirror> mirrorMap = new LinkedHashMap<String, Mirror>();

    @Override
    public DownloadMirrorSelector openSelector( String mirrorOfUrl )
    {
        return new P2DownloadMirrorSelector( this, mirrorOfUrl );
    }

    @Override
    public List<Mirror> getMirrors()
    {
        return new ArrayList<Mirror>( mirrorMap.values() );
    }

    @Override
    public void setMirrors( List<Mirror> mirrors )
    {
        this.mirrorMap.clear();
        for ( Mirror mirror : mirrors )
        {
            this.mirrorMap.put( mirror.getId(), mirror );
        }
        this.setMaxMirrors( mirrors.size() );
    }

    public void addMirror( Mirror mirror )
    {
        if ( mirror.getId() == "default" )
        {
            // This is a mirror added by nexus by default and
            // it "points" to the remote URL for the current p2 proxy repository.
            // We do not want this mirror... so, ignore it.
            return;
        }
        mirrorMap.put( mirror.getId(), mirror );
        this.setMaxMirrors( mirrorMap.size() );
    }

    @Override
    protected boolean existsMirrorWithId( boolean forWrite, String id )
    {
        return this.mirrorMap.containsKey( id );
    }
}
