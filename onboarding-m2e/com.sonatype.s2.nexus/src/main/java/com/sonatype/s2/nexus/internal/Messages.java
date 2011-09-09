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
package com.sonatype.s2.nexus.internal;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "com.sonatype.s2.nexus.internal.messages"; //$NON-NLS-1$

    static
    {
        NLS.initializeMessages( BUNDLE_NAME, Messages.class );
    }

    public static String nexusFacade_cannotConstructUrl;

    public static String nexusFacade_connectionFailed;

    public static String nexusFacade_couldNotValidateCredentials;

    public static String nexusFacade_loginFailed;

    public static String nexusFacade_noNexusThere;

    public static String nexusFacade_noRouteToHost;

    public static String nexusFacade_unresolvedAddress;

    public static String nexusFacade_unexpectedServerResponse;

    public static String nexusFacade_urlCannotBeNull;
}
