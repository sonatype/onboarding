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
package com.sonatype.m2e.preferences.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;

public class MavenPreferencesInitializer
    extends AbstractPreferenceInitializer
{
    @Override
    public void initializeDefaultPreferences()
    {
        IPreferenceStore store = M2EUIPluginActivator.getDefault().getPreferenceStore();

        store.setDefault( MavenPreferenceConstants.P_DISABLE_JDK_CHECK, true );
    }
}
