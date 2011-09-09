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

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.model.IPrerequisites;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.validation.api.AbstractProjectValidator;
import com.sonatype.s2.project.validation.api.IS2ProjectValidationStatus;
import com.sonatype.s2.project.validation.api.S2ProjectValidationContext;
import com.sonatype.s2.project.validation.api.S2ProjectValidationMultiStatus;
import com.sonatype.s2.project.validation.api.S2ProjectValidationStatus;
import com.sonatype.s2.project.validator.internal.S2ProjectValidationPlugin;

public class PrerequisitesValidator
    extends AbstractProjectValidator
{
    private final Logger log = LoggerFactory.getLogger( PrerequisitesValidator.class );

    public IS2ProjectValidationStatus validate( IS2Project s2Project, IProgressMonitor monitor )
    {
        log.debug( "Validating prerequisites for project {}", s2Project.getName() );

        monitor.beginTask( "Validating project prerequisites", 1 );

        ArrayList<IStatus> results = new ArrayList<IStatus>();

        IPrerequisites prerequisites = s2Project.getPrerequisites();
        if ( prerequisites != null )
        {
            String requiredMem = prerequisites.getRequiredMemory();
            if ( requiredMem != null && requiredMem.length() > 0 )
            {
                long max = MemoryHelper.INSTANCE.getMaxMemory();

                log.debug( "Validating memory requirement {} is satisfied by {}", requiredMem, max );

                try
                {
                    long requiredBytes = MemoryHelper.INSTANCE.parseMemSpec( requiredMem );

                    // diff by 4 MB or 1% of max mem is tolerated
                    long tolerance = Math.max( 4 * 1024 * 1024, max / 100 );

                    log.debug( "Max memory = {}, tolerance = {}", max, tolerance );

                    if ( requiredBytes > max + tolerance )
                    {
                        results.add( createErrorStatus( "Insufficient heap memory, the materialization requires at least "
                            + ( requiredBytes / 1024 / 1024 )
                            + " MB but only "
                            + ( max / 1024 / 1024 )
                            + " MB are available" ) );
                    }
                }
                catch ( NumberFormatException e )
                {
                    results.add( createWarningStatus( "Unable to parse memory requirement: " + requiredMem ) );
                }
            }
        }

        monitor.worked( 1 );

        monitor.done();

        IS2ProjectValidationStatus status;
        if ( results.isEmpty() )
        {
            status = S2ProjectValidationStatus.getOKStatus( this );
        }
        else
        {
            status =
                new S2ProjectValidationMultiStatus( this, -1,
                                                    results.toArray( new IStatus[results.size()] ), null, null );
        }

        log.debug( "Validated prerequisites for project {} with result {}", s2Project.getName(), status );

        return status;
    }

    public IS2ProjectValidationStatus canRemediate( boolean headless )
    {
        return createErrorStatus( "Cannot remediate prerequisites for a codebase." );
    }

    public IS2ProjectValidationStatus remediate( boolean headless, IProgressMonitor monitor )
    {
        throw new UnsupportedOperationException();
    }

    public String getPluginId()
    {
        return S2ProjectValidationPlugin.PLUGIN_ID;
    }

    @Override
    public boolean isApplicable( S2ProjectValidationContext validationContext )
    {
        if ( !super.isApplicable( validationContext ) )
        {
            return false;
        }

        if ( validationContext != null
            && validationContext.getProperty( S2ProjectValidationContext.FRESH_INSTALL_PROPNAME ) != null )
        {
            log.debug( "The prerequisites validation is not applicable to a fresh installation." );
            return false;
        }

        return true;
    }
}
