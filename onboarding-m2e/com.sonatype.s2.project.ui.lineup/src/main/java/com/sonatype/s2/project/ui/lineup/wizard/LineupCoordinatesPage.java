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
package com.sonatype.s2.project.ui.lineup.wizard;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.sonatype.s2.project.ui.internal.composites.NexusUrlComposite;
import com.sonatype.s2.project.ui.internal.wizards.NexusUrlPage;
import com.sonatype.s2.project.ui.lineup.Messages;
import com.sonatype.s2.project.ui.lineup.composites.LineupGAVComposite;
import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;

public class LineupCoordinatesPage
    extends NexusUrlPage
{
    private LineupGAVComposite gavComposite;

    private NexusLineupPublishingInfo info;

    public LineupCoordinatesPage( NexusLineupPublishingInfo info )
    {
        this.info = info;

        setDescription( Messages.lineupCoordinatesPage_description );
        setTitle( Messages.lineupCoordinatesPage_title );
    }

    @Override
    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayout( new GridLayout() );
        composite.addControlListener( getWidthGroup() );

        Label nexusServerLabel = new Label( composite, SWT.NONE );
        nexusServerLabel.setText( Messages.lineupCoordinatesPage_nexusServer );

        NexusUrlComposite nexusUrlComposite = createNexusUrlComposite( composite );
        ( (GridData) nexusUrlComposite.getLayoutData() ).horizontalIndent = 10;

        Label codebaseCoordinatesLabel = new Label( composite, SWT.NONE );
        codebaseCoordinatesLabel.setText( Messages.lineupCoordinatesPage_lineupCoordinates );
        GridData codebaseCoordinatesData = new GridData( SWT.LEFT, SWT.TOP, false, false );
        codebaseCoordinatesData.verticalIndent = 10;
        codebaseCoordinatesLabel.setLayoutData( codebaseCoordinatesData );

        gavComposite = new LineupGAVComposite( composite, getWidthGroup(), getValidationGroup(), null );
        GridData gavData = new GridData( SWT.FILL, SWT.TOP, true, false );
        gavData.horizontalIndent = 10;
        gavComposite.setLayoutData( gavData );
        gavComposite.setLineupInfo( info );

        setControl( composite );
    }

    @Override
    public IStatus checkNexus( String url, IProgressMonitor monitor )
    {
        IStatus status = super.checkNexus( url, monitor );
        if ( status.isOK() )
        {
            info.setServerUrl( url );
            return info.getPublisher().preValidateUpload( url, info.getLineup(), monitor );

        }
        return status;
    }

    @Override
    protected String getMessageForNotFoundException()
    {
        return Messages.lineupCoordinatesPage_messageForNotFoundException;
    }
}
