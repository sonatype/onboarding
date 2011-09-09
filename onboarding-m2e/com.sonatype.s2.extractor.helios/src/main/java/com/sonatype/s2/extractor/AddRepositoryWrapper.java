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
package com.sonatype.s2.extractor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

import com.sonatype.s2.internal.extractor.Activator;
import com.sonatype.s2.internal.extractor.Messages;

public class AddRepositoryWrapper
{
    /*
     * Adds the given repository addresses to the manager
     * @return addresses previously unknown
     */
    public Collection<URI> addRepositories( Collection<String> urls )
    {
        Collection<URI> newRepos = new ArrayList<URI>();
        RepositoryTracker tracker = getRepositoryTracker();
        ProvisioningSession session = getSession();

        for ( String location : urls )
        {
            try
            {
                URI repo = URIUtil.fromString( location );
                IStatus valid = tracker.validateRepositoryLocation( session, repo, false, new NullProgressMonitor() );
                if ( valid.isOK() )
                {
                    tracker.addRepository( repo, null, session );
                    newRepos.add( repo );
                }
            }
            catch ( URISyntaxException e )
            {
                error( NLS.bind( Messages.errorInvalidUrl, location ), e );
            }
        }
        return newRepos;
    }

    public final void addRepositoryToCurrentIDE( String location, String name )
    {
        try
        {
            URI repo = URIUtil.fromString( location );
            RepositoryTracker tracker = getRepositoryTracker();

            IStatus valid = tracker.validateRepositoryLocation( getSession(), repo, false, new NullProgressMonitor() );

            if ( valid.isOK() )
                tracker.addRepository( repo, name, getSession() );
            else
                StatusManager.getManager().handle( valid );
        }
        catch ( URISyntaxException e )
        {
            error( NLS.bind( Messages.errorInvalidUrl, location ), e );
        }
    }

    /**
     * Return a RepositoryTracker appropriate for validating and adding the repository. The default tracker is described
     * by the ProvisioningUI.
     * 
     * @return the repository tracker
     */
    private RepositoryTracker getRepositoryTracker()
    {
        return ProvisioningUI.getDefaultUI().getRepositoryTracker();
    }

    private ProvisioningSession getSession()
    {
        return ProvisioningUI.getDefaultUI().getSession();
    }

    private void error( String message, Exception e )
    {
        StatusManager.getManager().handle( new Status( IStatus.ERROR, Activator.PLUGIN_ID, message, e ) );
    }
}
