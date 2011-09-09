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
package com.sonatype.s2.project.validation.cvs;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.validation.api.IScmAccessData;
import com.sonatype.s2.project.validation.api.IScmAccessValidator;
import com.sonatype.s2.project.validation.api.UnauthorizedStatus;
import com.sonatype.s2.project.validation.cvs.internal.CVSRepositoryLocation;
import com.sonatype.s2.project.validation.cvs.internal.CVSURI;

@SuppressWarnings( "restriction" )
public class CvsAccessValidator
    implements IScmAccessValidator, IExecutableExtension
{
    private final Logger log = LoggerFactory.getLogger( CvsAccessValidator.class );

    static final String CVS_SCM_ID = "scm:cvs:";

    private int priority;

    /*
     * (non-Javadoc)
     * @see com.sonatype.s2.project.validation.api.IScmAccessValidator#accept(java.lang.String)
     */
    public boolean accept( String type )
    {
        return "cvs".equalsIgnoreCase( type );
    }

    /*
     * (non-Javadoc)
     * @see com.sonatype.s2.project.validation.api.IScmAccessValidator#accept(com.sonatype.s2.project.validation.api.
     * IScmAccessData)
     */
    public boolean accept( IScmAccessData data )
    {
        return data.getRepositoryUrl() != null && data.getRepositoryUrl().startsWith( CVS_SCM_ID );
    }

    /*
     * (non-Javadoc)
     * @see com.sonatype.s2.project.validation.api.IScmAccessValidator#getPriority()
     */
    public int getPriority()
    {
        return priority;
    }

    /*
     * (non-Javadoc)
     * @see com.sonatype.s2.project.validation.api.IScmAccessValidator#validate(com.sonatype.s2.project.validation.api.
     * IScmAccessData, org.eclipse.core.runtime.IProgressMonitor)
     */
    public IStatus validate( IScmAccessData data, IProgressMonitor monitor )
    {
        log.debug( "Validating access to {} for {}", data, data.getUsername() );

        CVSRepositoryLocation repository = null;
        try
        {
            CVSURI uri = CVSURI.fromUri( new URI( data.getRepositoryUrl().substring( CVS_SCM_ID.length() ) ) );

            repository = uri.getRepository();

            if ( !repository.getMethod().getName().equals( "pserver" ) )
            {
                return error( NLS.bind( Messages.unsupported_method, repository.getMethod().getName() ), null );
            }

            // We can only set the username/password if it wasn't set in the URI itself
            if ( ( (CVSRepositoryLocation) repository ).isUsernameMutable() )
            {
                repository.setUsername( data.getUsername() != null ? data.getUsername() : "anonymous" );
                repository.setPassword( data.getPassword() != null ? data.getPassword() : "anonymous" );
            }
            // Set an Authenticator which will prevent workbench login prompts
            CvsHelper.setNonInteractiveUserAuthenticator( repository );

            // Validate access to the repository
            repository.validateConnection( monitor );

            if ( uri.getPath() == null )
            {
                return error( Messages.missing_path, null );
            }
        }
        catch ( CVSException e )
        {
            if ( e.getStatus() instanceof CVSStatus && e.getStatus().getCode() == CVSStatus.AUTHENTICATION_FAILURE )
            {
                if ( repository != null )
                {
                    if ( repository.isUsernameMutable() )
                    {
                        return new UnauthorizedStatus( IStatus.ERROR, getClass().getPackage().toString(),
                                                       e.getStatus().getMessage(), e );
                    }
                    else
                    {
                        return new Status( IStatus.ERROR, getClass().getPackage().toString(), Messages.username_in_uri,
                                           e );
                    }
                }
            }
            return e.getStatus();
        }
        catch ( OperationCanceledException e )
        {
            return new Status( IStatus.CANCEL, getClass().getName(), -1, e.getMessage(), e );
        }
        catch ( URISyntaxException e )
        {
            return error( "Invalid repository format, supported formats are:\n cvs://[:]method:user[:password]@host:[port]/root/path#project/path[,tagName] \n cvs://_method_user[_password]~host_[port]!root!path/project/path[?<version,branch,date,revision>=tagName]",
                          e );
        }
        catch ( Exception e )
        {
            if ( e instanceof CVSException && e.getCause() != null && e.getCause() instanceof CoreException )
            {
                return ( (CoreException) e.getCause() ).getStatus();
            }
            return error( e.getMessage(), e );
        }
        return Status.OK_STATUS;
    }

    private IStatus error( String message, Exception e )
    {
        return new Status( IStatus.ERROR, getClass().getPackage().toString(), -1, message, e );
    }

    /*
     * (non-Javadoc)
     * @see
     * org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement
     * , java.lang.String, java.lang.Object)
     */
    public void setInitializationData( IConfigurationElement config, String propertyName, Object data )
        throws CoreException
    {
        String priority = config.getAttribute( "priority" );

        if ( priority != null )
        {
            try
            {
                this.priority = Integer.parseInt( priority );
            }
            catch ( Exception ex )
            {
                log.error( "Unable to parse priority for " + getClass().getName(), ex );
            }
        }
    }

}
