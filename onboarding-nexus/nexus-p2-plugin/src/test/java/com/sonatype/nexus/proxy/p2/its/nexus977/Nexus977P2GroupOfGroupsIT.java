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
package com.sonatype.nexus.proxy.p2.its.nexus977;

import java.net.URL;

import org.junit.Test;

import com.sonatype.nexus.proxy.p2.its.AbstractNexusProxyP2IntegrationIT;

public class Nexus977P2GroupOfGroupsIT
    extends AbstractNexusProxyP2IntegrationIT
{

    @Test
    public void groupOfGroups()
        throws Exception
    {
        downloadFile( new URL( getRepositoryUrl( "g1" ) + "/content.xml" ), "target/downloads/nxcm1995/1/content.xml" );
    }
}
