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

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationRequest;
import org.sonatype.configuration.validation.ValidationResponse;

import com.sonatype.nexus.onboarding.persist.model.CSecurityRealm;
import com.sonatype.nexus.onboarding.persist.model.CSecurityRealmURLAssoc;
import com.sonatype.nexus.onboarding.persist.model.CSecurityRealmsConfiguration;

@Component( role = SecurityRealmConfigurationValidator.class )
public class DefaultSecurityRealmConfigurationValidator
    implements SecurityRealmConfigurationValidator
{
    public ValidationResponse validateRealm( CSecurityRealm realm )
    {
        ValidationResponse vr = new ValidationResponse();

        if ( StringUtils.isEmpty( realm.getId() ) )
        {
            vr.addValidationError( new ValidationMessage( "*", "Realm ID not defined" ) );
        }

        if ( StringUtils.isEmpty( realm.getName() ) )
        {
            vr.addValidationError( new ValidationMessage( "*", "Realm name not defined" ) );
        }

        return vr;
    }

    public ValidationResponse validateRealmURLAssoc( CSecurityRealmURLAssoc securityRealmURLAssoc )
    {
        return validateRealmURLAssoc( securityRealmURLAssoc, false /* allowEmptyId */);
    }

    public ValidationResponse validateRealmURLAssoc( CSecurityRealmURLAssoc securityRealmURLAssoc, boolean allowEmptyId )
    {
        ValidationResponse vr = new ValidationResponse();

        if ( !allowEmptyId && StringUtils.isEmpty( securityRealmURLAssoc.getId() ) )
        {
            vr.addValidationError( new ValidationMessage( "*", "Id not defined" ) );
        }

        if ( StringUtils.isEmpty( securityRealmURLAssoc.getRealmId() ) )
        {
            vr.addValidationError( new ValidationMessage( "*", "Realm id not defined" ) );
        }

        if ( StringUtils.isEmpty( securityRealmURLAssoc.getUrl() ) )
        {
            vr.addValidationError( new ValidationMessage( "*", "URL not defined" ) );
        }

        return vr;
    }

    public ValidationResponse validateModel( ValidationRequest<CSecurityRealmsConfiguration> request )
    {
        ValidationResponse vr = new ValidationResponse();

        CSecurityRealmsConfiguration cfg = request.getConfiguration();
        if ( cfg == null )
        {
            return vr;
        }
        else
        {
            for ( CSecurityRealm realm : cfg.getRealms() )
            {
                vr.append( validateRealm( realm ) );
            }
            for ( CSecurityRealmURLAssoc url : cfg.getUrls() )
            {
                vr.append( validateRealmURLAssoc( url ) );
            }
        }

        return vr;
    }
}
