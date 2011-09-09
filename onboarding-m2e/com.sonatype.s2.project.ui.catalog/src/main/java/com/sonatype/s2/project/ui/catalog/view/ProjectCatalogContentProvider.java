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
package com.sonatype.s2.project.ui.catalog.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.io.UnauthorizedException;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IS2ProjectCatalogRegistry;
import com.sonatype.s2.project.core.IS2ProjectCatalogRegistryEntry;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.IS2ProjectCatalogEntry;
import com.sonatype.s2.project.ui.catalog.Messages;
import com.sonatype.s2.project.ui.internal.Activator;

public class ProjectCatalogContentProvider
    implements IStructuredContentProvider, ITreeContentProvider
{
    private static final Logger log = LoggerFactory.getLogger( ProjectCatalogContentProvider.class );

    private final static String[] ERROR = { Messages.catalogContentProvider_errors_errorLoadingCatalog };

    private final static String[] LOADING = { Messages.loading };

    protected TreeViewer viewer;

    protected boolean rootLoadFailed = false;

    protected boolean initialLoadFailuresReported = false;

    private final IS2ProjectCatalogRegistry catalogRegistry;

    protected List<IS2ProjectCatalogRegistryEntry> entries;

    protected Map<String, Object[]> projects;

    public ProjectCatalogContentProvider( IS2ProjectCatalogRegistry catalogRegistry, TreeViewer viewer )
    {
        this.viewer = viewer;
        this.catalogRegistry = catalogRegistry;

        refresh();
        viewer.setContentProvider( this );
    }

    public void refresh()
    {
        entries = null;
        rootLoadFailed = false;
        projects = new HashMap<String, Object[]>();
    }

    public void reset()
    {
        initialLoadFailuresReported = false;
        refresh();
    }

    public Object[] getElements( Object inputElement )
    {
        if ( !( inputElement instanceof IViewSite ) )
        {
            return getChildren( inputElement );
        }

        if ( rootLoadFailed )
        {
            return ERROR;
        }
        else if ( entries == null )
        {
            new Job( Messages.catalogContentProvider_jobs_loadingCatalogs )
            {
                @Override
                public IStatus run( IProgressMonitor monitor )
                {
                    try
                    {
                        entries = catalogRegistry.getCatalogEntries( monitor );

                        if ( !initialLoadFailuresReported )
                        {
                            final List<IStatus> errors = new ArrayList<IStatus>();
                            for ( final IS2ProjectCatalogRegistryEntry entry : entries )
                            {
                                if ( !entry.isLoaded() )
                                {
                                    IStatus status = entry.getStatus();
                                    if ( status.getException() instanceof UnauthorizedException )
                                    {
                                        PlatformUI.getWorkbench().getDisplay().asyncExec( new Runnable()
                                        {
                                            public void run()
                                            {
                                                UrlInputDialog dlg =
                                                    new UrlInputDialog(
                                                                        viewer.getControl().getShell(),
                                                                        Messages.catalogContentProvider_errors_authenticationError,
                                                                        Messages.catalogContentProvider_catalogUrlLabel,
                                                                        entry.getUrl() );
                                                if ( dlg.open() == Window.OK )
                                                {
                                                    catalogRegistry.removeCatalog( entry.getUrl() );
                                                    catalogRegistry.addCatalog( dlg.getUrl() );
                                                }
                                            }
                                        } );
                                    }
                                    else
                                    {
                                        errors.add( entry.getStatus() );
                                    }
                                }
                            }
                            if ( errors.size() > 0 )
                            {
                                return new MultiStatus( Activator.PLUGIN_ID, IStatus.ERROR,
                                                        errors.toArray( new IStatus[errors.size()] ),
                                                        Messages.catalogContentProvider_errors_errorLoadingCatalog,
                                                        null );
                            }
                        }

                        return Status.OK_STATUS;
                    }
                    catch ( CoreException e )
                    {
                        log.error( Messages.catalogContentProvider_errors_errorLoadingRegistry, e );
                        rootLoadFailed = true;
                        return e.getStatus();
                    }
                    finally
                    {
                        initialLoadFailuresReported = true;
                        PlatformUI.getWorkbench().getDisplay().syncExec( new Runnable()
                        {
                            public void run()
                            {
                                viewer.refresh();
                                loadCallback( entries == null ? 0 : entries.size() );
                            }
                        } );
                    }
                }
            }.schedule();
            return LOADING;
        }
        else
        {
            return entries.toArray();
        }
    }

    public void dispose()
    {
    }

    public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
    {
    }

    public Object[] getChildren( final Object parentElement )
    {
        if ( parentElement instanceof IS2ProjectCatalogRegistryEntry )
        {
            IS2ProjectCatalogRegistryEntry entry = (IS2ProjectCatalogRegistryEntry) parentElement;
            return entry.isLoaded() ? entry.getCatalog().getEntries().toArray()
                            : new Object[] { entry.getStatus().getMessage() };
        }
        else if ( parentElement instanceof IS2ProjectCatalogEntry )
        {
            try
            {
                final String url = catalogRegistry.getEffectiveDescriptorUrl( (IS2ProjectCatalogEntry) parentElement );
                Object[] modules = projects.get( url );
                if ( modules == null )
                {
                    new Job( NLS.bind( Messages.catalogContentProvider_jobs_loadingProjectFrom, url ) )
                    {
                        @Override
                        public IStatus run( IProgressMonitor monitor )
                        {
                            try
                            {
                                IS2Project project = S2ProjectCore.getInstance().loadProject( url, monitor );
                                projects.put( url, project.getModules().toArray() );
                                return Status.OK_STATUS;
                            }
                            catch ( CoreException e )
                            {
                                projects.put( url, new Object[] {} );
                                log.error( Messages.catalogContentProvider_errors_errorLoadingProject, e );
                                return e.getStatus();
                            }
                            finally
                            {
                                PlatformUI.getWorkbench().getDisplay().syncExec( new Runnable()
                                {
                                    public void run()
                                    {
                                        viewer.refresh( parentElement );
                                    }
                                } );
                            }
                        }
                    }.schedule();
                    return LOADING;
                }
                else
                {
                    return modules;
                }
            }
            catch ( CoreException e )
            {
                StatusManager.getManager().handle( e, Activator.PLUGIN_ID );
            }
        }
        return new Object[0];
    }

    public Object getParent( Object element )
    {
        return null;
    }

    public boolean hasChildren( Object element )
    {
        return element instanceof IS2ProjectCatalogRegistryEntry;// || element instanceof IS2ProjectCatalogEntry;
    }

    protected void loadCallback( int size )
    {
    }
}
