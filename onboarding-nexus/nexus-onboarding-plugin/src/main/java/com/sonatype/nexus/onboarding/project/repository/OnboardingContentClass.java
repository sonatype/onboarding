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
package com.sonatype.nexus.onboarding.project.repository;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.registry.AbstractIdContentClass;
import org.sonatype.nexus.proxy.registry.ContentClass;

@Component( role = ContentClass.class, hint = OnboardingContentClass.ID )
public class OnboardingContentClass
    extends AbstractIdContentClass
    implements ContentClass
{
    public static final String ID = "Onboarding";

    public String getId()
    {
        return ID;
    }
    
    @Override
    public boolean isCompatible( ContentClass contentClass )
    {
        boolean result = super.isCompatible( contentClass )
            || contentClass.getId().equals( Maven2ContentClass.ID );
        
        return result;
    }

}
