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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class FeedContentProvider
    implements IStructuredContentProvider, ITreeContentProvider
{
    private static final Logger log = LoggerFactory.getLogger( FeedContentProvider.class );

    private final static String[] LOADING = { "Loading..." };

    protected TreeViewer viewer;

    public FeedContentProvider( TreeViewer viewer )
    {
        this.viewer = viewer;

        viewer.setContentProvider( this );
    }

    public Object[] getElements( Object inputElement )
    {
        return getChildren( inputElement );
    }

    public void dispose()
    {
    }

    public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
    {
    }

    @SuppressWarnings( "unchecked" )
    public Object[] getChildren( final Object parentElement )
    {
        if ( parentElement instanceof List )
        {
            final List list = (List) parentElement;
            if ( list.size() > 0 )
            {
                if ( list.get( 0 ) instanceof String )
                {
                    final List<SyndFeed> feeds = new ArrayList<SyndFeed>();
                    new Job( "Loading News Feeds" )
                    {
                        @Override
                        public IStatus run( IProgressMonitor monitor )
                        {
                            SyndFeedInput input = new SyndFeedInput();
                            for ( Object url : list )
                            {
                                SyndFeed feed;
                                try
                                {
                                    feed = input.build( new XmlReader( new URL( String.valueOf( url ) ) ) );
                                    feeds.add( feed );
                                }
                                catch ( IllegalArgumentException e )
                                {
                                    log.error( "Error loading feed", e );
                                }
                                catch ( MalformedURLException e )
                                {
                                    log.error( "Invalid feed URL", e );
                                }
                                catch ( FeedException e )
                                {
                                    log.error( "Error loading feed", e );
                                }
                                catch ( IOException e )
                                {
                                    log.error( "Error loading feed", e );
                                }
                            }
                            if ( !viewer.getControl().isDisposed() )
                            {
                                viewer.getControl().getDisplay().asyncExec( new Runnable()
                                {
                                    public void run()
                                    {
                                        viewer.setInput( feeds );
                                    }
                                } );
                            }
                            return Status.OK_STATUS;
                        }
                    }.schedule();
                    return LOADING;
                }
                else
                {
                    return list.toArray();
                }
            }
        }
        else if ( parentElement instanceof SyndFeed )
        {
            return ( (SyndFeed) parentElement ).getEntries().toArray();
        }
        return new Object[0];
    }

    public Object getParent( Object element )
    {
        return null;
    }

    public boolean hasChildren( Object element )
    {
        return element instanceof SyndFeed;
    }
}
