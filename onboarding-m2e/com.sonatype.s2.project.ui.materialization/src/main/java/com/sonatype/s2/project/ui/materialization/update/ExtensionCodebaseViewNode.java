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
package com.sonatype.s2.project.ui.materialization.update;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.graphics.Image;

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.ui.materialization.update.spi.CodebaseViewNodeProvider;

public class ExtensionCodebaseViewNode
    extends CodebaseViewNode
{

    private CodebaseViewNodeProvider provider;

    public ExtensionCodebaseViewNode( IWorkspaceCodebase workspaceCodebase, CodebaseViewNodeProvider provider )
    {
        super( workspaceCodebase, provider.getName(), provider.getURL(), provider.getImage() );
        this.provider = provider;
    }

    @Override
    public String getStatus()
    {
        return provider.getStatus();
    }

    @Override
	public Image getImage(boolean showStatus) {
    	//TODO
		return provider.getImage();
	}

	@Override
    public boolean isUpdateAvailable()
    {
        return provider.isUpdateAvailable();
    }

    @Override
    public void update()
    {
        //hudson jobs rely on this happening here. done this way to minimize exported packages exposure..
        AbstractCodebaseUpdateJob job = new AbstractCodebaseUpdateJob(getWorkspaceCodebase(), "Update codebase")
        {
            
            @Override
            protected IStatus doRun( IProgressMonitor monitor )
                throws CoreException, InterruptedException
            {
                return provider.update( monitor);
            }
        };
        job.schedule();
    }

}
