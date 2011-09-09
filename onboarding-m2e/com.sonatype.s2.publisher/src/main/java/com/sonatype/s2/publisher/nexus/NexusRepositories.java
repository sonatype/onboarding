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
package com.sonatype.s2.publisher.nexus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.io.HttpBaseSupport.HttpInputStream;
import org.maven.ide.eclipse.io.HttpFetcher;
import org.maven.ide.eclipse.io.S2IOFacade;

import com.sonatype.s2.publisher.Activator;
import com.sonatype.s2.publisher.internal.Messages;

public class NexusRepositories
{
    public static Collection<String> getP2Repositories(NexusLineupPublishingInfo info, Collection<String> repos, IProgressMonitor monitor) {
        String nexusUrl = info.getServerUrl();
        if (nexusUrl == null)
            return repos;

        StringBuilder builder = new StringBuilder(nexusUrl);
        if (!nexusUrl.endsWith( "/" ))
            builder.append( '/' );

        builder.append("service/local/all_repositories");
        try
        {        
            HttpFetcher fetcher = new HttpFetcher();
            HttpInputStream is = fetcher.openStream( new URI( builder.toString() ), monitor, AuthFacade.getAuthService(),
                                                     S2IOFacade.getProxyService() );
            // Xpp3DomBuilder.build closes the "is" stream
            Xpp3Dom dom = Xpp3DomBuilder.build( is, is.getEncoding() );
            Xpp3Dom child = dom.getChild( "data" );
            if (child == null)
                return repos;

            Xpp3Dom[] repositories =  child.getChildren("repositories-item");
            if (repositories == null)
                return repos;

            for (Xpp3Dom repository : repositories) {
                child = repository.getChild( "format" );
                if (child == null || !child.getValue().equals( "p2" ))
                    continue;
                child = repository.getChild("contentResourceURI");
                if (child != null)
                    repos.add(child.getValue());
            }
        }
        catch ( XmlPullParserException e )
        {
            log(Messages.errorParsing, e);
        }
        catch ( IOException e )
        {
            log(Messages.errorContactingNexus, e);
        }
        catch ( URISyntaxException e )
        {
            // This shouldn't happen
            log(Messages.errorInvalidNexusUrl, e);
        }

        return repos;
    }

    private static void log(String message, Exception e) {
        IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, e);
        StatusManager.getManager().handle( status, StatusManager.LOG );
    }
}
