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
package com.sonatype.s2.project.validator.p2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileModificationJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.Dialog;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.maven.ide.eclipse.io.UrlFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupHelper;
import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Reader;
import com.sonatype.s2.project.core.ide.IIDEUpdater;
import com.sonatype.s2.project.model.IP2LineupLocation;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.validation.api.AbstractProjectValidator;
import com.sonatype.s2.project.validation.api.IS2ProjectValidationStatus;
import com.sonatype.s2.project.validation.api.IS2ProjectValidator;
import com.sonatype.s2.project.validation.api.S2ProjectValidationContext;
import com.sonatype.s2.project.validation.api.S2ProjectValidationException;
import com.sonatype.s2.project.validation.api.S2ProjectValidationStatus;
import com.sonatype.s2.project.validation.p2.internal.P2ValidatorPlugin;

@SuppressWarnings( "restriction" )
public class EclipseInstallationValidator
    extends AbstractProjectValidator
    implements IS2ProjectValidator, IIDEUpdater
{
    private static final String COM_SONATYPE_LINEUP_MATERIALIZED = "com.sonatype.lineup.metarialized";

    private static final Logger log = LoggerFactory.getLogger( EclipseInstallationValidator.class );

    public static final String SUCCESS_MESSAGE =
        "The current eclipse installation meets all requirements specified in the associated p2 lineup.";

    public static final int CAN_TRY_REMEDIATE_STATUS_CODE = 100;

    private IProfile targetProfile;

    private InstallOperation installationOperation = null;

    private IProvisioningAgent p2Agent = null;

    private IS2Project s2Project;

    private URI p2LineupUri;

    private IInstallableUnit p2LineupIU;

    private IS2ProjectValidationStatus validationStatus;

    private IInstallableUnit getP2LineupInstallableUnit( URI p2LineupUri, IP2Lineup p2Lineup, IProgressMonitor monitor )
        throws S2ProjectValidationException
    {
        String p2LineupIUId = P2LineupHelper.getMasterInstallableUnitId( p2Lineup );
        Version p2LineupIUVersion = Version.parseVersion( p2Lineup.getVersion() );

        IMetadataRepository lineupRepo = null;
        try
        {
            lineupRepo =
                ( (IMetadataRepositoryManager) getp2Agent().getService( IMetadataRepositoryManager.SERVICE_NAME ) ).loadRepository( p2LineupUri,
                                                                                                                                    monitor );
        }
        catch ( ProvisionException e )
        {
            throw new S2ProjectValidationException( e );
        }
        catch ( OperationCanceledException e )
        {
            throw new S2ProjectValidationException( createErrorStatus( "Loading of repository line up cancelled" ) );
        }
        IQueryResult<IInstallableUnit> lineUpIU =
            lineupRepo.query( QueryUtil.createIUQuery( new VersionedId( p2LineupIUId, p2LineupIUVersion ) ), monitor );
        if ( lineUpIU.isEmpty() )
        {
            throw new S2ProjectValidationException( "Cannot find p2 lineup installable unit with id=" + p2LineupIUId
                + ", version=" + p2LineupIUVersion + " at " + p2LineupUri );
        }

        return lineUpIU.iterator().next();
    }

    public IS2ProjectValidationStatus getValidationStatus()
    {
        return validationStatus;
    }

    private void setValidationStatus( IS2ProjectValidationStatus validationStatus )
    {
        if ( this.validationStatus == null )
        {
            this.validationStatus = validationStatus;
        }
    }

    private boolean setCredentialsForUri( URI uri )
    {
        boolean cleanP2Credentials = false;
        IAuthData authData = AuthFacade.getAuthService().select( uri.toString() );
        if ( authData != null )
        {
            cleanP2Credentials = !P2AuthHelper.hasCredentialsForURI( uri );
            P2AuthHelper.setCredentials( uri, authData.getUsername(), authData.getPassword() );
        }
        return cleanP2Credentials;
    }

    private IProvisioningAgent getp2Agent()
    {
        if ( p2Agent == null )
            p2Agent =
                (IProvisioningAgent) ServiceHelper.getService( P2ValidatorPlugin.getDefault().getContext(),
                                                               IProvisioningAgent.SERVICE_NAME );
        return p2Agent;
    }

    public IS2ProjectValidationStatus validate( IS2Project s2Project_, IProgressMonitor monitor )
    {
        validationStatus = null;

        if ( s2Project != null && s2Project != s2Project_ )
        {
            throw new IllegalStateException(
                                             "The validate method cannot be called for different projects on the same validator instance" );
        }
        s2Project = s2Project_;

        log.debug( "Validating eclipse installation for S2 project: {}", s2Project.getName() );
        long start = System.currentTimeMillis();

        monitor.beginTask( "Validating eclipse installation", 1 );

        try
        {
            if ( s2Project.getP2LineupLocation() == null )
            {
                validationStatus =
                    S2ProjectValidationStatus.getOKStatus( this, "OK (The '" + s2Project.getName()
                        + "' S2 project does not have a p2 lineup URL.)" );
                return validationStatus;
            }

            try
            {
                targetProfile = getSelfProfile();
                if ( targetProfile == null )
                {
                    return createErrorStatus( "Cannot get the p2 profile for this eclipse installation - this is expected if eclipse was started from an eclipse application launch configuration." );
                }
                log.debug( "ProfileId={}", targetProfile.getProfileId() );

                IP2Lineup p2Lineup = loadP2Lineup( s2Project, monitor );
                String p2LineupUrlStr = s2Project.getP2LineupLocation().getUrl();
                p2LineupUri = new URI( p2LineupUrlStr );

                boolean cleanP2Credentials = setCredentialsForUri( p2LineupUri );
                try
                {
                    try
                    {
                        p2LineupIU = getP2LineupInstallableUnit( p2LineupUri, p2Lineup, monitor );
                    }
                    catch ( S2ProjectValidationException e )
                    {
                        return createErrorStatus( e );
                    }
                    log.debug( "p2 lineup installable unit: {}", p2LineupIU );

                    Set<IRequirement> missingCapabilities = new LinkedHashSet<IRequirement>();
                    Collection<IRequirement> requiredCapabilities = p2LineupIU.getRequirements();
                    for ( IRequirement requiredCapability : requiredCapabilities )
                    {

                        log.debug( "Required installable unit: id={}, version={}.", requiredCapability.toString() );

                        IQueryResult<IInstallableUnit> requirementMetInProfile =
                            targetProfile.available( QueryUtil.createMatchQuery( requiredCapability.getMatches() ),
                                                     monitor );
                        if ( !requirementMetInProfile.isEmpty() )
                        {
                            IInstallableUnit match = requirementMetInProfile.iterator().next();
                            log.debug( "Found installed installable unit: {}, version={}.", match.getId(),
                                       match.getVersion() );
                        }
                        else
                        {
                            log.debug( "Required installable unit:  is not installed.", requiredCapability.toString() );
                            missingCapabilities.add( requiredCapability );
                        }
                    }

                    if ( missingCapabilities.size() == 0 )
                    {
                        // Everything is ok, the current eclipse installation is complete
                        validationStatus = S2ProjectValidationStatus.getOKStatus( this, SUCCESS_MESSAGE );
                        return validationStatus;
                    }

                    // Some installable units are missing from the current eclipse installation
                    StringBuilder message = new StringBuilder( "Missing installable units:" );
                    for ( IRequirement missingCapability : missingCapabilities )
                    {
                        message.append( "\n   " ).append( missingCapability.toString() );
                    }
                    validationStatus = createErrorStatus( message.toString() );
                    validationStatus.setCode( CAN_TRY_REMEDIATE_STATUS_CODE );
                    return validationStatus;
                }
                finally
                {
                    if ( cleanP2Credentials )
                    {
                        P2AuthHelper.removeCredentials( p2LineupUri );
                    }
                }
            }
            catch ( ProvisionException e )
            {
                return createErrorStatus( "Cannot materialize project " + s2Project.getName(), e );
            }
            catch ( URISyntaxException e )
            {
                return createErrorStatus( "Cannot materialize project " + s2Project.getName(), e );
            }
            catch ( IOException e )
            {
                return createErrorStatus( "Cannot materialize project " + s2Project.getName(), e );
            }
            catch ( XmlPullParserException e )
            {
                return createErrorStatus( "Cannot materialize project " + s2Project.getName(), e );
            }
        }
        finally
        {
            monitor.worked( 1 );

            monitor.done();

            log.debug( "S2 project '{}' validated in {} ms.", s2Project.getName(), System.currentTimeMillis() - start );
        }
    }

    private IProfile getSelfProfile()
        throws ProvisionException
    {
        IProfileRegistry registry = (IProfileRegistry) getp2Agent().getService( IProfileRegistry.SERVICE_NAME );
        // Get the profile of the running system.
        String profileId;
        if ( profileIdForUnitTests != null )
        {
            IProfile testProfile = registry.getProfile( profileIdForUnitTests );
            if ( testProfile == null )
            {
                testProfile = registry.addProfile( profileIdForUnitTests );
            }
            return testProfile;
        }
        else
        {
            profileId = IProfileRegistry.SELF;
        }
        return registry.getProfile( profileId );
    }

    private String profileIdForUnitTests;

    public void setProfileIdForUnitTests( String profileIdForUnitTests )
    {
        log.info( "Setting profile id for unit tests to {}", profileIdForUnitTests );
        this.profileIdForUnitTests = profileIdForUnitTests;
    }

    public IS2ProjectValidationStatus canRemediate( boolean headless )
    {
        if ( s2Project == null )
        {
            throw new IllegalStateException( "The canRemediate method was called for an unknown project." );
        }
        if ( validationStatus == null )
        {
            throw new IllegalStateException( "The canRemediate method was called before the validate method." );
        }
        if ( validationStatus.isOK() )
        {
            // Nothing to remediate
            log.warn( "The canRemediate method was called, but the validation was successful." );
            return createErrorStatus( "Nothing to remediate" );
        }

        if ( validationStatus.getCode() != CAN_TRY_REMEDIATE_STATUS_CODE )
        {
            log.debug( "Cannot remediate eclipse installation: validationStatus.getCode()={}",
                       validationStatus.getCode() );
            return validationStatus;
        }

        boolean cleanP2Credentials = setCredentialsForUri( p2LineupUri );
        try
        {
            IProgressMonitor monitor = new NullProgressMonitor();
            Collection<IInstallableUnit> toInstall = new ArrayList<IInstallableUnit>( 1 );
            toInstall.add( p2LineupIU );
            installationOperation = new InstallOperation( new ProvisioningSession( getp2Agent() ), toInstall );
            installationOperation.setProfileId( targetProfile.getProfileId() );
            ProvisioningContext provisioningContext = new ProvisioningContext( getp2Agent() );
            provisioningContext.setMetadataRepositories( new URI[] { p2LineupUri } );
            provisioningContext.setArtifactRepositories( new URI[] { p2LineupUri } );
            installationOperation.setProvisioningContext( provisioningContext );

            IStatus status = installationOperation.resolveModal( monitor );

            installationOperation.getProvisioningPlan().setInstallableUnitProfileProperty( toInstall.iterator().next(),
                                                                                           COM_SONATYPE_LINEUP_MATERIALIZED,
                                                                                           "true" );
            log.debug( "Execution of the resolution for the install operation status={}", status );

            // If the p2 lineup IU is already installed, the status severity is WARNING
            // (because we asked p2 to install something that is already installed)
            if ( status.getSeverity() == IStatus.ERROR )
            {
                return createErrorStatus( status );
            }

            return new S2ProjectValidationStatus( this, IStatus.OK,
                                                  "The current eclipse installation can be upgraded to match the requirements for the '"
                                                      + s2Project.getName() + "' S2 project." );
        }
        finally
        {
            if ( cleanP2Credentials )
            {
                P2AuthHelper.removeCredentials( p2LineupUri );
            }
        }
    }

    public IS2ProjectValidationStatus remediate( boolean headless, IProgressMonitor monitor )
    {
        if ( s2Project == null )
        {
            throw new IllegalStateException( "The remediate method was called for an unknown project." );
        }
        if ( validationStatus == null )
        {
            throw new IllegalStateException( "The remediate method was called before the validate method." );
        }
        if ( validationStatus.isOK() )
        {
            // Nothing to remediate
            return S2ProjectValidationStatus.getOKStatus( this,
                                                          "The current eclipse installation already matches the requirements for the '"
                                                              + s2Project.getName() + "' S2 project." );
        }

        if ( headless )
        {
            return remediateHeadless( monitor );
        }
        else
        {
            return remediateWithUI( monitor );
        }
    }

    private IS2ProjectValidationStatus remediateHeadless( IProgressMonitor monitor )
    {
        log.debug( "Remediating eclipse installation (headless) for S2 project: {}", s2Project.getName() );
        long start = System.currentTimeMillis();

        monitor.beginTask( "Remediating eclipse installation", 1 );

        boolean cleanP2Credentials = setCredentialsForUri( p2LineupUri );
        try
        {
            return new S2ProjectValidationStatus(
                                                  this,
                                                  installationOperation.getProvisioningJob( monitor ).runModal( monitor ) );
        }
        finally
        {
            monitor.worked( 1 );

            monitor.done();

            log.debug( "S2 project '{}' remediated in {} ms.", s2Project.getName(), System.currentTimeMillis() - start );

            if ( cleanP2Credentials )
            {
                P2AuthHelper.removeCredentials( p2LineupUri );
            }
        }
    }

    private IS2ProjectValidationStatus remediateWithUI( IProgressMonitor monitor )
    {
        IStatus status = installationOperation.getResolutionResult();
        if ( status.getCode() == IStatus.CANCEL )
            return createErrorStatus( "Remediation cancelled" );

        if ( status.getCode() == IStatus.ERROR )
            return createErrorStatus( status );

        Collection<IInstallableUnit> initialSelection = new ArrayList<IInstallableUnit>( 1 );
        initialSelection.add( p2LineupIU );
        if ( ProvisioningUI.getDefaultUI().openInstallWizard( initialSelection, installationOperation, null ) == Dialog.OK )
        {
            // Note that the provisioning operation is unlikely to be complete when we are returning (e.g. install is
            // likely going on)
            return new S2ProjectValidationStatus( this, status );
        }
        return createErrorStatus( "Remediation cancelled" );
    }

    @Override
    protected IS2ProjectValidationStatus createErrorStatus( String message )
    {
        IS2ProjectValidationStatus status = super.createErrorStatus( message );
        setValidationStatus( status );
        return status;
    }

    @Override
    protected IS2ProjectValidationStatus createErrorStatus( Throwable exception )
    {
        IS2ProjectValidationStatus status = super.createErrorStatus( exception );
        setValidationStatus( status );
        return status;
    }

    @Override
    protected IS2ProjectValidationStatus createErrorStatus( String message, Throwable exception )
    {
        IS2ProjectValidationStatus status = super.createErrorStatus( message, exception );
        setValidationStatus( status );
        return status;
    }

    @Override
    protected IS2ProjectValidationStatus createErrorStatus( IStatus status_ )
    {
        IS2ProjectValidationStatus status = super.createErrorStatus( status_ );
        setValidationStatus( status );
        return status;
    }

    public IP2Lineup loadP2Lineup( String p2LineupUrlStr, IProgressMonitor monitor )
        throws URISyntaxException, IOException, XmlPullParserException
    {
        if ( !p2LineupUrlStr.endsWith( "/" ) )
        {
            p2LineupUrlStr += "/";
        }
        URI p2LineupUri = new URL( new URL( p2LineupUrlStr ), "p2lineup.xml" ).toURI();
        InputStream is =
            new UrlFetcher().openStream( p2LineupUri, monitor, AuthFacade.getAuthService(),
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

    protected IP2Lineup loadP2Lineup( IS2Project s2Project, IProgressMonitor monitor )
        throws URISyntaxException, IOException, XmlPullParserException
    {
        String p2LineupUrlStr = s2Project.getP2LineupLocation().getUrl();
        return loadP2Lineup( p2LineupUrlStr, monitor );
    }

    public String getPluginId()
    {
        return P2ValidatorPlugin.PLUGIN_ID;
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
            log.debug( "The eclipse installation validation is not applicable to a fresh installation." ); //$NON-NLS-1$
            return false;
        }

        return true;
    }

    private IInstallableUnit getPreviouslyMaterialized( IProgressMonitor monitor )
        throws ProvisionException
    {
        log.debug( "Looking for previous lineup materialized." ); //$NON-NLS-1$
        IQueryResult<IInstallableUnit> result =
            getSelfProfile().query( new IUProfilePropertyQuery( COM_SONATYPE_LINEUP_MATERIALIZED, "true" ), monitor );
        if ( result.isEmpty() )
        {
            return null;
        }

        return result.iterator().next();
    }

    public boolean isLineupManaged( IProgressMonitor monitor )
        throws S2ProjectValidationException
    {
        try
        {
            return getPreviouslyMaterialized( monitor ) != null;
        }
        catch ( ProvisionException e )
        {
            throw new S2ProjectValidationException( e );
        }
    }

    public String isUpToDate( IP2LineupLocation lineupLocation, IProgressMonitor monitor )
    {
        log.info( "Checking for IDE update." );

        monitor.subTask( "Checking for IDE update." );
        if ( lineupLocation == null || lineupLocation.getUrl() == null )
        {
            log.info( "The codebase does not specify a lineup url." );
            return IIDEUpdater.UP_TO_DATE;
        }

        // Detect the case where the install is not managed by a lineup
        try
        {
            if ( getPreviouslyMaterialized( monitor ) == null )
            {
                return IIDEUpdater.NOT_LINEUP_MANAGED;
            }
        }
        catch ( ProvisionException e )
        {
            log.error( e.getMessage(), e );
            return IIDEUpdater.ERROR;
        }

        // Validate the lineup location
        URI lineupURI = null;
        try
        {
            lineupURI = new URI( lineupLocation.getUrl() );
        }
        catch ( URISyntaxException e )
        {
            log.error( "Lineup url location is invalid: " + lineupLocation.getUrl(), e );
            return IIDEUpdater.ERROR;
        }

        boolean cleanP2Credentials = setCredentialsForUri( lineupURI );
        try
        {
            IP2Lineup p2Lineup = loadP2Lineup( lineupLocation.getUrl(), monitor );
            String p2LineupIUId = P2LineupHelper.getMasterInstallableUnitId( p2Lineup );
            Version p2LineupIUVersion = Version.parseVersion( p2Lineup.getVersion() );

            IProfile selfProfile = getSelfProfile();
            if (selfProfile == null) 
            {
                log.debug( "Cannot get the self profile." );
                return IIDEUpdater.UNKNOWN;
            }

            IQueryResult<IInstallableUnit> result =
                selfProfile.query( QueryUtil.createIUQuery( p2LineupIUId, p2LineupIUVersion ), monitor );
            if ( !result.isEmpty() )
            {
                return IIDEUpdater.UP_TO_DATE;
            }

            // The lineup may not be up to date or be a different id / version
            return IIDEUpdater.NOT_UP_TO_DATE;
        }
        catch ( ProvisionException e )
        {
            log.error( "Error while loading the profile.", e );
            return IIDEUpdater.ERROR;
        }
        catch ( URISyntaxException e )
        {
            log.error( "Exception loading lineup during check for update.", e );
            return IIDEUpdater.ERROR;
        }
        catch ( IOException e )
        {
            log.error( "Exception loading lineup during check for update.", e );
            return IIDEUpdater.ERROR;
        }
        catch ( XmlPullParserException e )
        {
            log.error( "Exception loading lineup during check for update.", e );
            return IIDEUpdater.ERROR;
        }
        finally
        {
            if ( cleanP2Credentials )
            {
                P2AuthHelper.removeCredentials( URI.create( lineupLocation.getUrl() ) );
            }
        }
    }

    public IStatus performUpdate( String lineupLocation, IProgressMonitor monitor )
    {
        assert lineupLocation != null;

        // Validate the lineup location
        URI lineupURI = null;
        try
        {
            lineupURI = new URI( lineupLocation );
        }
        catch ( URISyntaxException e2 )
        {
            return new Status( IStatus.ERROR, P2ValidatorPlugin.PLUGIN_ID, "The lineup URL is invalid", e2 );
        }

        try
        {
            // Do dependency resolution
            IPlanner planner = (IPlanner) getp2Agent().getService( IPlanner.SERVICE_NAME );
            IProfileChangeRequest request =
                ProfileChangeRequest.createByProfileId( getp2Agent(), getSelfProfile().getProfileId() );

            IInstallableUnit toInstall =
                getP2LineupInstallableUnit( lineupURI, loadP2Lineup( lineupLocation, monitor ), monitor );
            request.add( toInstall );
            IInstallableUnit toUninstall = getPreviouslyMaterialized( monitor );
            if ( toUninstall != null )
            {
                request.remove( toUninstall );
            }
            else
            {
                log.debug( "Nothing found to uninstall." );
                return IIDEUpdater.NOT_LINEUP_CREATED_STATUS;
            }

            // The context is limited to the line
            ProvisioningContext provisioningContext = new ProvisioningContext( getp2Agent() );
            provisioningContext.setMetadataRepositories( new URI[] { URI.create( lineupLocation ) } );
            provisioningContext.setArtifactRepositories( new URI[] { URI.create( lineupLocation ) } );
            IProvisioningPlan plan = planner.getProvisioningPlan( request, provisioningContext, monitor );
            plan.setInstallableUnitProfileProperty( toInstall, COM_SONATYPE_LINEUP_MATERIALIZED, "true" );
            plan.setInstallableUnitProfileProperty( toInstall, IProfile.PROP_PROFILE_ROOT_IU, "true" );
            if ( !plan.getStatus().isOK() )
            {
                log.error( "Resolution failed: " + plan.getStatus().toString() );
                return plan.getStatus();
            }

            // Perform the changes
            ProfileModificationJob job =
                new ProfileModificationJob( "Lineup update", new ProvisioningSession( getp2Agent() ),
                                            plan.getProfile().getProfileId(), plan, null );
            ProvisioningUI.getDefaultUI().manageJob( job, Policy.RESTART_POLICY_PROMPT );
            job.setUser(true);
            job.schedule();
            return Status.OK_STATUS;
        }
        catch ( ProvisionException e )
        {
            return new Status( IStatus.ERROR, P2ValidatorPlugin.PLUGIN_ID,
                               "Internal error occured while updating to lineup: " + lineupLocation, e );
        }
        catch ( URISyntaxException e )
        {
            return new Status( IStatus.ERROR, P2ValidatorPlugin.PLUGIN_ID,
                               "Internal error occured while updating to lineup: " + lineupLocation, e );
        }
        catch ( IOException e )
        {
            return new Status( IStatus.ERROR, P2ValidatorPlugin.PLUGIN_ID,
                               "Internal error occured while updating to lineup: " + lineupLocation, e );
        }
        catch ( XmlPullParserException e )
        {
            return new Status( IStatus.ERROR, P2ValidatorPlugin.PLUGIN_ID,
                               "Internal error occured while updating to lineup: " + lineupLocation, e );
        }
        catch ( S2ProjectValidationException e )
        {
            return new Status( IStatus.ERROR, P2ValidatorPlugin.PLUGIN_ID,
                               "Internal error occured while updating to lineup: " + lineupLocation, e );
        }
    }
}
