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
package com.sonatype.nexus.p2.security;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.security.realms.tools.AbstractStaticSecurityResource;
import org.sonatype.security.realms.tools.StaticSecurityResource;

@Component( role = StaticSecurityResource.class, hint = "NexusP2SecurityResource" )
public class NexusP2SecurityResource
    extends AbstractStaticSecurityResource
{
    public String getResourcePath()
    {
        return "/META-INF/nexus-p2-plugin-security.xml";
    }
}
