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
package com.sonatype.s2.project.validation.api;

import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.internal.AuthData;


public class ScmAccessData
    implements IScmAccessData
{
    private String repositoryUrl;

    private String branch;

    private String revision;

    private IAuthData authData;

    public ScmAccessData( String repositoryUrl, String branch, String revision, IAuthData authData )
    {
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.revision = revision;
        if ( authData == null )
        {
            authData = new AuthData( AuthenticationType.USERNAME_PASSWORD );
        }
        this.authData = authData;
    }

    public ScmAccessData( String repositoryUrl, String branch, String revision, String username, String password )
    {
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.revision = revision;
        authData = new AuthData( username, password, null /* anonymousAccessType */);
    }

    public String getRepositoryUrl()
    {
        return repositoryUrl;
    }

    public String getBranch()
    {
        return branch;
    }

    public String getRevision()
    {
        return revision;
    }

    public String getUsername()
    {
        if ( authData == null )
        {
            return null;
        }
        return authData.getUsername();
    }

    public String getPassword()
    {
        if ( authData == null )
        {
            return null;
        }
        return authData.getPassword();
    }

    @Override
    public String toString()
    {
        return getRepositoryUrl();
    }

    public IAuthData getAuthData()
    {
        return authData;
    }
    //
    // public void setAuthData( IAuthData authData )
    // {
    // this.authData = authData;
    // }
}
