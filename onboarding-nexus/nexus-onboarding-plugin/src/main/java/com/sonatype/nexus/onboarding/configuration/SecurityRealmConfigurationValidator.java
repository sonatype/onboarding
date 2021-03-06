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

import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.validator.ConfigurationValidator;

import com.sonatype.nexus.onboarding.persist.model.CSecurityRealm;
import com.sonatype.nexus.onboarding.persist.model.CSecurityRealmURLAssoc;
import com.sonatype.nexus.onboarding.persist.model.CSecurityRealmsConfiguration;

public interface SecurityRealmConfigurationValidator
    extends ConfigurationValidator<CSecurityRealmsConfiguration>
{
    public ValidationResponse validateRealm( CSecurityRealm realm );

    public ValidationResponse validateRealmURLAssoc( CSecurityRealmURLAssoc securityRealmURLAssoc );

    public ValidationResponse validateRealmURLAssoc( CSecurityRealmURLAssoc securityRealmURLAssoc, boolean allowEmptyId );
}
