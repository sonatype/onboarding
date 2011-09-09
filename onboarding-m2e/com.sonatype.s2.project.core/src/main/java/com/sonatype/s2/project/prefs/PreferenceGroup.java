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
package com.sonatype.s2.project.prefs;

/**
 * Describes sets of preferences that can be exported/imported
 */
public enum PreferenceGroup
{

    /**
     * Preferences for network proxies.
     */
    PROXY,

    /**
     * Preferences for Java development like compiler settings and formatter options.
     */
    JDT,

    /**
     * Preferences for M2Eclipse and S2.
     */
    M2E,

}
