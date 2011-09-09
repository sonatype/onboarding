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
package com.sonatype.s2.project.model;

import java.util.List;

public interface IS2Module
{
    /** Module name, used to name workspace projects */
    public String getName();

    public void setName( String name );

    /** homepage URL */
    public String getHomeUrl();

    public void setHomeUrl( String url );

    /** project documentation (e.g. wiki) URL */
    public String getDocsUrl();

    public void setDocsUrl( String url );

    /** SCM location URL */
    public IScmLocation getScmLocation();

    public void setScmLocation( IScmLocation scmLocation );

    /** issue tracking system URL */
    public String getIssuesUrl();

    public void setIssuesUrl( String url );

    /** CI system URL */
    public String getBuildUrl();

    public void setBuildUrl( String url );
    
    public List<ICIServerLocation> getCiServers();

    public List<String> getProfiles();

    public void addProfile( String profile );

    public void removeProfile( String profile );

    /**
     * Paths of module source roots relative to scmUrl.
     */
    public List<String> getRoots();

    public void addRoot( String root );

    public void removeRoot( String root );

    /** List of feeds */
    public List<String> getFeeds();

    public void addFeed( String feed );

    public void removeFeed( String feed );
}
