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
package com.sonatype.nexus.p2.facade.internal;

import java.util.List;

public class P2AdviceData
{
    private String touchpointId;

    private String touchpointVersion;

    List<String> advices;

    public String getTouchpointId()
    {
        return touchpointId;
    }

    public void setTouchpointId( String touchpointId )
    {
        this.touchpointId = touchpointId;
    }

    public String getTouchpointVersion()
    {
        return touchpointVersion;
    }

    public void setTouchpointVersion( String touchpointVersion )
    {
        this.touchpointVersion = touchpointVersion;
    }

    public List<String> getAdvices()
    {
        return advices;
    }

    public void setAdvices( List<String> advices )
    {
        this.advices = advices;
    }
}
