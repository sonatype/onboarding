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
package com.sonatype.s2.project.ui.internal.composites;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputComposite;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputDialog;

public class NexusUrlDialog
    extends UrlInputDialog
{
    private NexusUrlComposite nexusUrlComposite;

    public NexusUrlDialog( Shell parentShell, String title, String nexusServerUrl, int inputStyle )
    {
        super( parentShell, title, "Nexus Server URL:", nexusServerUrl, inputStyle );
    }

    @Override
    protected UrlInputComposite createUrlInputComposite( Composite parent, SwtValidationGroup validation, int inputStyle )
    {
        nexusUrlComposite = new NexusUrlComposite( parent, null, validation, getUrl(), inputStyle );
        return nexusUrlComposite;
    }

    public IStatus checkNexus( IProgressMonitor monitor )
    {
        IStatus status = nexusUrlComposite.checkNexus( monitor );

        if ( status.isOK() )
        {
            try
            {
                nexusUrlComposite.saveNexusUrl( monitor );
            }
            catch ( CoreException e )
            {
                status = e.getStatus();
            }
        }

        return status;
    }
}
