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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.maven.ide.eclipse.io.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.IUrlLocation;
import com.sonatype.s2.project.validation.api.AbstractProjectValidator;
import com.sonatype.s2.project.validation.api.IS2ProjectValidationStatus;
import com.sonatype.s2.project.validation.api.IS2ProjectValidator;
import com.sonatype.s2.project.validation.api.S2ProjectValidationStatus;
import com.sonatype.s2.project.validator.internal.S2ProjectValidationPlugin;

public abstract class AbstractUrlAccessValidator
    extends AbstractProjectValidator
{
    private static final Logger log = LoggerFactory.getLogger( AbstractUrlAccessValidator.class );

    private final String resourceName;

    public AbstractUrlAccessValidator( String resourceName )
    {
        this.resourceName = resourceName;
    }

    public static void accessURL( String sURL, String securityRealmId, IProgressMonitor monitor )
        throws IOException, URISyntaxException
    {
        log.debug( "Validating access to URL '{}'", sURL );

        InputStream is = S2IOFacade.openStream( sURL, monitor );
        try
        {
            // Try to read one byte from the URL
            byte[] b = new byte[1];
            is.read( b );
        }
        finally
        {
            IOUtil.close( is );
        }
    }

    /**
     * Returns OK, WARN or ERROR.
     */
    protected IS2ProjectValidationStatus validate( IS2Project s2Project, String securityRealmIdFilter,
                                                   IUrlLocation location, String urlSuffix, IProgressMonitor monitor )
    {
        if ( s2Project != null )
        {
            log.debug( "Validating {} access for project {}", resourceName, s2Project.getName() );
        }

        if ( location == null )
        {
            return null;// S2ProjectValidationStatus.getOKStatus( this );
        }

        String realmIdForUrl = null;
        IAuthRealm realmForUrl = AuthFacade.getAuthRegistry().getRealmForURI( location.getUrl() );
        if ( realmForUrl != null )
        {
            realmIdForUrl = realmForUrl.getId();
        }
        if ( securityRealmIdFilter != null && !securityRealmIdFilter.equals( realmIdForUrl )
            && !urlsEquals( securityRealmIdFilter, location.getUrl() ) )
        {
            // This location does not match the specified securityRealmId
            return null;
        }

        monitor.beginTask( "Validating " + resourceName + " access", 1 );

        Exception exception;
        try
        {
            String sURL = location.getUrl();

            if ( urlSuffix != null )
            {
                sURL = new URL( new URI( sURL + "/" ).normalize().toURL(), urlSuffix ).toString();
            }
            accessURL( sURL, realmIdForUrl, monitor );

            if ( monitor.isCanceled() )
            {
                throw new OperationCanceledException();
            }
            return new AccessValidationStatus( this, S2ProjectValidationStatus.getOKStatus( this ), location );
        }
        catch ( UnauthorizedException e )
        {
            exception = e;
        }
        catch ( URISyntaxException e )
        {
            exception = e;
        }
        catch ( IOException e )
        {
            exception = e;
        }
        finally
        {
            monitor.done();
        }

        if ( exception != null )
        {
            StringBuilder message = new StringBuilder();
            if ( s2Project != null )
            {
                message.append( "Error validating project " ).append( s2Project.getName() ).append( ": " );
            }
            message.append( exception.getMessage() );
            return new AccessValidationStatus( this, createErrorStatus( message.toString(), exception ), location );
        }
        throw new IllegalStateException();
    }

    public String getCategory()
    {
        return IS2ProjectValidator.CATEGORY_ACCESS_VALIDATION;
    }

    public IS2ProjectValidationStatus canRemediate( boolean headless )
    {
        return createErrorStatus( "Cannot remediate " + resourceName + " access for a codebase." );
    }

    public IS2ProjectValidationStatus remediate( boolean headless, IProgressMonitor monitor )
    {
        throw new UnsupportedOperationException();
    }

    public String getPluginId()
    {
        return S2ProjectValidationPlugin.PLUGIN_ID;
    }
}
