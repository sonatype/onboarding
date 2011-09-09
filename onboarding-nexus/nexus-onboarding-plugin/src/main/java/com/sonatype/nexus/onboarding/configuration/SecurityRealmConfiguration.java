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
package com.sonatype.nexus.onboarding.configuration;

import java.util.List;

import org.sonatype.configuration.validation.InvalidConfigurationException;

import com.sonatype.nexus.onboarding.persist.model.CSecurityRealm;
import com.sonatype.nexus.onboarding.persist.model.CSecurityRealmURLAssoc;

public interface SecurityRealmConfiguration
{
    List<CSecurityRealm> listRealms();

    CSecurityRealm readRealm( String realmId )
        throws InvalidConfigurationException;

    void createOrUpdateRealm( CSecurityRealm realm )
        throws InvalidConfigurationException;

    void deleteRealm( String realmId )
        throws InvalidConfigurationException;

    List<CSecurityRealmURLAssoc> listURLs();

    CSecurityRealmURLAssoc readURL( String urlId )
        throws InvalidConfigurationException;

    CSecurityRealmURLAssoc createURL( CSecurityRealmURLAssoc url )
        throws InvalidConfigurationException;

    void updateURL( CSecurityRealmURLAssoc url )
        throws InvalidConfigurationException;

    void deleteURL( String urlId )
        throws InvalidConfigurationException;

    void save();
}
