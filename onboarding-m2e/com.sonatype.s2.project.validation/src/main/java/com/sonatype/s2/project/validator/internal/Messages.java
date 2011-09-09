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
package com.sonatype.s2.project.validator.internal;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends NLS
{
    private static final String BUNDLE = Messages.class.getName().toLowerCase();

    static
    {
        NLS.initializeMessages( BUNDLE, Messages.class );
    }

    public static String error_accessValidatorInterfaceRequired;

    public static String error_projectValidatorInterfaceRequired;

    public static String progress_validatingAccess;

    public static String progress_validatingProject;
    
    public static String resource_credentials;

    public static String warning_undefinedCredentials;
    
    public static String targetEnvValidator_format;    
}
