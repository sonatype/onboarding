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
package com.sonatype.nexus.proxy.p2.its.P2LineupResolver;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.nexus.p2.rest.model.P2LineupError;
import com.sonatype.nexus.p2.rest.model.P2LineupErrorResponse;
import com.sonatype.nexus.p2.rest.model.P2LineupRepositoryError;
import com.sonatype.nexus.p2.rest.model.P2LineupUnresolvedInstallableUnit;
import com.sonatype.nexus.proxy.p2.its.AbstractP2LineupIT;
import com.sonatype.s2.p2lineup.model.IP2Lineup;

public class P2LineupResolverIT
    extends AbstractP2LineupIT
{
    private static final String P2_LINEUP_REPO_ID = "p2lineups";

    private static final boolean WARNING = true;

    private static final boolean ERROR = false;

    public P2LineupResolverIT()
    {
        super( P2_LINEUP_REPO_ID );
    }

    @Test
    public void oneMissingRepository()
        throws Exception
    {
        P2LineupErrorResponse errorResponse = uploadInvalidP2LineupDryRun( "oneMissingRepository.xml" );
        List<P2LineupError> errors = errorResponse.getErrors();
        Assert.assertNotNull( errors );
        Assert.assertEquals( errors.toString(), 2, errors.size() );
        for ( P2LineupError p2LineupError : errors )
        {
            if ( p2LineupError instanceof P2LineupRepositoryError )
            {
                assertP2LineupUnresolvedRepository( p2LineupError, "doesnotexist/", WARNING );
            }
            else
            {
                assertP2MissingExecutable( p2LineupError );
            }
        }
    }

    @Test
    public void oneMissingRepositoryAddLineup()
        throws Exception
    {
        // The lineup should have only a warning (unresolved repository), so it must be added successfully
        IP2Lineup lineup = uploadP2Lineup( "oneMissingRepository.xml" );
        Assert.assertNotNull( lineup );
    }

    @Test
    public void twoMissingRepositories()
        throws Exception
    {
        P2LineupErrorResponse errorResponse = uploadInvalidP2LineupDryRun( "twoMissingRepository.xml" );
        List<P2LineupError> errors = errorResponse.getErrors();
        Assert.assertNotNull( errors );
        Assert.assertEquals( errors.toString(), 3, errors.size() );
        assertP2MissingExecutable( errors.get( 0 ) );
        assertP2LineupUnresolvedRepository( errors.get( 1 ), "doesnotexist/", WARNING );
        assertP2LineupUnresolvedRepository( errors.get( 2 ), "doesnotexist1/", WARNING );
    }

    @Test
    public void oneMissingInstallableUnit()
        throws Exception
    {
        P2LineupErrorResponse errorResponse = uploadInvalidP2Lineup( "oneMissingInstallableUnit.xml" );
        List<P2LineupError> errors = errorResponse.getErrors();
        Assert.assertNotNull( errors );
        Assert.assertEquals( 1, errors.size() );
        assertP2LineupUnresolvedInstallableUnit( errors.get( 0 ), "com.sonatype.nexus.p2.its.bundle2", "1.0.0",
                                                 "Missing requirement: 'com.sonatype.nexus.p2.its.bundle2', version 1.0.0" );
    }

    @Test
    public void oneMissingInstallableUnitWithOneTargetEnvironment()
        throws Exception
    {
        P2LineupErrorResponse errorResponse =
            uploadInvalidP2Lineup( "oneMissingInstallableUnitWithOneTargetEnvironment.xml" );
        List<P2LineupError> errors = errorResponse.getErrors();
        Assert.assertNotNull( errors );
        Assert.assertEquals( 1, errors.size() );
        assertP2LineupUnresolvedInstallableUnit( errors.get( 0 ),
                                                 "com.sonatype.nexus.p2.its.bundle2",
                                                 "1.0.0",
                                                 "Missing requirement: 'com.sonatype.nexus.p2.its.bundle2', version 1.0.0, target environment {osgi.ws=win32, osgi.os=win32, osgi.arch=x86}" );
    }

    @Test
    public void oneMissingInstallableUnitWithTwoTargetEnvironments()
        throws Exception
    {
        P2LineupErrorResponse errorResponse =
            uploadInvalidP2Lineup( "oneMissingInstallableUnitWithTwoTargetEnvironments.xml" );
        List<P2LineupError> errors = errorResponse.getErrors();
        Assert.assertNotNull( errors );
        Assert.assertEquals( 1, errors.size() );
        assertP2LineupUnresolvedInstallableUnit( errors.get( 0 ),
                                                 "com.sonatype.nexus.p2.its.bundle2",
                                                 "1.0.0",
                                                 "Missing requirement: 'com.sonatype.nexus.p2.its.bundle2', version 1.0.0, target environment {osgi.ws=win32, osgi.os=win32, osgi.arch=x86}, target environment {osgi.ws=gtk, osgi.os=linux, osgi.arch=x86}" );
    }

    @Test
    public void twoMissingInstallableUnit()
        throws Exception
    {
        P2LineupErrorResponse errorResponse = uploadInvalidP2Lineup( "twoMissingInstallableUnit.xml" );
        List<P2LineupError> errors = errorResponse.getErrors();
        Assert.assertNotNull( errors );
        Assert.assertEquals( 2, errors.size() );
        assertP2LineupUnresolvedInstallableUnit( errors.get( 0 ), "com.sonatype.nexus.p2.its.bundle2", "1.0.0",
                                                 "Missing requirement: 'com.sonatype.nexus.p2.its.bundle2', version 1.0.0" );
        assertP2LineupUnresolvedInstallableUnit( errors.get( 1 ), "com.sonatype.nexus.p2.its.bundle3", "1.0.0",
                                                 "Missing requirement: 'com.sonatype.nexus.p2.its.bundle3', version 1.0.0" );
    }

    @Test
    public void oneMissingRepository_oneMissingInstallableUnit()
        throws Exception
    {
        P2LineupErrorResponse errorResponse =
            uploadInvalidP2Lineup( "oneMissingRepository_oneMissingInstallableUnit.xml" );
        List<P2LineupError> errors = errorResponse.getErrors();
        Assert.assertNotNull( errors );
        Assert.assertEquals( 2, errors.size() );
        assertP2LineupUnresolvedRepository( errors.get( 0 ), "doesnotexist/", ERROR );
        assertP2LineupUnresolvedInstallableUnit( errors.get( 1 ), "com.sonatype.nexus.p2.its.bundle2", "1.0.0",
                                                 "Missing requirement: 'com.sonatype.nexus.p2.its.bundle2', version 1.0.0" );
    }

    @Test
    public void oneMissingInstallableUnitTransitiveDependency()
        throws Exception
    {
        P2LineupErrorResponse errorResponse =
            uploadInvalidP2Lineup( "oneMissingInstallableUnitTransitiveDependency.xml" );
        List<P2LineupError> errors = errorResponse.getErrors();
        Assert.assertNotNull( errors );
        Assert.assertEquals( 1, errors.size() );
        assertP2LineupUnresolvedInstallableUnit( errors.get( 0 ),
                                                 "com.sonatype.nexus.p2.its.bundle",
                                                 "[1.0.0,1.0.0]",
                                                 "Missing requirement: com.sonatype.nexus.p2.its.bundle3 1.0.0 requires 'com.sonatype.nexus.p2.its.bundle [1.0.0]' but it could not be found" );
    }

    private void assertP2LineupUnresolvedRepository( P2LineupError error, String repositoryURL, boolean warning )
    {
        Assert.assertTrue( "Expected instance of P2LineupRepositoryError, error was: " + error.getErrorMessage(),
                           error instanceof P2LineupRepositoryError );
        P2LineupRepositoryError error1 = (P2LineupRepositoryError) error;
        Assert.assertTrue( error1.getRepositoryURL(), error1.getRepositoryURL().endsWith( repositoryURL ) );
        Assert.assertEquals( "Cannot resolve repository on Nexus server.", error1.getErrorMessage() );
        Assert.assertEquals( "Expected error.isWarning=" + warning, warning, error1.isWarning() );
    }

    private void assertP2LineupRepositoryError( P2LineupError error, String repositoryURL, String errorMessage,
                                                boolean warning )
    {
        Assert.assertTrue( "Expected instance of P2LineupRepositoryError, error was: " + error.getErrorMessage(),
                           error instanceof P2LineupRepositoryError );
        P2LineupRepositoryError error1 = (P2LineupRepositoryError) error;
        Assert.assertTrue( error1.getRepositoryURL(), error1.getRepositoryURL().endsWith( repositoryURL ) );
        Assert.assertEquals( errorMessage, error1.getErrorMessage() );
        Assert.assertEquals( "Expected error.isWarning=" + warning, warning, error1.isWarning() );
    }

    private void assertP2LineupUnresolvedInstallableUnit( P2LineupError error, String iuId, String iuVersion,
                                                          String explanation )
    {
        Assert.assertTrue( "Expected instance of P2LineupUnresolvedInstallableUnit",
                           error instanceof P2LineupUnresolvedInstallableUnit );
        P2LineupUnresolvedInstallableUnit error1 = (P2LineupUnresolvedInstallableUnit) error;
        Assert.assertEquals( iuId, error1.getInstallableUnitId() );
        Assert.assertEquals( iuVersion, error1.getInstallableUnitVersion() );
        Assert.assertEquals( explanation, error1.getErrorMessage() );
    }

    private void assertP2MissingExecutable( P2LineupError error )
    {
        Assert.assertTrue( error.isWarning() );
        Assert.assertEquals( "The lineup does not contain an executable.", error.getErrorMessage() );
    }

    @Test
    public void oneInstallableUnitUndeclaredTargetEnvironment()
        throws Exception
    {
        P2LineupErrorResponse errorResponse =
            uploadInvalidP2Lineup( "oneInstallableUnitUndeclaredTargetEnvironment.xml" );
        List<P2LineupError> errors = errorResponse.getErrors();
        Assert.assertNotNull( errors );
        Assert.assertEquals( 1, errors.size() );
        assertP2LineupUnresolvedInstallableUnit( errors.get( 0 ),
                                                 "com.sonatype.nexus.p2.its.feature.feature.group",
                                                 "1.0.0",
                                                 "Target environment: 'osgiOS=linux/osgiArch=x86_64' does not match any of the target environments declared in the lineup." );
    }

    @Test
    public void twoInstallableUnitsUndeclaredTargetEnvironment()
        throws Exception
    {
        P2LineupErrorResponse errorResponse =
            uploadInvalidP2Lineup( "twoInstallableUnitUndeclaredTargetEnvironment.xml" );
        List<P2LineupError> errors = errorResponse.getErrors();
        Assert.assertNotNull( errors );
        Assert.assertEquals( 2, errors.size() );
        assertP2LineupUnresolvedInstallableUnit( errors.get( 0 ),
                                                 "com.sonatype.nexus.p2.its.feature.feature.group",
                                                 "1.0.0",
                                                 "Target environment: 'osgiOS=linux/osgiArch=x86_64' does not match any of the target environments declared in the lineup." );
        assertP2LineupUnresolvedInstallableUnit( errors.get( 1 ), "com.sonatype.nexus.p2.its.feature.feature.group",
                                                 "1.0.0",
                                                 "Target environment: 'osgiOS=macos' does not match any of the target environments declared in the lineup." );
    }

    @Test
    public void noInstallableUnits()
        throws Exception
    {
        P2LineupErrorResponse errorResponse = uploadInvalidP2Lineup( "noInstallableUnits.xml" );
        List<P2LineupError> errors = errorResponse.getErrors();
        Assert.assertNotNull( errors );
        Assert.assertEquals( 1, errors.size() );
        Assert.assertTrue( "Error class: " + errors.get( 0 ).getClass().getCanonicalName(),
                           errors.get( 0 ) instanceof P2LineupError );
        Assert.assertEquals( "At least one installable unit must be specified", errors.get( 0 ).getErrorMessage() );
    }

    @Test
    public void noRepositories()
        throws Exception
    {
        P2LineupErrorResponse errorResponse = uploadInvalidP2Lineup( "noRepositories.xml" );
        List<P2LineupError> errors = errorResponse.getErrors();
        Assert.assertNotNull( errors );
        Assert.assertEquals( 1, errors.size() );
        Assert.assertTrue( "Error class: " + errors.get( 0 ).getClass().getCanonicalName(),
                           errors.get( 0 ) instanceof P2LineupError );
        Assert.assertEquals( "At least one repository must be specified", errors.get( 0 ).getErrorMessage() );
    }

    @Test
    public void oneInvalidRepository()
        throws Exception
    {
        P2LineupErrorResponse errorResponse = uploadInvalidP2Lineup( "oneInvalidRepository.xml" );
        List<P2LineupError> errors = errorResponse.getErrors();
        Assert.assertNotNull( errors );
        Assert.assertEquals( 2, errors.size() );
        assertP2LineupRepositoryError( errors.get( 0 ),
                                       "updatesite",
                                       "java.lang.RuntimeException: Unknown repository type org.eclipse.equinox.internal.p2.updatesite.artifact.UpdateSiteArtifactRepository",
                                       ERROR );
    }

    @Test
    public void includeExecutable()
        throws Exception
    {
        IP2Lineup lineup = uploadP2Lineup( "withExecutable.xml" );
        Assert.assertNotNull( lineup );
    }

    @Test
    // The lineup does not have an executable, but the lineup should be added successfully
    public void doesNotIncludeExecutable()
        throws Exception
    {
        IP2Lineup lineup = uploadP2Lineup( "withoutExecutable.xml" );
        Assert.assertNotNull( lineup );
        // Response response = RequestFacade.doGetRequest( getP2RepoURL( lineup ) + "/content.xml" );
        // Assert.assertEquals( 200, response.getStatus().getCode() );
    }

    @Test
    // Verify that we do report errors when
    public void doesNotIncludeExecutableDryRun()
        throws Exception
    {
        P2LineupErrorResponse p2Lineup = uploadInvalidP2LineupDryRun( "withoutExecutable2.xml" );
        Assert.assertEquals( 1, p2Lineup.getErrors().size() );
        assertP2MissingExecutable( p2Lineup.getErrors().get( 0 ) );
    }

    @Test
    public void includeExecutableDryRun()
        throws Exception
    {
        dryRunValidP2Lineup( "withExecutable2.xml" );
    }
}
