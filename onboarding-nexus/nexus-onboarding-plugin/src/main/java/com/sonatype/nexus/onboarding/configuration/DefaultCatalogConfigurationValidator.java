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

import com.sonatype.nexus.onboarding.persist.model.CCatalog;
import com.sonatype.nexus.onboarding.persist.model.CCatalogConfiguration;
import com.sonatype.nexus.onboarding.persist.model.CCatalogEntry;

@Component( role = CatalogConfigurationValidator.class )
public class DefaultCatalogConfigurationValidator
    implements CatalogConfigurationValidator
{

    public ValidationResponse validateModel( ValidationRequest<CCatalogConfiguration> req )
    {
        ValidationResponse vr = new ValidationResponse();

        CCatalogConfiguration cfg = req.getConfiguration();
        if ( cfg == null )
        {
            ValidationMessage msg = new ValidationMessage( "*", "No configuration available to validate" );

            vr.addValidationError( msg );
        }
        else
        {
            for ( CCatalog catalog : cfg.getCatalogs() )
            {
                vr.append( validateCatalog( catalog ) );
            }
        }

        return vr;
    }

    public ValidationResponse validateCatalog( CCatalog catalog )
    {
        ValidationResponse vr = new ValidationResponse();

        if ( StringUtils.isEmpty( catalog.getId() ) )
        {
            vr.addValidationError( new ValidationMessage( "*", "Catalog ID not defined" ) );
        }

        if ( StringUtils.isEmpty( catalog.getName() ) )
        {
            vr.addValidationError( new ValidationMessage( "*", "Catalog name not defined" ) );
        }

        for ( CCatalogEntry entry : catalog.getEntries() )
        {
            vr.append( validateCatalogEntry( entry ) );
        }

        return vr;
    }

    public ValidationResponse validateCatalogEntry( CCatalogEntry entry )
    {
        ValidationResponse vr = new ValidationResponse();

        if ( StringUtils.isEmpty( entry.getId() ) )
        {
            vr.addValidationError( new ValidationMessage( "*", "Catalog entry ID not defined" ) );
        }

        if ( StringUtils.isEmpty( entry.getName() ) )
        {
            vr.addValidationError( new ValidationMessage( "*", "Catalog entry name not defined" ) );
        }

        return vr;
    }

}
