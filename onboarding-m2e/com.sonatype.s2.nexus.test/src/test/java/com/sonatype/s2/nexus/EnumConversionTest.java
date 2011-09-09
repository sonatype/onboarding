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

import junit.framework.TestCase;

import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthenticationType;

import com.sonatype.s2.securityrealm.model.S2AnonymousAccessType;
import com.sonatype.s2.securityrealm.model.S2SecurityRealmAuthenticationType;

/**
 * Tests that enums in org.maven.ide.eclipse.authentication are identical to enums in
 * com.sonatype.s2.securityrealm.model. These two projects cannot depend on each other and introducing another bundle
 * just for these enums would complicate dependencies between Nexus plugins and mse.
 */
public class EnumConversionTest
    extends TestCase
{
    public void testAnonymousAccessType()
    {
        for ( AnonymousAccessType type : AnonymousAccessType.values() )
        {
            S2AnonymousAccessType.valueOf( type.toString() );
        }
        for ( S2AnonymousAccessType type : S2AnonymousAccessType.values() )
        {
            AnonymousAccessType.valueOf( type.toString() );
        }
    }

    public void testSecurityRealmAuthenticationType()
    {
        for ( AuthenticationType type : AuthenticationType.values() )
        {
            S2SecurityRealmAuthenticationType.valueOf( type.toString() );
        }
        for ( S2SecurityRealmAuthenticationType type : S2SecurityRealmAuthenticationType.values() )
        {
            AuthenticationType.valueOf( type.toString() );
        }
    }
}
