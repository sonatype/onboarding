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

import java.io.File;
import java.net.MalformedURLException;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import com.sonatype.s2.p2lineup.model.P2LineupTargetEnvironment;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.descriptor.P2LineupLocation;
import com.sonatype.s2.project.model.descriptor.Project;
import com.sonatype.s2.project.validation.api.IS2ProjectValidationStatus;

@SuppressWarnings( "restriction" )
public class TargetEnvironmentValidatorTest
    extends TestCase
{
    public void testNoLineup()
        throws Exception
    {
        IS2Project project = newProject( null );
        assertTrue( validate( project ).isOK() );
    }

    public void testLineupMatchingTargetEnvironments()
        throws Exception
    {
        IS2Project project = newProject( "resources/targetenvironmentvalidator/realtargetenvironments" );
        IS2ProjectValidationStatus status = validate( project );
        assertTrue( status.getMessage(), status.isOK() );
    }

    public void testLineupNotMatchingTargetEnvironments()
        throws Exception
    {
        IS2Project project = newProject( "resources/targetenvironmentvalidator/faketargetenvironments" );
        IS2ProjectValidationStatus status = validate( project );
        assertEquals( status.getMessage(), Status.ERROR, status.getSeverity() );
        assertEquals( errorMessage( getCurrentTargetEnvironment() ), status.getMessage() );
    }

    public void testLineupNoTargetEnvironments()
        throws Exception
    {
        IS2Project project = newProject( "resources/targetenvironmentvalidator/notargetenvironments" );
        IS2ProjectValidationStatus status = validate( project );
        assertEquals( status.getMessage(), Status.ERROR, status.getSeverity() );
        assertEquals( errorMessage( getCurrentTargetEnvironment() ), status.getMessage() );
    }
    
    private String errorMessage(P2LineupTargetEnvironment cte) {
        return NLS.bind( targetEnvValidator_format, 
                         new String[] { cte.getOsgiOS(), cte.getOsgiWS(), cte.getOsgiArch() } ); 
    }
    //com.sonatype.s2.project.validator.internal.messages.targetEnvValidator_format
    String targetEnvValidator_format = "The target environment {0}/{1}/{2} does not match any of the target environments specified in the P2 lineup."; 

    private IS2ProjectValidationStatus validate( IS2Project project )
    {
        TargetEnvironmentValidator validator = new TargetEnvironmentValidator();
        IS2ProjectValidationStatus status = validator.validate( project, new NullProgressMonitor() );
        assertFalse( validator.canRemediate( true ).isOK() );
        assertFalse( validator.canRemediate( false ).isOK() );
        return status;
    }

    private IS2Project newProject( String lineupUrl )
        throws MalformedURLException
    {
        Project project = new Project();
        project.setName( getName() );
        if ( lineupUrl != null )
        {
            lineupUrl = new File( lineupUrl ).toURI().toURL().toString();
            P2LineupLocation p2LineupLocation = new P2LineupLocation();
            p2LineupLocation.setUrl( lineupUrl );
            project.setP2LineupLocation( p2LineupLocation );
        }

        return project;
    }

    private static P2LineupTargetEnvironment getCurrentTargetEnvironment()
    {
        String os = System.getProperty( "osgi.os" );
        String ws = System.getProperty( "osgi.ws" );
        String arch = System.getProperty( "osgi.arch" );
        return new P2LineupTargetEnvironment( os, ws, arch );
    }
}
