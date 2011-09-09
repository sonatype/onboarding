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
package com.sonatype.s2.project.validation.api;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;

public abstract class AbstractProjectValidator
    implements IS2ProjectValidator
{
    private String name;

    public void configure( IConfigurationElement configuration )
    {
        name = configuration.getDeclaringExtension().getLabel();
    }

    public String getName()
    {
        return name;
    }

    public String getCategory()
    {
        return null;
    }

    protected IS2ProjectValidationStatus createWarningStatus( String message )
    {
        return createWarningStatus( message, null );
    }

    protected IS2ProjectValidationStatus createWarningStatus( String message, Throwable exception )
    {
        return new S2ProjectValidationStatus( this, IStatus.WARNING, -1 /* code */, message, exception );
    }

    protected IS2ProjectValidationStatus createErrorStatus( String message )
    {
        return createErrorStatus( message, null );
    }

    protected IS2ProjectValidationStatus createErrorStatus( Throwable exception )
    {
        return createErrorStatus( exception.getMessage(), exception );
    }

    protected IS2ProjectValidationStatus createErrorStatus( String message, Throwable exception )
    {
        return new S2ProjectValidationStatus( this, IStatus.ERROR, -1 /* code */, message, exception );
    }

    protected IS2ProjectValidationStatus createErrorStatus( IStatus status )
    {
        return new S2ProjectValidationStatus( this, status );
    }

    protected static boolean urlsEquals( String url1, String url2 )
    {
        if ( url1.endsWith( "/" ) )
        {
            url1 = url1.substring( 0, url1.length() - 1 );
        }
        if ( url2.endsWith( "/" ) )
        {
            url2 = url2.substring( 0, url2.length() - 1 );
        }
        return url1.equals( url2 );
    }

    public boolean isApplicable( S2ProjectValidationContext validationContext )
    {
        return true;
    }
}
