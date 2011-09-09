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
package com.sonatype.s2.project.integration.test;

import com.sonatype.s2.ssh.SshHandler;

/**
 * Non-interactive SSH handler for automated testing.
 * 
 * @author Benjamin Bentmann
 */
public class SimpleSshHandler
    implements SshHandler
{

    public void showMessage( String message )
    {
        System.out.println( message );
    }

    public boolean promptYesNo( String message )
    {
        System.out.println( message );
        return true;
    }

    public int getPriority()
    {
        return Integer.MAX_VALUE - 1024;
    }

}
