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
package com.sonatype.s2.publisher;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class S2PublishRequest
{
    private static final Logger log = LoggerFactory.getLogger( S2PublishRequest.class );

    /**
     * The URI of the s2 project catalog where the s2 project will be published.
     */
    private String baseUrl;

    /**
     * The list of s2 projects to be published.
     */
    private Set<IPath> s2Projects = new LinkedHashSet<IPath>();

    public String getNexusBaseUrl()
    {
        return baseUrl;
    }

    public void setNexusBaseUrl( String baseUrl )
    {
        this.baseUrl = baseUrl;
    }

    public Set<IPath> getS2Projects()
    {
        return s2Projects;
    }

    /**
     * @param s2Project absolute path in the local file system
     */
    public void addS2Project( IPath s2Project )
    {
        s2Projects.add( s2Project.makeAbsolute() );
    }

    public void validate()
        throws S2PublisherException
    {
        if ( s2Projects.size() == 0 )
        {
            String message = "s2Projects cannot be empty";
            log.error( message );
            throw new S2PublisherException( message );
        }

        if ( baseUrl == null )
        {
            String message = "s2CatalogRepositoryURL cannot be null";
            log.error( message );
            throw new S2PublisherException( message );
        }
    }
}
