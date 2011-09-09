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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.common.S2ProjectCommon;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.validation.api.IS2AccessValidator;
import com.sonatype.s2.project.validation.api.IS2ProjectValidator;
import com.sonatype.s2.project.validation.api.S2ProjectValidationContext;
import com.sonatype.s2.project.validator.internal.Messages;
import com.sonatype.s2.project.validator.internal.S2ProjectValidationPlugin;

public class ValidationFacade
{

    private final Logger log = LoggerFactory.getLogger( ValidationFacade.class );

    private static ValidationFacade instance = new ValidationFacade();

    public static ValidationFacade getInstance()
    {
        return instance;
    }

    public Set<IS2AccessValidator> getAccessValidators()
        throws CoreException
    {
        IConfigurationElement[] validatorsArray =
            Platform.getExtensionRegistry().getConfigurationElementsFor( IS2AccessValidator.EXTENSION_POINT_ID );
        Set<IS2AccessValidator> validators = new LinkedHashSet<IS2AccessValidator>();
        for ( IConfigurationElement v : validatorsArray )
        {
            if ( !"validator".equals( v.getName() ) )
            {
                continue;
            }
            final Object o = v.createExecutableExtension( "class" );
            log.debug( "Found validator: {}", o.getClass().getCanonicalName() );
            if ( o instanceof IS2AccessValidator )
            {
                ( (IS2AccessValidator) o ).configure( v );
            }
            else
            {
                throw new IllegalArgumentException( NLS.bind( Messages.error_accessValidatorInterfaceRequired,
                                                              o.getClass().getCanonicalName() ) );
            }
            validators.add( (IS2AccessValidator) o );
        }
        return validators;
    }

    public Set<IS2ProjectValidator> getAllValidators()
        throws CoreException
    {
        IConfigurationElement[] validatorsArray =
            Platform.getExtensionRegistry().getConfigurationElementsFor( IS2ProjectValidator.EXTENSION_POINT_ID );
        Set<IS2ProjectValidator> validators = new LinkedHashSet<IS2ProjectValidator>();
        for ( IConfigurationElement v : validatorsArray )
        {
            if ( !"validator".equals( v.getName() ) )
            {
                continue;
            }
            final Object o = v.createExecutableExtension( "class" );
            log.debug( "Found validator: {}", o.getClass().getCanonicalName() );
            if ( o instanceof IS2ProjectValidator )
            {
                ( (IS2ProjectValidator) o ).configure( v );
            }
            else
            {
                throw new IllegalArgumentException( NLS.bind( Messages.error_projectValidatorInterfaceRequired,
                                                              o.getClass().getCanonicalName() ) );
            }
            validators.add( (IS2ProjectValidator) o );
        }
        return validators;
    }

    public IStatus validateAccess( IS2Project s2Project, IProgressMonitor monitor )
        throws CoreException
    {
        List<IStatus> results = new ArrayList<IStatus>();

        Map<String, Set<String>> realms = S2ProjectCommon.getURLsBySecurityRealmIds( s2Project );

        for ( final String realmId : realms.keySet() )
        {
            results.add( validateAccess( s2Project, realmId, monitor ) );
        }

        return new MultiStatus( S2ProjectValidationPlugin.PLUGIN_ID, -1,
                                results.toArray( new IStatus[results.size()] ), null /* message */, null /* exception */);
    }

    public IStatus validateAccess( IS2Project s2Project, String securityRealmIdFilter, IProgressMonitor monitor )
        throws CoreException
    {
        List<IStatus> results = new ArrayList<IStatus>();

        Set<IS2AccessValidator> validators = getAccessValidators();
        SubMonitor progress = SubMonitor.convert( monitor, Messages.progress_validatingAccess, validators.size() );
        for ( IS2AccessValidator validator : validators )
        {
            log.debug( "Calling validator: {}", validator.getClass().getCanonicalName() );
            IStatus status =
                validator.validate( s2Project, securityRealmIdFilter, progress.newChild( 1, SubMonitor.SUPPRESS_NONE ) );
            log.debug( "Result: {}", status );
            if ( status != null )
            {
                results.add( status );
            }
        }
        return new MultiStatus( S2ProjectValidationPlugin.PLUGIN_ID, -1,
                                results.toArray( new IStatus[results.size()] ), null /* message */, null /* exception */);
    }

    public IStatus validate( IS2Project s2Project, S2ProjectValidationContext validationContext,
                             IProgressMonitor monitor, boolean validateAccess )
        throws CoreException
    {
        log.debug( "Validating {}", s2Project.getName() );

        Set<IS2ProjectValidator> validators = getAllValidators();

        SubMonitor progress =
            SubMonitor.convert( monitor, NLS.bind( Messages.progress_validatingProject, s2Project.getName() ),
                                validators.size() );

        Set<IS2ProjectValidator> otherValidators = new LinkedHashSet<IS2ProjectValidator>();
        for ( IS2ProjectValidator validator : validators )
        {
            if ( !IS2ProjectValidator.CATEGORY_ACCESS_VALIDATION.equals( validator.getCategory() ) )
            {
                otherValidators.add( validator );
            }
        }

        List<IStatus> results = new ArrayList<IStatus>();
        if ( validateAccess )
        {
            IStatus accessValidationStatus =
                validateAccess( s2Project, progress.newChild( validators.size() - otherValidators.size(),
                                                              SubMonitor.SUPPRESS_NONE ) );
            if ( !accessValidationStatus.isOK() )
            {
                return accessValidationStatus;
            }
            results.add( accessValidationStatus );
        }
        progress.setWorkRemaining( otherValidators.size() );

        // Call all remaining validators
        for ( IS2ProjectValidator validator : otherValidators )
        {
            log.debug( "Calling validator: {}", validator.getClass().getCanonicalName() );
            if ( !validator.isApplicable( validationContext ) )
            {
                continue;
            }
            IStatus status = validator.validate( s2Project, progress.newChild( 1, SubMonitor.SUPPRESS_NONE ) );
            log.debug( "Result: {}", status );
            results.add( status );
        }
        return new MultiStatus( S2ProjectValidationPlugin.PLUGIN_ID, -1,
                                results.toArray( new IStatus[results.size()] ), null /* message */, null /* exception */);
    }
}
