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
package com.sonatype.nexus.onboarding.its;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.testng.annotations.BeforeMethod;

public abstract class AbstractOnboardingIT
    extends AbstractNexusIntegrationTest
{
    @Override
    @BeforeMethod
    public void oncePerClassSetUp()
        throws Exception
    {
        super.oncePerClassSetUp();
    }

    public AbstractOnboardingIT()
    {
    }

    public AbstractOnboardingIT( String testRepositoryId )
    {
        super( testRepositoryId );
    }
}