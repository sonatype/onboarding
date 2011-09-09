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
package com.sonatype.s2.project.validator;

import org.eclipse.core.runtime.IProgressMonitor;

import com.sonatype.s2.project.model.IMavenSettingsLocation;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.IUrlLocation;
import com.sonatype.s2.project.validation.api.IS2AccessValidator;
import com.sonatype.s2.project.validation.api.IS2ProjectValidationStatus;

public class MavenSettingsAccessValidator
    extends AbstractUrlAccessValidator
    implements IS2AccessValidator
{
    public MavenSettingsAccessValidator()
    {
        super( "maven settings" );
    }

    /**
     * Returns OK, WARN or ERROR.
     */
    public IS2ProjectValidationStatus validate( IS2Project s2Project, IProgressMonitor monitor )
    {
        return validate( s2Project, NULL_SECURITY_REALMID, monitor );
    }

    public boolean accept( IUrlLocation location )
    {
        return ( location instanceof IMavenSettingsLocation );
    }

    public IS2ProjectValidationStatus validate( IUrlLocation location, IProgressMonitor monitor )
    {
        if ( !accept( location ) )
        {
            throw new IllegalArgumentException(
                                                "MavenSettingsAccessValidator.validate() was called for a location that is not a maven settings location." );
        }

        return validate( null /* s2Project */, null /* securityRealmId */, location, null /* urlSuffix */, monitor );
    }

    public IS2ProjectValidationStatus validate( IS2Project s2Project, String securityRealmId, IProgressMonitor monitor )
    {
        return validate( s2Project, securityRealmId, s2Project.getMavenSettingsLocation(), null /* urlSuffix */,
                         monitor );
    }
}
