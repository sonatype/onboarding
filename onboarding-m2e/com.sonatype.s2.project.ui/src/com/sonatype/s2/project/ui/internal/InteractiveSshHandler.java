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
package com.sonatype.s2.project.ui.internal;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.sonatype.s2.ssh.SshHandler;

class InteractiveSshHandler
    implements SshHandler
{

    public int getPriority()
    {
        return 10;
    }

    public boolean promptYesNo( final String message )
    {
        final boolean result[] = new boolean[1];
        getDisplay().syncExec( new Runnable()
        {
            public void run()
            {
                result[0] = MessageDialog.openQuestion( null, Messages.sshHandler_title, message );

            }
        } );
        return result[0];
    }

    public void showMessage( final String message )
    {
        getDisplay().syncExec( new Runnable()
        {
            public void run()
            {
                MessageDialog.openInformation( null, Messages.sshHandler_title, message );
            }
        } );
    }

    private Display getDisplay()
    {
        return PlatformUI.isWorkbenchRunning() ? PlatformUI.getWorkbench().getDisplay() : Display.getDefault();
    }

}
