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
package com.sonatype.nexus.proxy.p2.its;

import org.sonatype.jettytestsuite.ServletServer;

public abstract class AbstractNexusProxyP2SecureIntegrationIT
    extends AbstractNexusProxyP2IntegrationIT
{
    protected AbstractNexusProxyP2SecureIntegrationIT()
    {
    }

    protected AbstractNexusProxyP2SecureIntegrationIT( String testRepositoryId )
    {
        super( testRepositoryId );
    }

    @Override
    public void startProxy()
        throws Exception
    {
        ServletServer server = (ServletServer) this.lookup( ServletServer.ROLE, "secure" );
        server.start();
    }

    @Override
    public void stopProxy()
        throws Exception
    {
        ServletServer server = (ServletServer) this.lookup( ServletServer.ROLE, "secure" );
        server.stop();
    }
}
