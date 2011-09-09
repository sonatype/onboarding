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
package com.sonatype.nexus.p2.lineup.persist;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.access.NexusItemAuthorizer;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.target.TargetSet;

public class MockNexusItemAuthorizer
    implements NexusItemAuthorizer
{
    
    private boolean authorized = true;
    
    public boolean authorizePath( Repository arg0, ResourceStoreRequest arg1, Action arg2 )
    {
        return authorized;
    }

    public boolean authorizePath( TargetSet arg0, Action arg1 )
    {
        return authorized;
    }

    public boolean authorizePermission( String arg0 )
    {
        return authorized;
    }

    public TargetSet getGroupsTargetSet( Repository arg0, ResourceStoreRequest arg1 )
    {
        return null;
    }

    public boolean isViewable( String arg0, String arg1 )
    {
        return authorized;
    }

    public void setAuthorized( boolean authorized )
    {
        this.authorized = authorized;
    }
    
}
