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
package com.sonatype.nexus.onboarding.project.repository;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.nexus.proxy.repository.AbstractRepositoryConfiguration;

public class OnboardingRepositoryConfiguration
    extends AbstractRepositoryConfiguration
{
    private static final String SECURITY_REALM_ID_FIELD = "securityRealmId";

    private static final String MSE_INSTALLERS_REPOSITORY_ID = "MSEInstallersRepositoryId";

    public static final String DEFAULT_MSE_INSTALLERS_REPOSITORY_ID = "nx-mse";

    public OnboardingRepositoryConfiguration( Xpp3Dom configuration )
    {
        super( configuration );
    }

    public String getSecurityRealmId()
    {
        return getNodeValue( getRootNode(), SECURITY_REALM_ID_FIELD, null );
    }

    public void setSecurityRealmId( String securityRealmId )
    {
        setNodeValue( getRootNode(), SECURITY_REALM_ID_FIELD, securityRealmId );
    }

    public String getMSEInstallersRepositoryId()
    {
        return getNodeValue( getRootNode(), MSE_INSTALLERS_REPOSITORY_ID, DEFAULT_MSE_INSTALLERS_REPOSITORY_ID );
    }

    public void setMSEInstallersRepositoryId( String mseInstallersRepositoryId )
    {
        setNodeValue( getRootNode(), MSE_INSTALLERS_REPOSITORY_ID, mseInstallersRepositoryId );
    }
}
