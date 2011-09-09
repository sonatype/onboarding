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
package com.sonatype.s2.project.ui.codebase.wizard;

import org.eclipse.jface.wizard.Wizard;

public class SelectMavenSettingsWizard
    extends Wizard
{
    private SelectMavenSettingsPage page;

    private String serverUrl;

	private String initialSelection;

    public SelectMavenSettingsWizard( String serverUrl, String currentTemplate )
    {
        this.serverUrl = serverUrl;
        this.initialSelection = currentTemplate;
        setNeedsProgressMonitor( true );
    }

    @Override
    public boolean performFinish()
    {
        return true;
    }

    @Override
    public void addPages()
    {
        addPage( page = new SelectMavenSettingsPage( serverUrl, initialSelection ) );
        setWindowTitle( page.getTitle() );
    }

    public String getMavenSettingsUrl()
    {
        return page.getUrl();
    }
}
