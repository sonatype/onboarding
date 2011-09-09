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
import java.util.LinkedHashSet;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;
import org.sonatype.nexus.proxy.mirror.DefaultDownloadMirrors;
import org.sonatype.nexus.proxy.mirror.DownloadMirrorSelector;
import org.sonatype.nexus.proxy.repository.Mirror;

public class P2DownloadMirrorSelector
    implements DownloadMirrorSelector
{

    private final DefaultDownloadMirrors dMirrors;

    private final LinkedHashSet<Mirror> mirrors = new LinkedHashSet<Mirror>();

    private final LinkedHashSet<Mirror> failedMirrors = new LinkedHashSet<Mirror>();

    private boolean success;

    public P2DownloadMirrorSelector( DefaultDownloadMirrors dMirrors, String mirrorOfUrl )
    {
        this.dMirrors = dMirrors;

        for ( Mirror mirror : dMirrors.getMirrors() )
        {
            if ( !dMirrors.isBlacklisted( mirror ) && StringUtils.equals( mirrorOfUrl, mirror.getMirrorOfUrl() ) )
            {
                mirrors.add( mirror );
            }

//            if ( mirrors.size() >= dMirrors.getMaxMirrors() )
//            {
//                break;
//            }
        }
    }

    public List<Mirror> getMirrors()
    {
        return new ArrayList<Mirror>( mirrors );
    }

    public void close()
    {
        if ( success )
        {
            dMirrors.blacklist( failedMirrors );
        }
    }

    public void feedbackSuccess( Mirror mirror )
    {
        // XXX validate URL

        failedMirrors.remove( mirror );

        this.success = true;
    }

    public void feedbackFailure( Mirror mirror )
    {
        // XXX validate URL

        failedMirrors.add( mirror );
    }

}
