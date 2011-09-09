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
package com.sonatype.s2.project.ui.test;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.authentication.AuthFacade;

import com.sonatype.s2.project.ui.codebase.NewCodebaseProjectOperation;
import com.sonatype.s2.publisher.S2PublishRequest;
import com.sonatype.s2.publisher.S2PublisherConstants;
import com.sonatype.s2.publisher.nexus.NexusCodebasePublisher;

public class NexusS2ProjectPublisherIT
    extends TestCase
{
    private IProgressMonitor monitor = new NullProgressMonitor();

    public void testPublish()
        throws Exception
    {
        NewCodebaseProjectOperation op = new NewCodebaseProjectOperation( "bar", "foo", "bar", "1.2.3", null );
        op.createProject( monitor );

        String serverUrl = "http://localhost:8081/nexus";
        final String username = "admin";
        final String password = "admin123";

        AuthFacade.getAuthService().save( serverUrl, username, password );
        
        NexusCodebasePublisher publisher = new NexusCodebasePublisher();

        S2PublishRequest request = new S2PublishRequest();
        request.setNexusBaseUrl( serverUrl );

        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject( "bar" );

        request.addS2Project( project.getFolder( S2PublisherConstants.PMD_PATH ).getLocation() );

        publisher.publish( request, monitor );
    }
}
