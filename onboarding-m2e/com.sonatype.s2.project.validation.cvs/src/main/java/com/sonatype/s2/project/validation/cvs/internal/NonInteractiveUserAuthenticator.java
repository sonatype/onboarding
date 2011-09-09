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
package com.sonatype.s2.project.validation.cvs.internal;

import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.IUserAuthenticator;
import org.eclipse.team.internal.ccvs.core.IUserInfo;

@SuppressWarnings( "restriction" )
public class NonInteractiveUserAuthenticator
    implements IUserAuthenticator
{

    public void promptForUserInfo( ICVSRepositoryLocation location, IUserInfo userInfo, String message )
        throws CVSException
    {
        throw new CVSException( new CVSStatus( IStatus.ERROR, CVSStatus.AUTHENTICATION_FAILURE, message, location ) );
    }

    public String[] promptForKeyboradInteractive( ICVSRepositoryLocation location, String destination, String name,
                                                  String instruction, String[] prompt, boolean[] echo )
        throws CVSException
    {
        throw new CVSException( new CVSStatus( IStatus.ERROR, CVSStatus.AUTHENTICATION_FAILURE, instruction, location ) );
    }

    public int prompt( ICVSRepositoryLocation location, int promptType, String title, String message,
                       int[] promptResponses, int defaultResponseIndex )
    {
        return defaultResponseIndex;
    }

    public boolean promptForHostKeyChange( ICVSRepositoryLocation location )
    {
        return false;
    }

    public Map promptToConfigureRepositoryLocations( Map alternativeMap )
    {
        return alternativeMap;
    }

}
