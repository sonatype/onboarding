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
package com.sonatype.s2.project.ui.catalog.feeds;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.sonatype.s2.project.ui.internal.Images;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

public class FeedLabelProvider
    extends LabelProvider
{

    public String getText( Object obj )
    {
        if ( obj instanceof SyndFeed )
        {
            SyndFeed feed = (SyndFeed) obj;
            String s = feed.getTitle();
            return s == null || s.length() == 0 ? feed.getLink() : s;
        }
        else if ( obj instanceof SyndEntry )
        {
            return ( (SyndEntry) obj ).getTitle().replaceAll( "\\<.*?>", "" );
        }
        return String.valueOf( obj );
    }

    public Image getImage( Object obj )
    {
        if ( obj instanceof SyndFeed )
        {
            return Images.FEED;
        }
        return super.getImage( obj );
    }

}
