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
package com.sonatype.s2.project.ui.codebase.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.ui.common.authentication.RealmUrlCollector;

import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.codebase.Activator;
import com.sonatype.s2.project.ui.codebase.Messages;

public class SourceTreeWizard
    extends Wizard
{
    private IS2Project project;

    private IS2Module module;

    private SourceTreeInfoPage sourceTreeInfoPage;

    public SourceTreeWizard( IS2Project project, IS2Module module )
    {
        this.project = project;
        this.module = module;

        setNeedsProgressMonitor( true );
    }

    @Override
    public boolean performFinish()
    {
        final RealmUrlCollector realmUrlCollector = new RealmUrlCollector();
        sourceTreeInfoPage.saveRealms( realmUrlCollector );
        if ( !realmUrlCollector.isEmpty() )
        {
            Throwable t = null;
            try
            {
                getContainer().run( true, true, new IRunnableWithProgress()
                {

                    public void run( IProgressMonitor monitor )
                        throws InvocationTargetException, InterruptedException
                    {
                        realmUrlCollector.save( monitor );
                        monitor.done();
                    }
                } );
            }
            catch ( InvocationTargetException e )
            {
                t = e.getTargetException();
            }
            catch ( InterruptedException e )
            {
                t = e;
            }
            if ( t != null )
            {
                String message = NLS.bind( Messages.sourceTreeWizard_0, t.getMessage() );
                sourceTreeInfoPage.setMessage( message, IMessageProvider.ERROR );
                StatusManager.getManager().handle( new Status( IStatus.ERROR, Activator.PLUGIN_ID, message, t ) );
                return false;
            }
        }
        return true;
    }

    @Override
    public void addPages()
    {
        sourceTreeInfoPage = new SourceTreeInfoPage( project, module );
        addPage( sourceTreeInfoPage );
    }
}
