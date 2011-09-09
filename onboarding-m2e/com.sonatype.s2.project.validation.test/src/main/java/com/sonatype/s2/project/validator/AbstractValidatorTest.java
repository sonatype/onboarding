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

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthRealm;

public abstract class AbstractValidatorTest
    extends TestCase
{
    protected static final IProgressMonitor monitor = new NullProgressMonitor();

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            AuthFacade.getAuthRegistry().clear();
        }
        finally
        {
            super.tearDown();
        }
    }

    protected void addRealmAndURL( String realmId, String url, String username, String password )
    {
        addRealmAndURL( realmId, url, AnonymousAccessType.ALLOWED, username, password );
    }

    protected void addRealmAndURL( String realmId, String url, AnonymousAccessType anonymousAccessType,
                                   String username, String password )
    {
        IAuthRealm realm =
            AuthFacade.getAuthRegistry().addRealm( realmId, realmId, realmId, AuthenticationType.USERNAME_PASSWORD,
                                                   monitor );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( url, realm.getId(), anonymousAccessType, monitor );
        AuthFacade.getAuthService().save( url, username, password );
    }
}
