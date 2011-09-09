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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.authentication.IRealmChangeListener;
import org.maven.ide.eclipse.ui.common.authentication.RealmUrlCollector;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.codebase.composites.EclipseInstallationComposite;

public class EclipsePage
    extends WizardPage
    implements IRealmChangeListener
{
    private EclipseInstallationComposite eclipseInstallationComposite;

    private IS2Project project;

    private boolean enableP2LineupControls = true;

    protected EclipsePage( IS2Project project )
    {
        super( EclipsePage.class.getName() );
        this.project = project;

        setDescription( Messages.eclipseInstallationComposite_description );
        setTitle( Messages.eclipseInstallationComposite_title );
    }

    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayout( new GridLayout() );

        SwtValidationGroup validationGroup = SwtValidationGroup.create( SwtValidationUI.createUI( this ) );
        WidthGroup widthGroup = new WidthGroup();
        composite.addControlListener( widthGroup );

        eclipseInstallationComposite =
            new EclipseInstallationComposite(
                                              composite,
                                              widthGroup,
                                              validationGroup,
                                              null,
                                              enableP2LineupControls ? EclipseInstallationComposite.ENABLE_P2_LINEUP_CONTROLS
                                                              : 0 );
        eclipseInstallationComposite.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );
        eclipseInstallationComposite.setProject( project );
        eclipseInstallationComposite.addRealmChangeListener( this );

        setControl( composite );
    }

    public void saveRealms( RealmUrlCollector realmUrlCollector )
    {
        eclipseInstallationComposite.saveRealms( realmUrlCollector );
    }

    public void realmsChanged()
    {
        eclipseInstallationComposite.setProject( project );
    }

    public void setLineupControlsEnabled( boolean b )
    {
        enableP2LineupControls = b;
    }
}
