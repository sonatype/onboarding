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

import java.util.List;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;

import com.sonatype.s2.project.model.ICIServerLocation;
import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.descriptor.S2Module;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.codebase.composites.SourceTreeListComposite;
import com.sonatype.s2.project.ui.internal.Dialog;
import com.sonatype.s2.project.ui.internal.Images;

@SuppressWarnings( "restriction" )
public class SourceTreesPage
    extends WizardPage
{
    private IS2Project project;

    private SourceTreeListComposite sourceTreeListComposite;

    protected SourceTreesPage( IS2Project project )
    {
        super( SourceTreesPage.class.getName() );
        this.project = project;

        setDescription( Messages.sourceTreeListComposite_description );
        setTitle( Messages.sourceTreeListComposite_title );
    }

    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayout( new GridLayout() );

        SwtValidationGroup validationGroup = SwtValidationGroup.create( SwtValidationUI.createUI( this ) );

        sourceTreeListComposite = new SourceTreeListComposite( composite, null, validationGroup, null )
        {
            @Override
            protected void removeModule( IS2Module module )
            {
                getProject().getModules().remove( module );
                getViewer().refresh();
            }

            @Override
            protected void editModule( int index )
            {
                List<IS2Module> modules = getProject().getModules();
                IS2Module originalModule = modules.get( index );
                IS2Module newModule = new S2Module();
                copyModule( originalModule, newModule );

                SourceTreeWizard sourceTreeWizard = new SourceTreeWizard( getProject(), newModule );
                sourceTreeWizard.setWindowTitle( Messages.sourceTreeListComposite_edit_title );

                WizardDialog wizardDialog = new WizardDialog( getShell(), sourceTreeWizard );
                if ( wizardDialog.open() == Dialog.OK )
                {
                    copyModule( newModule, originalModule );
                    getViewer().refresh();
                }
            }

            @Override
            protected void addModule( IS2Module module )
            {
                getProject().addModule( module );
                getViewer().refresh();
            }

            private void copyModule( IS2Module from, IS2Module to )
            {
                to.setBuildUrl( from.getBuildUrl() );
                to.setDocsUrl( from.getDocsUrl() );
                to.setHomeUrl( from.getHomeUrl() );
                to.setIssuesUrl( from.getIssuesUrl() );
                to.setName( from.getName() );
                to.setScmLocation( from.getScmLocation() );
                //MECLIPSE-1661 for now just populate the ciServers section based on the url.
                to.getCiServers().clear();
                for (ICIServerLocation serv : from.getCiServers()) {
                    to.getCiServers().add( serv );
                }
            }
        };
        sourceTreeListComposite.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );

        sourceTreeListComposite.getViewer().setLabelProvider( new LabelProvider()
        {
            @Override
            public Image getImage( Object element )
            {
                return element instanceof IS2Module ? Images.CATALOG_ENTRY : null;
            }

            public String getText( Object element )
            {
                if ( element instanceof IS2Module )
                {
                    IS2Module module = (IS2Module) element;
                    return NLS.bind( "{0} ({1})", module.getName(), module.getScmLocation().getUrl() );
                }
                return super.getText( element );
            }
        } );

        sourceTreeListComposite.setProject( project );

        setControl( composite );
    }
}
