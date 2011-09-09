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

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;

public class MockGlobalRestApiSettings implements GlobalRestApiSettings
{

    private boolean enabled = true;
    
    private String baseUrl = "http://localhost:8081/nexus/";
    
    private boolean forceBaseUrl = false;
    
    public void disable()
    {
        this.enabled = false;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public boolean isForceBaseUrl()
    {
        return forceBaseUrl;
    }

    public void setBaseUrl( String baseUrl )
    {
        this.baseUrl = baseUrl;
        
    }

    public void setForceBaseUrl( boolean forceBaseUrl )
    {
        this.forceBaseUrl = forceBaseUrl;
    }

    public boolean commitChanges()
        throws ConfigurationException
    {
        return true;
    }

    public void configure( Object config )
        throws ConfigurationException
    {
        // TODO Auto-generated method stub
        
    }

    public CoreConfiguration getCurrentCoreConfiguration()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getName()
    {
        return "mock";
    }

    public boolean isDirty()
    {
        return false;
    }

    public boolean rollbackChanges()
    {
        return false;
    }

    public int getUITimeout()
    {
        return 5000;
    }

    public void setUITimeout( int arg0 )
    {
    }
}
