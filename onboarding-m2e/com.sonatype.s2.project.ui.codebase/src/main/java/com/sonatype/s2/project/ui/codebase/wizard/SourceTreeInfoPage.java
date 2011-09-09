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
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.authentication.IRealmChangeListener;
import org.maven.ide.eclipse.ui.common.authentication.RealmUrlCollector;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.codebase.composites.SCMLocationComposite;
import com.sonatype.s2.project.ui.codebase.composites.SourceTreeInfoComposite;

public class SourceTreeInfoPage
    extends WizardPage implements IRealmChangeListener
{
    private static Logger log = LoggerFactory.getLogger( SourceTreeInfoPage.class );

    private SourceTreeInfoComposite sourceTreeInfoComposite;

    private SCMLocationComposite scmLocationComposite;

    private IS2Project project;

    private IS2Module module;

    protected SourceTreeInfoPage( IS2Project project, IS2Module module )
    {
        super( SourceTreeInfoPage.class.getName() );
        this.project = project;
        this.module = module;

        setDescription( Messages.sourceTreeInfoPage_description );
        setTitle( Messages.sourceTreeInfoPage_title );
    }

    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayout( new GridLayout() );

        SwtValidationGroup validationGroup = SwtValidationGroup.create( SwtValidationUI.createUI( this ) );
        WidthGroup widthGroup = new WidthGroup();
        composite.addControlListener( widthGroup );

        sourceTreeInfoComposite = new SourceTreeInfoComposite( composite, widthGroup, validationGroup, null );
        sourceTreeInfoComposite.setModule( project, module );
        sourceTreeInfoComposite.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );

        scmLocationComposite = new SCMLocationComposite( composite, widthGroup, validationGroup, null )
        {
            @Override
            protected void validateScmAccess()
            {
                final Shell shell = getShell();
                try
                {
                    getContainer().run( true, true, new IRunnableWithProgress()
                    {

                        public void run( IProgressMonitor monitor )
                            throws InvocationTargetException, InterruptedException
                        {
                            monitor.beginTask( Messages.scmLocationComposite_validating, IProgressMonitor.UNKNOWN );
                            IStatus status = runValidation( shell, monitor );
                            if ( !status.isOK() )
                                StatusManager.getManager().handle( status, StatusManager.SHOW );
                        }
                    } );
                }
                catch ( InterruptedException e )
                {
                    log.error( e.getMessage(), e );
                }
                catch ( InvocationTargetException e )
                {
                    log.error( e.getMessage(), e );
                }
            }
        };
        scmLocationComposite.setModule( project, module );
        scmLocationComposite.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );
        scmLocationComposite.addRealmChangeListener( this );

        setControl( composite );
    }

    public void saveRealms( RealmUrlCollector realmUrlCollector )
    {
        scmLocationComposite.saveRealms( realmUrlCollector );
    }

    public void realmsChanged()
    {
        scmLocationComposite.setModule( project, module );
    }
}
