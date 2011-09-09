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
package com.sonatype.m2e.egit.internal;

import java.io.File;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.m2e.scm.MavenProjectScmInfo;
import org.maven.ide.eclipse.authentication.internal.AuthData;

import com.sonatype.s2.project.validation.api.IScmAccessData;
import com.sonatype.s2.project.validation.api.ScmAccessData;
import com.sonatype.s2.project.validation.git.GitUtil;
import com.sonatype.s2.project.validation.git.JSchSecurityContext;

@SuppressWarnings( "restriction" )
public class EgitScmHandler
    extends org.sonatype.m2e.egit.internal.EgitScmHandler
{
    private AuthData toAuthData( MavenProjectScmInfo info )
    {
        AuthData authData = new AuthData();
        authData.setUsernameAndPassword( info.getUsername(), info.getPassword() );
        authData.setSSLCertificate( info.getSSLCertificate(), info.getSSLCertificatePassphrase() );

        return authData;
    }

    private IScmAccessData toScmAccessData( MavenProjectScmInfo info )
    {
        ScmAccessData scmData =
            new ScmAccessData( info.getRepositoryUrl(), info.getBranch(), info.getRevision(), toAuthData( info ) );

        return scmData;
    }

    @Override
    public void checkoutProject( MavenProjectScmInfo info, File location, IProgressMonitor monitor )
        throws CoreException, InterruptedException
    {
        JSchSecurityContext secCtx = JSchSecurityContext.enter( toAuthData( info ) );
        try
        {
            super.checkoutProject( info, location, monitor );
        }
        finally
        {
            secCtx.leave();
        }
    }

    @Override
    protected URIish getUri( MavenProjectScmInfo info )
        throws URISyntaxException
    {
        return GitUtil.getUri( toScmAccessData( info ) );
    }

}
