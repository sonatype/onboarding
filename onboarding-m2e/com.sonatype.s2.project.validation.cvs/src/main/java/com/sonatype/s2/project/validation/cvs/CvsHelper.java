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
package com.sonatype.s2.project.validation.cvs;

import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.IUserAuthenticator;

import com.sonatype.s2.project.validation.cvs.internal.NonInteractiveUserAuthenticator;

@SuppressWarnings( "restriction" )
public class CvsHelper
{
    private static final IUserAuthenticator auth = new NonInteractiveUserAuthenticator();

    public static void setNonInteractiveUserAuthenticator( ICVSRepositoryLocation cvsRepositoryLocation )
    {
        if ( cvsRepositoryLocation.getUserAuthenticator() instanceof NonInteractiveUserAuthenticator )
        {
            return;
        }
        cvsRepositoryLocation.setUserAuthenticator( auth );
    }
}
