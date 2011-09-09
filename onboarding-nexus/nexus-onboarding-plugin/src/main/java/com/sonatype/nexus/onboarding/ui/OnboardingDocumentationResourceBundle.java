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
package com.sonatype.nexus.onboarding.ui;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.plugins.rest.AbstractDocumentationNexusResourceBundle;
import org.sonatype.nexus.plugins.rest.NexusResourceBundle;

@Component( role = NexusResourceBundle.class, hint = "OnboardingDocumentationResourceBundle" )
public class OnboardingDocumentationResourceBundle
    extends AbstractDocumentationNexusResourceBundle
{

    @Override
    public String getPluginId()
    {
        return "nexus-onboarding-plugin";
    }

    @Override
    public String getDescription()
    {
        return "Onboarding Plugin API";
    }
}
