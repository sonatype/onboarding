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

import java.util.Collection;

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
import com.sonatype.s2.project.prefs.PreferenceGroup;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.codebase.composites.EclipseWorkspaceComposite;

public class WorkspacePage
    extends WizardPage implements IRealmChangeListener
{
    private EclipseWorkspaceComposite eclipseWorkspaceComposite;

    private IS2Project project;

    private Collection<PreferenceGroup> preferenceGroups;

    protected WorkspacePage( IS2Project project )
    {
        super( WorkspacePage.class.getName() );
        this.project = project;

        setDescription( Messages.eclipseWorkspaceComposite_description );
        setTitle( Messages.eclipseWorkspaceComposite_title );
    }

    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayout( new GridLayout() );

        SwtValidationGroup validationGroup = SwtValidationGroup.create( SwtValidationUI.createUI( this ) );
        WidthGroup widthGroup = new WidthGroup();
        composite.addControlListener( widthGroup );

        eclipseWorkspaceComposite = new EclipseWorkspaceComposite( composite, widthGroup, validationGroup, null )
        {

            @Override
            protected void saveEclipsePreferenceGroups( Collection<PreferenceGroup> groups )
            {
                preferenceGroups = groups;
            }

            @Override
            protected Collection<PreferenceGroup> getEclipsePreferenceGroups()
            {
                return preferenceGroups;
            }
        };
        eclipseWorkspaceComposite.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );
        eclipseWorkspaceComposite.setProject( project );
        eclipseWorkspaceComposite.addRealmChangeListener( this );

        setControl( composite );
    }

    public Collection<PreferenceGroup> getPreferenceGroups()
    {
        return preferenceGroups;
    }

    public void saveRealms( RealmUrlCollector realmUrlCollector )
    {
        eclipseWorkspaceComposite.saveRealms( realmUrlCollector );
    }

    public void realmsChanged()
    {
        eclipseWorkspaceComposite.setProject( project );
    }
}
