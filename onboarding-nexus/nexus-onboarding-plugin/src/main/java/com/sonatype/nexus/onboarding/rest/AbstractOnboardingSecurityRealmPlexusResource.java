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
package com.sonatype.nexus.onboarding.rest;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.component.annotations.Requirement;

import com.sonatype.nexus.onboarding.configuration.SecurityRealmConfiguration;
import com.sonatype.nexus.onboarding.persist.model.CSecurityRealm;
import com.sonatype.nexus.onboarding.persist.model.CSecurityRealmURLAssoc;
import com.sonatype.s2.securityrealm.model.S2SecurityRealm;
import com.sonatype.s2.securityrealm.model.S2SecurityRealmAuthenticationType;
import com.sonatype.s2.securityrealm.model.S2SecurityRealmURLAssoc;

public abstract class AbstractOnboardingSecurityRealmPlexusResource
    extends AbstractOnboardingPlexusResource
{
    @Requirement
    protected SecurityRealmConfiguration configuration;

    protected List<S2SecurityRealm> toRest( List<CSecurityRealm> listRealms )
    {
        List<S2SecurityRealm> res = new ArrayList<S2SecurityRealm>();
        for ( CSecurityRealm cRealm : listRealms )
        {
            res.add( toRest( cRealm ) );
        }
        return res;
    }

    protected S2SecurityRealm toRest( CSecurityRealm cRealm )
    {
        S2SecurityRealm req = new S2SecurityRealm();

        req.setId( cRealm.getId() );
        req.setName( cRealm.getName() );
        req.setDescription( cRealm.getDescription() );
        req.setAuthenticationType( S2SecurityRealmAuthenticationType.valueOf( cRealm.getAuthenticationType() ) );

        return req;
    }

    protected S2SecurityRealmURLAssoc toRest( CSecurityRealmURLAssoc cRealmURLAssoc )
    {
        S2SecurityRealmURLAssoc req = new S2SecurityRealmURLAssoc();

        req.setId( cRealmURLAssoc.getId() );
        req.setRealmId( cRealmURLAssoc.getRealmId() );
        req.setUrl( cRealmURLAssoc.getUrl() );
        req.setAnonymousAccess( cRealmURLAssoc.getAnonymousAccess() );

        return req;
    }

    protected List<S2SecurityRealmURLAssoc> urlAssocListToRest( List<CSecurityRealmURLAssoc> listRealmURLAssoc )
    {
        List<S2SecurityRealmURLAssoc> res = new ArrayList<S2SecurityRealmURLAssoc>();
        for ( CSecurityRealmURLAssoc cRealmURLAssoc : listRealmURLAssoc )
        {
            res.add( toRest( cRealmURLAssoc ) );
        }
        return res;
    }

    protected CSecurityRealm toModel( S2SecurityRealm dto )
    {
        CSecurityRealm realm = new CSecurityRealm();
        realm.setId( dto.getId() );
        realm.setName( dto.getName() );
        realm.setDescription( dto.getDescription() );
        realm.setAuthenticationType( dto.getAuthenticationType() );
        return realm;
    }

    protected CSecurityRealmURLAssoc toModel( S2SecurityRealmURLAssoc dto )
    {
        CSecurityRealmURLAssoc model = new CSecurityRealmURLAssoc();
        model.setId( dto.getId() );
        model.setRealmId( dto.getRealmId() );
        model.setUrl( dto.getUrl() );
        model.setAnonymousAccess( dto.getAnonymousAccess() );
        return model;
    }
}
