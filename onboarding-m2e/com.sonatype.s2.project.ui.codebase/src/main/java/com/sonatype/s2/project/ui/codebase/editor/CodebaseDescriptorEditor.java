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
package com.sonatype.s2.project.ui.codebase.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.ui.common.authentication.RealmUrlCollector;

import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.codebase.Activator;
import com.sonatype.s2.project.ui.codebase.Messages;

public class CodebaseDescriptorEditor
    extends AbstractCodebaseEditor
{
    private static final String EXTENSION_ID =
        "com.sonatype.s2.project.ui.codebase.editor.CodebaseDescriptorEditorAction"; //$NON-NLS-1$

    private static final int FIRST_MODULE_PAGE_OFFSET = 1;

    private CodebaseDetailsPage detailsPage;

    private List<SourceTreePage> sourceTreePages = new ArrayList<SourceTreePage>();

    @Override
    protected void addPages()
    {
        try
        {
            addPage( detailsPage = new CodebaseDetailsPage( this ) );
            if ( getProject() != null )
            {
                createModulePages();
            }
        }
        catch ( PartInitException e )
        {
            StatusManager.getManager().handle( e, Activator.PLUGIN_ID );
        }
    }

    @Override
    public void doSave( IProgressMonitor monitor )
    {
        final RealmUrlCollector realmUrlCollector = new RealmUrlCollector();
        detailsPage.saveRealms( realmUrlCollector );
        for ( SourceTreePage page : sourceTreePages )
        {
            page.saveRealms( realmUrlCollector );
        }

        try
        {
            setBusy( true );
            getCodebaseEditorInput().doSave( monitor );

            new Job( Messages.codebaseDescriptorEditor_job_savingRealmUrls )
            {
                @Override
                protected IStatus run( IProgressMonitor monitor )
                {
                    final boolean realmChanges = !realmUrlCollector.isEmpty();
                    if ( realmChanges )
                    {
                        realmUrlCollector.save( monitor );
                    }
                    Display.getDefault().asyncExec( new Runnable()
                    {
                        public void run()
                        {
                            clearDirty();
                            detailsPage.updateTitle();
                            if ( realmChanges )
                            {
                                updateAllCodebaseEditors();
                            }
                            else
                            {
                                updatePages();
                            }
                        }
                    } );
                    return Status.OK_STATUS;
                }
            }.schedule();

            setBusy( false );
        }
        catch ( CoreException e )
        {
            StatusManager.getManager().handle( e.getStatus(), StatusManager.BLOCK | StatusManager.LOG );
        }
        monitor.done();
    }

    @Override
    public void doSaveAs()
    {
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return false;
    }

    @Override
    public void setProject( IS2Project project )
    {
        super.setProject( project );

        createModulePages();
        updatePages();
    }

    private void createModulePages()
    {
        IS2Project project = getProject();
        if ( project == null || detailsPage == null )
        {
            return;
        }

        for ( int i = getPageCount() - 1; i >= 1; i-- )
        {
            removePage( i );
        }
        sourceTreePages.clear();

        for ( IS2Module module : project.getModules() )
        {
            try
            {
                SourceTreePage modulePage = new SourceTreePage( this, module );
                addPage( modulePage );
                sourceTreePages.add( modulePage );
            }
            catch ( PartInitException e )
            {
                StatusManager.getManager().handle( e, Activator.PLUGIN_ID );
            }
        }
    }

    @Override
    protected void updatePages()
    {
        IS2Project project = getProject();
        if ( project == null || detailsPage == null )
        {
            return;
        }

        super.updatePages();
    }

    void updateModule( IS2Module module )
    {
        detailsPage.updatePage();

        int n = getProject().getModules().indexOf( module );
        setTabText( FIRST_MODULE_PAGE_OFFSET + n, module.getName() );
    }

    void showModule( int n )
    {
        setActivePage( FIRST_MODULE_PAGE_OFFSET + n );
    }

    void addModule( IS2Module module )
    {
        try
        {
            SourceTreePage modulePage = new SourceTreePage( this, module );
            int n = addPage( modulePage );
            getProject().addModule( module );
            sourceTreePages.add( modulePage );
            detailsPage.updatePage();
            setActivePage( n );
        }
        catch ( PartInitException e )
        {
            StatusManager.getManager().handle( e, Activator.PLUGIN_ID );
        }
    }

    void deleteModule( IS2Module module )
    {
        List<IS2Module> modules = getProject().getModules();
        int n = modules.indexOf( module );
        modules.remove( n );
        sourceTreePages.remove( n );
        removePage( FIRST_MODULE_PAGE_OFFSET + n );
        detailsPage.updatePage();
    }

    @Override
    public void init( IEditorSite site, IEditorInput input )
        throws PartInitException
    {
        super.init( site, input );

        loadActionExtensions( EXTENSION_ID, Activator.PLUGIN_ID );
    }

    public static IStatus openEditor( IEditorInput input )
    {
        return openEditor( input, CodebaseDescriptorEditor.class.getName() );
    }

    public static void updateAllCodebaseEditors()
    {
        updateAllEditors( CodebaseDescriptorEditor.class.getName() );
    }
}
