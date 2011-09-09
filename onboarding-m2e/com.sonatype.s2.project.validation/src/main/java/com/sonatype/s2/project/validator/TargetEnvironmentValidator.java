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
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.maven.ide.eclipse.io.UrlFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.IP2LineupTargetEnvironment;
import com.sonatype.s2.p2lineup.model.P2LineupTargetEnvironment;
import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Reader;
import com.sonatype.s2.project.model.IP2LineupLocation;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.validation.api.AbstractProjectValidator;
import com.sonatype.s2.project.validation.api.IS2ProjectValidationStatus;
import com.sonatype.s2.project.validation.api.IS2ProjectValidator;
import com.sonatype.s2.project.validation.api.S2ProjectValidationStatus;
import com.sonatype.s2.project.validator.internal.Messages;
import com.sonatype.s2.project.validator.internal.S2ProjectValidationPlugin;

public class TargetEnvironmentValidator
    extends AbstractProjectValidator
    implements IS2ProjectValidator
{
    private final Logger log = LoggerFactory.getLogger( TargetEnvironmentValidator.class );

    public IS2ProjectValidationStatus validate( IS2Project s2Project, IProgressMonitor monitor )
    {
        monitor.beginTask( "Validating target environment", 1 );

        try
        {
            IP2LineupLocation lineupLocation = s2Project.getP2LineupLocation();
            if ( lineupLocation == null )
            {
                return S2ProjectValidationStatus.getOKStatus( this );
            }

            String os = System.getProperty( "osgi.os" );
            String ws = System.getProperty( "osgi.ws" );
            String arch = System.getProperty( "osgi.arch" );
            P2LineupTargetEnvironment currentTargetEnvironment = new P2LineupTargetEnvironment( os, ws, arch );
            log.debug( "Current target environment: {}", currentTargetEnvironment.toString() );

            String lineupUrl = lineupLocation.getUrl();
            IP2Lineup lineup = loadP2Lineup( lineupUrl );
            for ( IP2LineupTargetEnvironment lineupTargetEnvironment : lineup.getTargetEnvironments() )
            {
                if ( lineupTargetEnvironment.equals( currentTargetEnvironment ) )
                {
                    return S2ProjectValidationStatus.getOKStatus( this );
                }
            }
            return createErrorStatus( errorMessage( currentTargetEnvironment ) );
        }
        catch ( URISyntaxException e )
        {
            return createErrorStatus( e );
        }
        catch ( IOException e )
        {
            return createErrorStatus( e );
        }
        catch ( XmlPullParserException e )
        {
            return createErrorStatus( e );
        }
        finally
        {
            monitor.done();
        }
    }
    
    private String errorMessage(P2LineupTargetEnvironment cte) {
        return NLS.bind( Messages.targetEnvValidator_format, 
                         new String[] { cte.getOsgiOS(), cte.getOsgiWS(), cte.getOsgiArch() } ); 
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

    private IP2Lineup loadP2Lineup( String p2LineupURLString )
        throws URISyntaxException, IOException, XmlPullParserException
    {
        if ( !p2LineupURLString.endsWith( "/" ) )
        {
            p2LineupURLString += "/";
        }
        URI p2LineupUri = new URL( new URL( p2LineupURLString ), "p2lineup.xml" ).toURI();
        InputStream is =
            new UrlFetcher().openStream( p2LineupUri, new NullProgressMonitor(), AuthFacade.getAuthService(),
                                         S2IOFacade.getProxyService() );
        try
        {
            return new P2LineupXpp3Reader().read( is, false /* strict */);
        }
        finally
        {
            IOUtil.close( is );
        }
    }
}
