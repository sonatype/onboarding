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
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputComposite;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;

import com.sonatype.s2.nexus.NexusFacade;
import com.sonatype.s2.project.ui.internal.Messages;

/**
 * this composite handles entering of the "main" nexus server url that will be used for security realm queries. 
 *
 */
public class NexusUrlComposite
    extends UrlInputComposite
{
	/**
	 * 
	 * @param parent
	 * @param widthGroup
	 * @param validationGroup
	 * @param nexusUrl the root url of a nexus server
	 * @param style
	 */
    public NexusUrlComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                              String nexusUrl, int style )
    {
        super( parent, widthGroup, validationGroup, style );
        setUrlLabelText( Messages.nexusUrlComposite_url_label );
        if ( nexusUrl == null )
        {
            nexusUrl = NexusFacade.getMainNexusServerURL();
        }
        setUrl( nexusUrl );
    }

    public IStatus checkNexus( IProgressMonitor monitor )
    {
        String url = getUrl();
        IAuthData authData = AuthFacade.getAuthService().select( url );

        String username = "";
        String password = "";
        AnonymousAccessType anonymousAccessType = AnonymousAccessType.ALLOWED;
        if ( authData != null )
        {
            username = authData.getUsername();
            password = authData.getPassword();
            anonymousAccessType = authData.getAnonymousAccessType();
        }
        return NexusFacade.validateCredentials( url, username, password, anonymousAccessType, monitor );
    }

    public void saveNexusUrl( IProgressMonitor monitor )
        throws CoreException
    {
        String url = getUrl();
        IAuthData authData = AuthFacade.getAuthService().select( url );

        NexusFacade.setMainNexusServerData( url, authData.getUsername(), authData.getPassword(), monitor );
    }
}
