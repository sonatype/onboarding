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
package com.sonatype.s2.project.validation.api;

import java.util.Properties;

public class S2ProjectValidationContext
{
    public static String FRESH_INSTALL_PROPNAME = "FreshInstall";

    private Properties properties = new Properties();

    public String getProperty( String key )
    {
        return properties.getProperty( key );
    }

    public void setProperty( String key, String value )
    {
        properties.setProperty( key, value );
    }
}
