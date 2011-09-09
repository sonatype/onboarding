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

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.validation.api.AbstractProjectValidator;
import com.sonatype.s2.project.validation.api.IS2ProjectValidationStatus;
import com.sonatype.s2.project.validation.api.IS2ProjectValidator;
import com.sonatype.s2.project.validation.api.S2ProjectValidationContext;
import com.sonatype.s2.project.validation.api.S2ProjectValidationStatus;
import com.sonatype.s2.project.validator.internal.S2ProjectValidationPlugin;

public class EmptyWorkspaceValidator
    extends AbstractProjectValidator
    implements IS2ProjectValidator
{
    private final Logger log = LoggerFactory.getLogger( EmptyWorkspaceValidator.class );

    public static final String VALIDATION_FAILED_MESSAGE =
        "The workspace is not empty.  The materialization of a codebase in a workspace that is not empty is not recommended and it may render the workspace unusable.";

    public IS2ProjectValidationStatus validate( IS2Project s2Project, IProgressMonitor monitor )
    {
        monitor.beginTask( "Validating workspace state", 1 );
        IS2ProjectValidationStatus status;
        if ( ResourcesPlugin.getWorkspace().getRoot().getProjects().length == 0 )
        {
            status = S2ProjectValidationStatus.getOKStatus( this );
        }
        else
        {
            status = createErrorStatus( VALIDATION_FAILED_MESSAGE );
        }
        monitor.done();
        return status;
    }

    public IS2ProjectValidationStatus canRemediate( boolean headless )
    {
        return createErrorStatus( "Cannot remediate." );
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
            log.debug( "The empty workspace validation is not applicable to a fresh installation." );
            return false;
        }

        return true;
    }
}
