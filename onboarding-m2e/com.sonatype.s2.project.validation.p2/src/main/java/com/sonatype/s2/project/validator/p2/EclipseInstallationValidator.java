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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.console.ProvisioningHelper;
import org.eclipse.equinox.internal.p2.ui.PlanAnalyzer;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.DefaultPhaseSet;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.engine.IUProfilePropertyQuery;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.ResolutionResult;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.InstallAction;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.ProvisioningWizardDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;
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
import com.sonatype.s2.project.validator.p2.ui.P2PreselectedIUInstallWizard;

@SuppressWarnings( "restriction" )
public class EclipseInstallationValidator
    extends AbstractProjectValidator
    implements IS2ProjectValidator, IIDEUpdater
{
    private static final String COM_SONATYPE_LINEUP_METARIALIZED = "com.sonatype.lineup.metarialized";

    private static final Logger log = LoggerFactory.getLogger( EclipseInstallationValidator.class );

    public static final String SUCCESS_MESSAGE =
        "The current eclipse installation meets all requirements specified in the associated p2 lineup.";

    public static final int CAN_TRY_REMEDIATE_STATUS_CODE = 100;

    private IInstallableUnit p2LineupInstallableUnit;

    private IProfile targetProfile;

    private ProfileChangeRequest profileChangeRequest;

    private ProvisioningContext provisioningContext;

    private PlannerResolutionOperation resolutionOperation;

    public PlannerResolutionOperation getResolutionOperation()
    {
        return resolutionOperation;
    }

    public ProvisioningContext getProvisioningContext()
    {
        return provisioningContext;
    }

    public ProfileChangeRequest getProfileChangeRequest()
    {
        return profileChangeRequest;
    }

    public String getTargetProfileId()
    {
        if ( targetProfile == null )
        {
            return null;
        }
        return targetProfile.getProfileId();
    }

    public IInstallableUnit getP2LineupInstallableUnit()
    {
        return p2LineupInstallableUnit;
    }

    private IInstallableUnit getP2LineupInstallableUnit( URI p2LineupUri, IP2Lineup p2Lineup, IProgressMonitor monitor )
        throws S2ProjectValidationException
    {
        String p2LineupIUId = P2LineupHelper.getMasterInstallableUnitId( p2Lineup );
        Version p2LineupIUVersion = Version.parseVersion( p2Lineup.getVersion() );

        Query query = new InstallableUnitQuery( p2LineupIUId, p2LineupIUVersion );
        Collector collector = ProvisioningHelper.getInstallableUnits( p2LineupUri, query, monitor );
        if ( collector.size() == 0 )
        {
            throw new S2ProjectValidationException( "Cannot find p2 lineup installable unit with id=" + p2LineupIUId
                + ", version=" + p2LineupIUVersion + " at " + p2LineupUri );
        }

        p2LineupInstallableUnit = (IInstallableUnit) collector.iterator().next();
        return p2LineupInstallableUnit;
    }

    private IS2Project s2Project;

    private URI p2LineupUri;

    private IInstallableUnit p2LineupIU;

    private IS2ProjectValidationStatus validationStatus;

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

                    Collector profileIUs = targetProfile.available( InstallableUnitQuery.ANY, new Collector(), monitor );
                    Set<IRequiredCapability> missingCapabilities = new LinkedHashSet<IRequiredCapability>();
                    IRequiredCapability[] requiredCapabilities = p2LineupIU.getRequiredCapabilities();
                    for ( IRequiredCapability requiredCapability : requiredCapabilities )
                    {
                        if ( !IInstallableUnit.NAMESPACE_IU_ID.equals( requiredCapability.getNamespace() ) )
                        {
                            return createErrorStatus( "p2 lineup installable unit has required capability with namespace='"
                                + requiredCapability.getNamespace() + "'" );
                        }

                        String iuId = requiredCapability.getName();
                        VersionRange iuRange = requiredCapability.getRange();
                        log.debug( "Required installable unit: id={}, version={}.", iuId, iuRange );

                        boolean found = false;
                        Iterator<IInstallableUnit> profileIUsIter = profileIUs.iterator();
                        while ( profileIUsIter.hasNext() )
                        {
                            IInstallableUnit profileIU = profileIUsIter.next();
                            if ( iuId.equals( profileIU.getId() ) && iuRange.isIncluded( profileIU.getVersion() ) )
                            {
                                log.debug( "Found installed installable unit: {}, version={}.", profileIU.getId(),
                                           profileIU.getVersion() );
                                found = true;
                                break;
                            }
                        }
                        if ( !found )
                        {
                            log.debug( "Required installable unit: id={}, version={} is not installed.", iuId, iuRange );
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
                    for ( IRequiredCapability missingCapability : missingCapabilities )
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
        // Get the profile of the running system.
        String profileId;
        if ( profileIdForUnitTests != null )
        {
            IProfile testProfile = ProvisioningUtil.getProfile( profileIdForUnitTests );
            if ( testProfile == null )
            {
                testProfile = ProvisioningUtil.addProfile( profileIdForUnitTests, null, new NullProgressMonitor() );
            }
            return testProfile;
        }
        else
        {
            profileId = IProfileRegistry.SELF;
        }
        return ProvisioningUtil.getProfile( profileId );
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
            MultiStatus status = PlanAnalyzer.getProfileChangeAlteredStatus();
            profileChangeRequest =
                InstallAction.computeProfileChangeRequest( new IInstallableUnit[] { p2LineupIU },
                                                           targetProfile.getProfileId(), status,
                                                           new NullProgressMonitor() );
            log.debug( "ProfileChangeRequest status={}", status );
            // If the p2 lineup IU is already installed, the status severity is WARNING
            // (because we asked p2 to install something that is already installed)
            if ( status.getSeverity() == IStatus.ERROR )
            {
                return createErrorStatus( status );
            }
            if ( status.getSeverity() == IStatus.CANCEL )
            {
                throw new OperationCanceledException();
            }

            provisioningContext = new ProvisioningContext( new URI[] { p2LineupUri } );
            provisioningContext.setArtifactRepositories( new URI[] { p2LineupUri } );
            resolutionOperation =
                new PlannerResolutionOperation( ProvUIMessages.ProfileModificationAction_ResolutionOperationLabel,
                                                targetProfile.getProfileId(), profileChangeRequest,
                                                provisioningContext, status, false // isUser
                );
            IStatus newStatus = resolutionOperation.execute( monitor );
            log.debug( "PlannerResolutionOperation.execute status={}", status );
            if ( newStatus.getSeverity() == IStatus.ERROR )
            {
                return createErrorStatus( newStatus );
            }
            ResolutionResult resolutionResult = resolutionOperation.getResolutionResult();

            // ProvisioningPlan p2Plan =
            // ProvisioningUtil.getProvisioningPlan( profileChangeRequest, provisioningContext, monitor );
            // ResolutionResult resolutionResult =
            // PlanAnalyzer.computeResolutionResult( profileChangeRequest, p2Plan, status );
            newStatus = resolutionResult.getSummaryStatus();
            log.debug( "ResolutionResult status={}", newStatus );
            if ( newStatus.getSeverity() == IStatus.ERROR )
            {
                return createErrorStatus( newStatus );
            }
            if ( newStatus.getSeverity() == IStatus.CANCEL )
            {
                throw new OperationCanceledException();
            }
            return new S2ProjectValidationStatus( this, IStatus.OK,
                                                  "The current eclipse installation can be upgraded to match the requirements for the '"
                                                      + s2Project.getName() + "' S2 project." );
        }
        catch ( ProvisionException e )
        {
            log.error( e.getMessage(), e );
            return createErrorStatus( e );
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
            ProvisioningPlan p2Plan =
                ProvisioningUtil.getProvisioningPlan( profileChangeRequest, provisioningContext, monitor );
            IStatus status =
                ProvisioningUtil.performProvisioningPlan( p2Plan, new DefaultPhaseSet(), provisioningContext, monitor );
            return new S2ProjectValidationStatus( this, status );
        }
        catch ( ProvisionException e )
        {
            log.error( e.getMessage(), e );
            return createErrorStatus( e );
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
        P2PreselectedIUInstallWizard wizard =
            new P2PreselectedIUInstallWizard( Policy.getDefault(), this.getTargetProfileId(),
                                              new IInstallableUnit[] { getP2LineupInstallableUnit() },
                                              getResolutionOperation(), null );
        WizardDialog p2InstallDialog = new ProvisioningWizardDialog( Display.getDefault().getActiveShell(), wizard );
        p2InstallDialog.create();

        if ( p2InstallDialog.open() == Dialog.OK )
        {
            return new S2ProjectValidationStatus( this, wizard.getStatus() );
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

    public IP2Lineup loadP2Lineup( IS2Project s2Project, IProgressMonitor monitor )
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
            log.debug( "The eclipse installation validation is not applicable to a fresh installation." );
            return false;
        }

        return true;
    }

    public IInstallableUnit getPreviouslyMaterialized( IProgressMonitor monitor )
        throws ProvisionException
    {
        log.debug( "Looking for previous lineup materialized." ); //$NON-NLS-1$
        Collector result =
            getSelfProfile().query( new IUProfilePropertyQuery( getSelfProfile(), COM_SONATYPE_LINEUP_METARIALIZED,
                                                                null ), new Collector(), monitor );
        if ( result.isEmpty() )
        {
            return null;
        }

        return (IInstallableUnit) result.iterator().next();
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
        monitor.subTask( "Checking for IDE update." );
        if ( lineupLocation == null || lineupLocation.getUrl() == null )
            return IIDEUpdater.UP_TO_DATE;

        // Validate the lineup location
        URI lineupURI = null;
        try
        {
            lineupURI = new URI( lineupLocation.getUrl() );
        }
        catch ( URISyntaxException e2 )
        {
            log.debug( "Lineup url location is invalid: " + lineupLocation.getUrl() );
            return IIDEUpdater.ERROR;
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

        boolean cleanP2Credentials = setCredentialsForUri( lineupURI );
        try
        {
            IP2Lineup p2Lineup = null;
            p2Lineup = loadP2Lineup( lineupLocation.getUrl(), monitor );
            String p2LineupIUId = P2LineupHelper.getMasterInstallableUnitId( p2Lineup );
            Version p2LineupIUVersion = Version.parseVersion( p2Lineup.getVersion() );

            Collector result =
                getSelfProfile().query( new InstallableUnitQuery( p2LineupIUId, p2LineupIUVersion ), new Collector(),
                                        monitor );
            if ( !result.isEmpty() )
            {
                return IIDEUpdater.UP_TO_DATE;
            }

            // The line up may not be up to date or be a different id / version
            return IIDEUpdater.NOT_UP_TO_DATE;
        }
        catch ( ProvisionException e )
        {
            log.debug( "Error while loading the profile.", e );
            return IIDEUpdater.ERROR;
        }
        catch ( URISyntaxException e1 )
        {
            log.debug( "Exception loading lineup during check for update.", e1 );
            return IIDEUpdater.ERROR;
        }
        catch ( IOException e1 )
        {
            log.debug( "Exception loading lineup during check for update.", e1 );
            return IIDEUpdater.ERROR;
        }
        catch ( XmlPullParserException e1 )
        {
            log.debug( "Exception loading lineup during check for update.", e1 );
            return IIDEUpdater.ERROR;
        }
        finally
        {
            if ( cleanP2Credentials )
                P2AuthHelper.removeCredentials( URI.create( lineupLocation.getUrl() ) );
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
            IPlanner planner = ProvisioningUtil.getPlanner();
            ProfileChangeRequest request = new ProfileChangeRequest( getSelfProfile() );
            IInstallableUnit toInstall= getP2LineupInstallableUnit( lineupURI, loadP2Lineup( lineupLocation, monitor ),
                                                monitor );
            request.addInstallableUnits( new IInstallableUnit[] { toInstall } );

            IInstallableUnit toUninstall = getPreviouslyMaterialized( monitor );
            if ( toUninstall != null )
            {
                request.removeInstallableUnits( new IInstallableUnit[] { toUninstall } );
            }
            else
            {
                log.debug( "Nothing found to uninstall." );
                return IIDEUpdater.NOT_LINEUP_CREATED_STATUS;
            }

            request.setInstallableUnitProfileProperty( toInstall, COM_SONATYPE_LINEUP_METARIALIZED, "true" );
            ProvisioningPlan plan = planner.getProvisioningPlan( request, null, monitor );
            if ( !plan.getStatus().isOK() )
            {
                log.error( "Resolution failed: " + plan.getStatus().toString() );
                return plan.getStatus();
            }

            // Perform the changes
            ProfileModificationOperation job =
                new ProfileModificationOperation( "Lineup update", getSelfProfile().getProfileId(), plan, null, null, true );
            ProvisioningOperationRunner.requestRestart( true );
            ProvisioningOperationRunner.schedule( job, StatusManager.NONE );
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
