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
package com.sonatype.s2.project.ui.codebase;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.statushandlers.StatusManager;

public class LinkUtil
{
    public static Hyperlink createHyperlink( final Composite parent, FormToolkit toolkit, final Object url,
                                             String linkTitle, final Action[] actions )
    {
        Hyperlink link = toolkit.createHyperlink( parent, linkTitle, SWT.NONE );
        configureLayoutData( link );
        createMenu( link, parent, url, actions );

        return link;
    }

    public static Hyperlink createHyperlink( final Composite parent, final String url )
    {
        return createHyperlink( parent, url, url, null );
    }

    public static Hyperlink createHyperlink( final Composite parent, final Object url, String linkTitle,
                                             final Action[] actions )
    {
        Hyperlink link = new Hyperlink( parent, SWT.NONE );
        link.setText( linkTitle );
        configureLayoutData( link );
        createMenu( link, parent, url, actions );

        return link;
    }

    private static void configureLayoutData( Hyperlink link )
    {
        GridData gd = new GridData( SWT.FILL, SWT.CENTER, true, false );
        gd.minimumWidth = 1;
        gd.widthHint = 100;
        link.setLayoutData( gd );
    }

    private static void createMenu( Hyperlink link, final Composite parent, final Object url, final Action[] actions )
    {
        MenuManager menuManager = new MenuManager();
        menuManager.setRemoveAllWhenShown( true );
        menuManager.addMenuListener( new IMenuListener()
        {
            public void menuAboutToShow( IMenuManager manager )
            {
                if ( actions != null )
                {
                    for ( Action action : actions )
                    {
                        manager.add( action );
                    }
                }

                IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
                if ( support.isInternalWebBrowserAvailable() )
                {
                    manager.add( new Action( Messages.linkUtil_actions_open )
                    {
                        public void run()
                        {
                            openLink( url, true );
                        }
                    } );
                }
                manager.add( new Action( Messages.linkUtil_actions_openExternal )
                {
                    public void run()
                    {
                        openLink( url, false );
                    }
                } );
                manager.add( new Separator() );
                manager.add( new Action( Messages.linkUtil_actions_copy )
                {
                    public void run()
                    {
                        Clipboard clipboard = new Clipboard( parent.getShell().getDisplay() );
                        clipboard.setContents( new Object[] { url }, new TextTransfer[] { TextTransfer.getInstance() } );
                    }
                } );
            }
        } );
        ;
        link.setMenu( menuManager.createContextMenu( link ) );

        link.addHyperlinkListener( new HyperlinkAdapter()
        {
            @Override
            public void linkActivated( HyperlinkEvent e )
            {
                if ( actions != null && actions.length > 0 )
                {
                    actions[0].run();
                }
                else
                {
                    openLink( url, ( e.getStateMask() & SWT.CTRL ) == 0 );
                }
            }
        } );
    }

    public static Hyperlink createHyperlinkEntry( Composite parent, FormToolkit toolkit, String label, final String url )
    {
        return createHyperlinkEntry( parent, toolkit, label, url, null );
    }

    public static Hyperlink createHyperlinkEntry( Composite parent, FormToolkit toolkit, String label,
                                                  final String url, Action[] actions )
    {
        return createHyperlinkEntry( parent, toolkit, label, url, url, actions );
    }

    public static Hyperlink createHyperlinkEntry( Composite parent, FormToolkit toolkit, String label,
                                                  final String url, String linkTitle, Action[] actions )
    {
        if ( url == null || url.length() == 0 )
        {
            return null;
        }

        toolkit.createLabel( parent, label );

        return createHyperlink( parent, toolkit, url, linkTitle, actions );
    }

    /** Opens a link in a web browser */
    public static void openLink( Object urlObject, boolean internal )
    {
        String url =
            urlObject instanceof IUrlProvider ? ( (IUrlProvider) urlObject ).getUrl() : String.valueOf( urlObject );
        if ( url == null || url.length() == 0 )
        {
            return;
        }

        try
        {
            IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
            IWebBrowser browser =
                internal && support.isInternalWebBrowserAvailable() ? support.createBrowser( IWorkbenchBrowserSupport.AS_EDITOR
                                                                                                 | IWorkbenchBrowserSupport.LOCATION_BAR
                                                                                                 | IWorkbenchBrowserSupport.STATUS,
                                                                                             null, url, url )
                                : support.getExternalBrowser();
            browser.openURL( new URL( url ) );
        }
        catch ( PartInitException partInitException )
        {
            StatusManager.getManager().handle( partInitException, Activator.PLUGIN_ID );
        }
        catch ( MalformedURLException malformedURLException )
        {
            StatusManager.getManager().handle( new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                                                           Messages.errors_invalidUrl, malformedURLException ) );
        }
    }

    /**
     * Use this if the link is constructed at runtime. The method has to run in the display thread.
     */
    public interface IUrlProvider
    {
        public String getUrl();
    }
}
