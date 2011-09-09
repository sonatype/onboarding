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
package com.sonatype.nexus.p2.lineup.resolver;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.proxy.IllegalOperationException;

import com.sonatype.nexus.p2.rest.model.P2LineupError;
import com.sonatype.nexus.p2.rest.model.P2LineupRepositoryError;
import com.sonatype.nexus.p2.rest.model.P2LineupUnresolvedInstallableUnit;

public class CannotResolveP2LineupException
    extends IllegalOperationException
{
    private static final long serialVersionUID = 4843118245681420894L;
    
    private P2LineupError error;

    private List<P2LineupRepositoryError> repositoryErrors = new ArrayList<P2LineupRepositoryError>();
    
    public List<P2LineupRepositoryError> getRepositoryErrors()
    {
        return repositoryErrors;
    }

    public List<P2LineupUnresolvedInstallableUnit> getUnresolvedInstallableUnits()
    {
        return unresolvedInstallableUnits;
    }

    private List<P2LineupUnresolvedInstallableUnit> unresolvedInstallableUnits =
        new ArrayList<P2LineupUnresolvedInstallableUnit>();

    public CannotResolveP2LineupException()
    {
        super( "" );
    }

    public CannotResolveP2LineupException( String message )
    {
        super( message );
        error = new P2LineupError( message );
    }

    public CannotResolveP2LineupException( Throwable cause )
    {
        super( "Could not resolve the p2 lineup: " + cause.getMessage(), cause );
    }

    public void addRepositoryError( String respositoryURL, String errorMessage )
    {
        repositoryErrors.add( new P2LineupRepositoryError( respositoryURL, errorMessage ) );
    }

    public void addUnresolvedInstallableUnit( P2LineupUnresolvedInstallableUnit iu )
    {
        unresolvedInstallableUnits.add( iu );
    }

    public void addUnresolvedInstallableUnit( String id, String version, String explanation )
    {
        unresolvedInstallableUnits.add( new P2LineupUnresolvedInstallableUnit( id, version, explanation ) );
    }

    @Override
    public String getMessage()
    {
        StringBuilder result = new StringBuilder( "Could not resolve the p2 lineup: " );
        if ( error != null )
        {
            result.append( error.getErrorMessage() ).append( ": " );
        }
        if ( getCause() != null )
        {
            result.append( getCause().getMessage() );
        }
        for ( P2LineupRepositoryError repositoryError : repositoryErrors )
        {
            result.append( "\n" ).append( "Repository: " ).append( repositoryError.getRepositoryURL() );
            result.append( ": " ).append( repositoryError.getErrorMessage() );
        }
        for ( P2LineupUnresolvedInstallableUnit unresolvedInstallableUnit : unresolvedInstallableUnits )
        {
            result.append( "\n" ).append( unresolvedInstallableUnit.getErrorMessage() );
        }
        return result.toString();
    }

    public boolean isEmpty()
    {
        return getCause() == null && repositoryErrors.isEmpty() && unresolvedInstallableUnits.isEmpty() && error == null;
    }

    public boolean isFatal()
    {
        return isFatal( true /* considerRepositoryErrors */);
    }

    public boolean isFatal( boolean considerRepositoryErrors )
    {
        if ( getCause() != null )
        {
            return true;
        }

        if ( error != null )
        {
            if (! error.isWarning())
            {
                return true;
            }
        }

        if ( !unresolvedInstallableUnits.isEmpty() )
        {
            return true;
        }

        if ( considerRepositoryErrors )
        {
            for ( P2LineupRepositoryError repositoryError : repositoryErrors )
            {
                if ( !repositoryError.isWarning() )
                {
                    return true;
                }
            }
        }
        return false;
    }

    public void setError(P2LineupError error) 
    {
        this.error = error;
    }
    
    public P2LineupError getError()
    {
        return error;
    }
}
