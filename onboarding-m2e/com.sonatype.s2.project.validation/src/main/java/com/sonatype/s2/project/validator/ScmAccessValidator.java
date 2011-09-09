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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.model.IResourceLocation;
import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.IScmLocation;
import com.sonatype.s2.project.model.IUrlLocation;
import com.sonatype.s2.project.validation.api.AbstractProjectValidator;
import com.sonatype.s2.project.validation.api.IS2AccessValidator;
import com.sonatype.s2.project.validation.api.IS2ProjectValidationStatus;
import com.sonatype.s2.project.validation.api.IS2ProjectValidator;
import com.sonatype.s2.project.validation.api.IScmAccessData;
import com.sonatype.s2.project.validation.api.IScmAccessValidator;
import com.sonatype.s2.project.validation.api.S2ProjectValidationMultiStatus;
import com.sonatype.s2.project.validation.api.ScmAccessData;
import com.sonatype.s2.project.validator.internal.S2ProjectValidationPlugin;

public class ScmAccessValidator
    extends AbstractProjectValidator
    implements IS2AccessValidator
{
    private final Logger log = LoggerFactory.getLogger( ScmAccessValidator.class );

    public IS2ProjectValidationStatus validate( IS2Project s2Project, IProgressMonitor monitor )
    {
        return validate( s2Project, NULL_SECURITY_REALMID, monitor );
    }

    /**
     * Returns OK, WARN or ERROR.
     */
    public IS2ProjectValidationStatus validate( IS2Project s2Project, String securityRealmIdFilter,
                                                IProgressMonitor monitor )
    {
        log.debug( "Validating SCM access for project {}", s2Project.getName() );

        List<IS2Module> modules = s2Project.getModules();

        SubMonitor progress = SubMonitor.convert( monitor, "Validating source repository access", modules.size() );

        ArrayList<IStatus> results = new ArrayList<IStatus>();

        for ( final IS2Module module : modules )
        {
            if ( progress.isCanceled() )
            {
                throw new OperationCanceledException();
            }

            progress.subTask( module.getName() );

            IResourceLocation scmLocation = module.getScmLocation();
            if ( scmLocation == null )
            {
                results.add( new Status( IStatus.ERROR, S2ProjectValidationPlugin.PLUGIN_ID,
                                         "SCM location is not specified" ) );
                monitor.worked( 1 );
                continue;
            }

            String scmUrl = scmLocation.getUrl();
            String realmIdForUrl = null;
            IAuthRealm realmForUrl = AuthFacade.getAuthRegistry().getRealmForURI( scmUrl );
            if ( realmForUrl != null )
            {
                realmIdForUrl = realmForUrl.getId();
            }
            if ( securityRealmIdFilter != null && !securityRealmIdFilter.equals( realmIdForUrl )
                && !urlsEquals( securityRealmIdFilter, scmUrl ) )
            {
                // This location does not match the specified securityRealmId
                monitor.worked( 1 );
                continue;
            }
            log.debug( "Validating SCM access for module {} at {}", module.getName(), scmUrl );

            IStatus status = validate( scmLocation, progress );
            results.add( status );
        }

        progress.subTask( "" );
        progress.done();

        if ( results.isEmpty() )
        {
            return null;
        }

        return new S2ProjectValidationMultiStatus( this, -1, results.toArray( new IStatus[results.size()] ),
                                                   null /* message */, null /* exception */);
    }

    private AccessValidationStatus wrapStatus( IStatus status, IUrlLocation location )
    {
        return new AccessValidationStatus( this, status, location );
    }

    public String getCategory()
    {
        return IS2ProjectValidator.CATEGORY_ACCESS_VALIDATION;
    }

    public IS2ProjectValidationStatus canRemediate( boolean headless )
    {
        return createErrorStatus( "Cannot remediate SCM access for a codebase." );
    }

    public IS2ProjectValidationStatus remediate( boolean headless, IProgressMonitor monitor )
    {
        throw new UnsupportedOperationException();
    }

    public String getPluginId()
    {
        return S2ProjectValidationPlugin.PLUGIN_ID;
    }

    public boolean accept( IUrlLocation location )
    {
        return ( location instanceof IScmLocation );
    }

    public IS2ProjectValidationStatus validate( IUrlLocation location, IProgressMonitor monitor )
    {
        if ( !accept( location ) )
        {
            throw new IllegalArgumentException(
                                                "ScmAccessValidator.validate() was called for a location that is not an SCM location." );
        }

        IScmLocation scmLocation = (IScmLocation) location;
        String scmUrl = scmLocation.getUrl();
        IAuthData authData = AuthFacade.getAuthService().select( scmUrl );
        IStatus status = validateAuthData( authData );
        if ( status.isOK() )
        {
            IScmAccessData scmData = new ScmAccessData( scmUrl, scmLocation.getBranch(), "HEAD", authData );

            IScmAccessValidator validator = getValidator( scmData );
            log.debug( "Validating SCM access to {} with {}", scmData.getRepositoryUrl(), validator );

            if ( validator == null )
            {
                status =
                    new Status( IStatus.ERROR, S2ProjectValidationPlugin.PLUGIN_ID,
                                "SCM provider is not available for " + scmData.getRepositoryUrl() );
                monitor.worked( 1 );
            }
            else
            {
                SubMonitor subprogress = SubMonitor.convert( monitor, 1 );
                status = validator.validate( scmData, subprogress );
                subprogress.done();
            }
        }
        else
        {
            monitor.worked( 1 );
        }

        if ( status != null && status.getException() != null )
        {
            log.debug( "Validated SCM access to " + scmUrl + " with result " + status,
                       status.getException() );
        }
        else
        {
            log.debug( "Validated SCM access to {} with result {}", scmUrl, status );
        }

        return wrapStatus( status, scmLocation );
    }

    private IStatus validateAuthData( IAuthData authData )
    {
        if ( authData == null )
        {
            // Nothing to validate
            return Status.OK_STATUS;
        }

        if ( authData.allowsUsernameAndPassword() && !authData.allowsAnonymousAccess() )
        {
            if ( StringUtils.isEmpty( authData.getUsername() ) && StringUtils.isEmpty( authData.getPassword() ) )
            {
                return new Status( IStatus.ERROR, S2ProjectValidationPlugin.PLUGIN_ID,
                                   "Anonymous access is not allowed" );
            }
        }

        return Status.OK_STATUS;
    }

    private IScmAccessValidator getValidator( IScmAccessData data )
    {
        List<IScmAccessValidator> validators = new ArrayList<IScmAccessValidator>();

        for ( IScmAccessValidator validator : getAllValidators() )
        {
            if ( validator.accept( data ) )
            {
                validators.add( validator );
            }
        }

        Collections.sort( validators, new Comparator<IScmAccessValidator>()
        {
            public int compare( IScmAccessValidator o1, IScmAccessValidator o2 )
            {
                return o1.getPriority() - o2.getPriority();
            }
        } );

        return validators.isEmpty() ? null : validators.get( 0 );
    }

    protected List<IScmAccessValidator> getAllValidators()
    {
        List<IScmAccessValidator> validators = new ArrayList<IScmAccessValidator>();

        IConfigurationElement[] configurationElements =
            Platform.getExtensionRegistry().getConfigurationElementsFor( IScmAccessValidator.EXTENSION_POINT_ID );
        for ( IConfigurationElement v : configurationElements )
        {
            if ( !"validator".equals( v.getName() ) )
            {
                continue;
            }
            try
            {
                Object extension = v.createExecutableExtension( "class" );
                log.debug( "Found SCM validator: {}", extension.getClass().getCanonicalName() );

                if ( !( extension instanceof IScmAccessValidator ) )
                {
                    throw new IllegalArgumentException( extension.getClass().getCanonicalName()
                        + " does not implement the IScmAccessValidator interface" );
                }
                validators.add( (IScmAccessValidator) extension );
            }
            catch ( CoreException e )
            {
                log.error( "Failed to instantiate SCM access validator, will be ignored", e );
            }
        }
        return validators;
    }

    public boolean isSupportedType( String type )
    {
        for ( IScmAccessValidator validator : getAllValidators() )
        {
            if ( validator.accept( type ) )
            {
                return true;
            }
        }
        return false;
    }
}
