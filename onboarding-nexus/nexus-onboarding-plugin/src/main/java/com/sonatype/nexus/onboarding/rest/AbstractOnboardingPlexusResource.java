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
package com.sonatype.nexus.onboarding.rest;

import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;

import com.sonatype.nexus.onboarding.rest.dto.xstream.XStreamUtil;
import com.thoughtworks.xstream.XStream;

public abstract class AbstractOnboardingPlexusResource
    extends AbstractNexusPlexusResource
{
    @Requirement
    protected Logger logger;

    @Override
    public final void configureXStream( XStream xstream )
    {
        super.configureXStream( xstream );
        XStreamUtil.initializeXStream( xstream );
    }
}
