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
package com.sonatype.s2.project.ui.lineup.composites;

import static org.maven.ide.eclipse.ui.common.FormUtils.nvl;
import static org.maven.ide.eclipse.ui.common.FormUtils.toNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;

import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;

public class LineupInfoComposite
    extends LineupComposite
{
    public static final String DESCRIPTION_CONTROL = "descriptionText"; //$NON-NLS-1$

    private Text descriptionText;

    public LineupInfoComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                                FormToolkit toolkit )
    {
        super( parent, widthGroup, validationGroup, toolkit );

        setLayout( new GridLayout( 2, false ) );

        Label label = createLabel( Messages.lineupInfoComposite_description_label );
        GridData labelData = (GridData) label.getLayoutData();
        labelData.verticalAlignment = SWT.TOP;

        descriptionText = createText( SWT.MULTI | SWT.WRAP | SWT.V_SCROLL, 1, 1, DESCRIPTION_CONTROL, null ); //$NON-NLS-1$  
        descriptionText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                getLineupInfo().getLineup().setDescription( toNull( descriptionText ) );
                notifyLineupChangeListeners();
            }
        } );
        GridData descriptionTextData = (GridData) descriptionText.getLayoutData();
        descriptionTextData.grabExcessVerticalSpace = true;
        descriptionTextData.heightHint = 100;
        descriptionTextData.minimumHeight = 100;
        descriptionTextData.verticalAlignment = SWT.FILL;
    }

    @Override
    protected void update( NexusLineupPublishingInfo info )
    {
        descriptionText.setText( nvl( info.getLineup().getDescription() ) );
    }

}
