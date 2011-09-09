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
package com.sonatype.s2.project.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.ui.common.ErrorHandlingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.IS2ProjectCatalogEntry;
import com.sonatype.s2.project.model.IUrlLocation;
import com.sonatype.s2.project.ui.internal.Activator;
import com.sonatype.s2.project.ui.internal.Messages;
import com.sonatype.s2.project.ui.internal.ProjectData;
import com.sonatype.s2.project.validation.api.S2ProjectValidationContext;
import com.sonatype.s2.project.validator.AccessValidationStatus;
import com.sonatype.s2.project.validator.ValidationFacade;

/**
 * Overrides page management to build dynamic page sequences.
 */
abstract public class AbstractMaterializationWizard
    extends Wizard
{
    private static Logger log = LoggerFactory.getLogger( AbstractMaterializationWizard.class );

    protected IS2Project project = null;

    private S2ProjectValidationContext validationContext = null;

    protected String projectDescriptorUrl;

    protected IStatus validationStatus = null;

    private List<IWizardPage> pages;

    protected UserSettingsPage userSettingsPage;

    protected ProjectValidationPage projectValidationPage;

    private Map<String, List<IStatus>> accessStatuses = null;

    protected boolean accessErrorsFound = false;

    private Throwable error = null;

    public Throwable getError()
    {
        return error;
    }

    public void setError( Throwable error )
    {
        this.error = error;
    }

    protected AbstractMaterializationWizard()
    {
        pages = new ArrayList<IWizardPage>();
        accessStatuses = new LinkedHashMap<String, List<IStatus>>();
        setNeedsProgressMonitor( true );
    }

    public void setProject( IS2ProjectCatalogEntry entry )
        throws CoreException
    {
        setProject( new ProjectData( entry ) );
    }

    public void setProject( final String url )
        throws CoreException
    {
        setProject( new ProjectData( url ) );
    }

    public void setProject( final ProjectData projectData )
        throws CoreException
    {
        final Exception[] errors = new Exception[] { null };

        try
        {
            getContainer().run( true, true, new IRunnableWithProgress()
            {
                public void run( IProgressMonitor monitor )
                    throws InvocationTargetException, InterruptedException
                {
                    try
                    {
                        projectData.load( monitor );
                        projectDescriptorUrl = projectData.getUrl();
                        Display.getDefault().syncExec( new Runnable()
                        {
                            public void run()
                            {
                                try
                                {
                                    setProject( projectData.getProject() );
                                }
                                catch ( Exception e )
                                {
                                    errors[0] = e;
                                }
                            }
                        } );
                    }
                    catch ( CoreException e )
                    {
                        Throwable cause = e.getStatus().getException();
                        String message = ErrorHandlingUtils.convertNexusIOExceptionToUIText( cause, 
                                                  Messages.materializationWizard_urlPage_errors_authenticationError,
                                                  Messages.materializationWizard_errors_forbidden, 
                                                  Messages.materializationWizard_errors_notfound);
                        errors[0] =
                            message != null ? new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                                                                             message, cause ) ) : e;
                    }
                }
            } );
        }
        catch ( InvocationTargetException e )
        {
            errors[0] = e;
        }
        catch ( InterruptedException e )
        {
            errors[0] = e;
        }
        if ( errors[0] != null )
        {
            error = errors[0];
            IStatus status =
                error instanceof CoreException ? ( (CoreException) error ).getStatus()
                                : new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                                              NLS.bind( Messages.materializationWizard_errors_errorLoadingProject,
                                                        error.getMessage() ), error );

            log.error( status.getMessage(), error );
            throw new CoreException( status );
        }
    }

    public void setProject( IS2Project project )
    {
        this.project = project;
        validationStatus = null;

        if ( project.getName() != null )
        {
            setWindowTitle( getWindowTitle() + " - " + project.getName() ); //$NON-NLS-1$
            getContainer().getCurrentPage().setDescription( Messages.materializationWizard_urlPage_description + ": " //$NON-NLS-1$
                                                                + project.getName() ); 
        }
        removePagesAfterCurrent();

        addMaterializationPages();
    }

    protected void removePagesAfterCurrent()
    {
        IWizardPage currentPage = getContainer().getCurrentPage();
        for ( int i = pages.size() - 1; i >= 0; i-- )
        {
            IWizardPage page = pages.get( i );
            if ( page == currentPage )
            {
                break;
            }
            pages.remove( i );
            page.dispose();
        }
    }

    protected void addMaterializationPages()
    {
        if ( validationStatus == null )
        {
            validateProject();
        }

        parseStatus();

        for ( String realmId : accessStatuses.keySet() )
        {
            List<IStatus> statuses = accessStatuses.get( realmId );

            // If the project has not been validated, create pages for each realm.
            // Otherwise, only show realm pages with errors.
            boolean addThisPage = validationStatus == null;
            boolean isScm = false;
//            if ( !addThisPage )
//            {
                for ( IStatus status : statuses )
                {
                    if ( !status.isOK() )
                    {
                        addThisPage = true;
                    }
                    //mkleint: is there a better way to find out if a page shall be a scm page? 
                    if (status instanceof AccessValidationStatus) {
                        AccessValidationStatus avs = (AccessValidationStatus)status;
                        if (avs.getLocation() instanceof IUrlLocation) {
                            IUrlLocation l = (IUrlLocation)avs.getLocation();
                            if (l.getUrl().startsWith( "scm:" )) { //$NON-NLS-1$
                                isScm = true;
                            }
                        }
                    }
                    if ( addThisPage && isScm ) break;
                }
//            }
            if ( addThisPage )
            {
                addPage( new ProjectScmSecurityRealmsPage( project, realmId, statuses, isScm ) );
            }
        }

        if ( project.getMavenSettingsLocation() != null )
        {
            userSettingsPage = new UserSettingsPage( project );
            addPage( userSettingsPage );
        }

        projectValidationPage =
            new ProjectValidationPage( project, getValidationContext(), accessErrorsFound ? null : validationStatus );
        addPage( projectValidationPage );
    }

    private void parseStatus()
    {
        resetParsedStatus();

        if ( validationStatus == null )
        {
            return;
        }

        if ( validationStatus.isMultiStatus() )
        {
            parseAccessStatus( validationStatus );
        }
    }

    private void resetParsedStatus()
    {
        accessErrorsFound = false;
        accessStatuses.clear();
    }

    private void parseAccessStatus( IStatus status )
    {
        if ( status instanceof AccessValidationStatus )
        {
            AccessValidationStatus accessStatus = (AccessValidationStatus) status;
            if ( !accessStatus.isOK() )
            {
                accessErrorsFound = true;
            }

            String realmId;
            IAuthRealm realm = AuthFacade.getAuthRegistry().getRealmForURI( accessStatus.getLocation().getUrl() );
            if ( realm != null )
            {
                realmId = realm.getId();
            }
            else
            {
                realmId = accessStatus.getLocation().getUrl();
            }
            List<IStatus> realmStatuses = accessStatuses.get( realmId );
            if ( realmStatuses == null )
            {
                realmStatuses = new ArrayList<IStatus>();
                accessStatuses.put( realmId, realmStatuses );
            }
            realmStatuses.add( accessStatus );
        }

        if ( status.isMultiStatus() )
        {
            for ( IStatus child : status.getChildren() )
            {
                parseAccessStatus( child );
            }
        }
    }

    protected void validateProject()
    {
        Throwable exception = null;
        try
        {
            getContainer().run( true, true, new IRunnableWithProgress()
            {

                public void run( IProgressMonitor monitor )
                    throws InvocationTargetException, InterruptedException
                {
                    monitor.beginTask( Messages.actions_materializeProject_tasks_validatingProjectRequirements, 1 );
                    try
                    {
                        validationStatus =
                            ValidationFacade.getInstance().validate( project, validationContext, monitor, true );
                    }
                    catch ( CoreException e )
                    {
                        log.error( Messages.materializationWizard_errors_couldNotValidate, e );
                        validationStatus = e.getStatus();
                    }
                }
            } );
        }
        catch ( InvocationTargetException e )
        {
            exception = e.getTargetException();
        }
        catch ( InterruptedException e )
        {
            exception = e;
        }
        if ( exception != null )
        {
            String message = NLS.bind( Messages.materializationWizard_errors_couldNotValidate, exception.getMessage() );
            log.error( message, exception );
            validationStatus = new Status( IStatus.ERROR, Activator.PLUGIN_ID, message, exception );
        }
    }

    @Override
    public void addPage( IWizardPage page )
    {
        pages.add( page );
        page.setWizard( this );
    }

    protected void removePage( IWizardPage page )
    {
        if ( pages.remove( page ) )
        {
            page.dispose();
        }
    }

    @Override
    public boolean canFinish()
    {
        for ( IWizardPage page : pages )
        {
            if ( !page.isPageComplete() )
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public void createPageControls( Composite pageContainer )
    {
        for ( IWizardPage page : pages )
        {
            page.createControl( pageContainer );
            Assert.isNotNull( page.getControl() );
        }
    }

    @Override
    public void dispose()
    {
        for ( IWizardPage page : pages )
        {
            page.dispose();
        }
        super.dispose();
    }

    @Override
    public IWizardPage getNextPage( IWizardPage page )
    {
        int n = pages.indexOf( page );
        if ( n > -1 && n < pages.size() - 1 )
        {
            IWizardPage nextPage = pages.get( n + 1 );
            if ( nextPage != null && nextPage == userSettingsPage && !userSettingsPage.loadVariables() )
            {
                pages.remove( userSettingsPage );
                userSettingsPage.dispose();
                userSettingsPage = null;
                return getNextPage( page );
            }
            return nextPage;
        }
        return null;
    }

    @Override
    public IWizardPage getPage( String name )
    {
        if ( name != null )
        {
            for ( IWizardPage page : pages )
            {
                if ( name.equals( page.getName() ) )
                {
                    return page;
                }
            }
        }
        return null;
    }

    @Override
    public int getPageCount()
    {
        return pages.size();
    }

    @Override
    public IWizardPage[] getPages()
    {
        return pages.toArray( new IWizardPage[pages.size()] );
    }

    @Override
    public IWizardPage getPreviousPage( IWizardPage page )
    {
        int n = pages.indexOf( page );
        if ( n > 0 )
        {
            return pages.get( n - 1 );
        }
        return null;
    }

    @Override
    public IWizardPage getStartingPage()
    {
        return pages.size() == 0 ? null : pages.get( 0 );
    }

    @Override
    public boolean needsPreviousAndNextButtons()
    {
        return true;
    }

    public S2ProjectValidationContext getValidationContext()
    {
        return validationContext;
    }

    public void setValidationContext( S2ProjectValidationContext validationContext )
    {
        this.validationContext = validationContext;
    }
}
