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
package com.sonatype.nexus.p2;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.proxy.registry.AbstractIdContentClass;
import org.sonatype.nexus.proxy.registry.ContentClass;

@Component( role = ContentClass.class, hint = P2ContentClass.ID )
public class P2ContentClass
    extends AbstractIdContentClass
{

    public static final String ID = "p2";
    public static final String NAME = "Eclipse P2";

    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return NAME;
    }
}
