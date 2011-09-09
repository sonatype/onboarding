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

import com.sonatype.s2.project.model.IP2LineupLocation;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.IUrlLocation;
import com.sonatype.s2.project.validation.api.IS2AccessValidator;
import com.sonatype.s2.project.validation.api.IS2ProjectValidationStatus;
import com.sonatype.s2.project.validation.api.IS2ProjectValidator;

public class P2LineupAccessValidator
    extends AbstractUrlAccessValidator
    implements IS2ProjectValidator, IS2AccessValidator
{
    public P2LineupAccessValidator()
    {
        super( "P2 lineup" );
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
        return ( location instanceof IP2LineupLocation );
    }

    public IS2ProjectValidationStatus validate( IUrlLocation location, IProgressMonitor monitor )
    {
        if ( !accept( location ) )
        {
            throw new IllegalArgumentException(
                                                "P2LineupAccessValidator.validate() was called for a location that is not a p2 lineup location." );
        }

        return validate( null /* s2Project */, null /* securityRealmId */, location, "p2lineup.xml", monitor );
    }

    public IS2ProjectValidationStatus validate( IS2Project s2Project, String securityRealmId, IProgressMonitor monitor )
    {
        return validate( s2Project, securityRealmId, s2Project.getP2LineupLocation(), "p2lineup.xml", monitor );
    }
}
