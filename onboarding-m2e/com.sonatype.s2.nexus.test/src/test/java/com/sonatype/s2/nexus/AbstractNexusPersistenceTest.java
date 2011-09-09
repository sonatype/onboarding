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
package com.sonatype.s2.nexus;

import java.net.URI;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.maven.ide.eclipse.authentication.internal.SimpleAuthService;

public abstract class AbstractNexusPersistenceTest
    extends TestCase
{
    protected static final String NEXUS_URL = "http://localhost:8081/nexus";

    protected static final String NEXUS_USERNAME = "admin";

    protected static final String NEXUS_PASSWORD = "admin123";

    protected static final IProgressMonitor monitor = new NullProgressMonitor();

    protected static final boolean enabled = false;

    protected static SimpleAuthService simpleAuthService =
        new SimpleAuthService( SecurePreferencesFactory.getDefault() );

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            simpleAuthService.removeURI( new URI( NEXUS_URL ) );
        }
        finally
        {
            super.tearDown();
        }
    }
}
