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

import static com.sonatype.s2.project.ui.catalog.LinkUtil.openLink;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Section;

import com.sonatype.s2.project.ui.internal.Images;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

public class FeedsComposite
    extends Composite
{
    protected TreeViewer viewer;

    protected FeedContentProvider provider;

    private IAction collapseAllAction;

    private IAction refreshAction;

    private IAction openAction;

    private IAction openExternalAction;

    private IAction copyAction;

    protected List<String> urls;

    public FeedsComposite( Composite parent )
    {
        super( parent, SWT.BORDER );
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        this.setLayout( gl );

        viewer = new TreeViewer( this, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL );
        viewer.getControl().setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );

        provider = new FeedContentProvider( viewer );
        viewer.setLabelProvider( new FeedLabelProvider() );
        viewer.setComparer( new IElementComparer()
        {
            public int hashCode( Object element )
            {
                return element.hashCode();
            }

            // a work around for SyndFeed equals() bug
            public boolean equals( Object a, Object b )
            {
                return a == b;
            }
        } );

        viewer.addDoubleClickListener( new IDoubleClickListener()
        {
            public void doubleClick( DoubleClickEvent event )
            {
                Object selection = ( (IStructuredSelection) event.getSelection() ).getFirstElement();
                if ( selection instanceof SyndFeed )
                {
                    viewer.expandToLevel( selection, 1 );
                }
                else if ( selection instanceof SyndEntry )
                {
                    openEntry( selection, true );
                }
            }
        } );

        createActions();
        createContextMenu();
        populateToolbar( parent );
    }

    public void setFeeds( List<String> urls )
    {
        this.urls = urls;
        viewer.setInput( urls );
    }

    private void createActions()
    {
        collapseAllAction = new Action( "Collapse All" )
        {
            public void run()
            {
                viewer.collapseAll();
            }
        };
        collapseAllAction.setToolTipText( "Collapse all" );
        collapseAllAction.setImageDescriptor( PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                                                                                                              ISharedImages.IMG_ELCL_COLLAPSEALL ) );

        refreshAction = new Action( "Refresh All" )
        {
            public void run()
            {
                viewer.setInput( urls );
            }
        };
        refreshAction.setToolTipText( "Reload the feeds" );
        refreshAction.setImageDescriptor( Images.REFRESH_DESCRIPTOR );

        openAction = new Action( "Open" )
        {
            public void run()
            {
                Object selection = ( (IStructuredSelection) viewer.getSelection() ).getFirstElement();
                if ( selection instanceof SyndFeed )
                {
                    viewer.expandToLevel( selection, 1 );
                }
                else if ( selection instanceof SyndEntry )
                {
                    openEntry( selection, true );
                }
            }
        };

        openExternalAction = new Action( "Open in External Browser" )
        {
            public void run()
            {
                Object selection = ( (IStructuredSelection) viewer.getSelection() ).getFirstElement();
                if ( selection instanceof SyndEntry )
                {
                    openEntry( selection, false );
                }
            }
        };

        copyAction = new Action( "Copy Link" )
        {
            public void run()
            {
                Object selection = ( (IStructuredSelection) viewer.getSelection() ).getFirstElement();
                if ( selection instanceof SyndEntry )
                {
                    String url = ( (SyndEntry) selection ).getLink();
                    Clipboard clipboard = new Clipboard( viewer.getControl().getShell().getDisplay() );
                    clipboard.setContents( new Object[] { url }, new TextTransfer[] { TextTransfer.getInstance() } );
                }
            }
        };
    }

    private void createContextMenu()
    {
        MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown( true );
        menuMgr.addMenuListener( new IMenuListener()
        {
            public void menuAboutToShow( IMenuManager manager )
            {
                populateContextMenu( manager );
            }
        } );

        Menu menu = menuMgr.createContextMenu( viewer.getControl() );
        viewer.getControl().setMenu( menu );
        // viewer.registerContextMenu( menuMgr, viewer );
    }

    protected void populateContextMenu( IMenuManager menuManager )
    {
        Object selection = ( (IStructuredSelection) viewer.getSelection() ).getFirstElement();
        if ( selection instanceof SyndFeed )
        {
            menuManager.add( openAction );
        }
        if ( selection instanceof SyndEntry )
        {
            if ( PlatformUI.getWorkbench().getBrowserSupport().isInternalWebBrowserAvailable() )
            {
                menuManager.add( openAction );
            }
            menuManager.add( openExternalAction );
            menuManager.add( new Separator() );
            menuManager.add( copyAction );
        }
        menuManager.add( new Separator() );
        menuManager.add( collapseAllAction );
        menuManager.add( refreshAction );
        menuManager.add( new Separator( IWorkbenchActionConstants.MB_ADDITIONS ) );
    }

    private void populateToolbar( Composite parent )
    {
        while ( parent != null && !( parent instanceof Section ) )
        {
            parent = parent.getParent();
        }
        if ( parent == null )
        {
            return;
        }

        ToolBarManager toolBarManager = new ToolBarManager( SWT.FLAT );
        toolBarManager.add( collapseAllAction );
        toolBarManager.add( refreshAction );

        Composite toolbarComposite = new Composite( parent, SWT.NONE );
        GridLayout toolbarLayout = new GridLayout( 1, true );
        toolbarLayout.marginHeight = 0;
        toolbarLayout.marginWidth = 0;
        toolbarComposite.setLayout( toolbarLayout );
        toolbarComposite.setBackground( null );

        toolBarManager.createControl( toolbarComposite );
        if ( parent instanceof Section )
        {
            ( (Section) parent ).setTextClient( toolbarComposite );
        }
    }

    protected void openEntry( Object selection, boolean internal )
    {
        SyndEntry entry = (SyndEntry) selection;
        openLink( entry.getLink(), internal );
    }
}
