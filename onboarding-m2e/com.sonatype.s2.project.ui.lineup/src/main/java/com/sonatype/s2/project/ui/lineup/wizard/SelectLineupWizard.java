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
package com.sonatype.s2.project.ui.lineup.wizard;

import org.eclipse.jface.wizard.Wizard;

public class SelectLineupWizard
    extends Wizard
{
    private SelectLineupPage page;

    private String serverUrl;

	private String previousValue;

    public SelectLineupWizard( String serverUrl, String previousValue )
    {
        this.serverUrl = serverUrl;
        setNeedsProgressMonitor( true );
        this.previousValue = previousValue;
    }

	@Override
    public boolean performFinish()
    {
        return true;
    }

    @Override
    public void addPages()
    {
        addPage( page = new SelectLineupPage( serverUrl, previousValue ) );
        setWindowTitle( page.getTitle() );
    }

    public String getLineupUrl()
    {
        return page.getLineupUrl();
    }
}
