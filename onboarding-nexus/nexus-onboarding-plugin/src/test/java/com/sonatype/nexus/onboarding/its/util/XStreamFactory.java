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
package com.sonatype.nexus.onboarding.its.util;

import com.sonatype.nexus.onboarding.rest.dto.xstream.XStreamUtil;
import com.thoughtworks.xstream.XStream;

public class XStreamFactory
{
    public static XStream getXmlXStream()
    {
        XStream xs = org.sonatype.nexus.test.utils.XStreamFactory.getXmlXStream();
        configureXStream( xs );
        return xs;
    }

    public static XStream getJsonXStream()
    {
        XStream xs = org.sonatype.nexus.test.utils.XStreamFactory.getJsonXStream();
        configureXStream( xs );
        return xs;
    }

    private static void configureXStream( XStream xstream )
    {
        XStreamUtil.initializeXStream( xstream );
    }

}
