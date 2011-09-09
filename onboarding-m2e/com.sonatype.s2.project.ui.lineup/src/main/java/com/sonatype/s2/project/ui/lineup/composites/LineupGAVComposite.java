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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.composites.GAVComposite;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;

import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;

public class LineupGAVComposite
    extends LineupComposite
{
    private GAVComposite gavComposite;

    public LineupGAVComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                               FormToolkit toolkit )
    {
        super( parent, widthGroup, validationGroup, toolkit );

        GridLayout gridLayout = new GridLayout();
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.verticalSpacing = 0;
        setLayout( gridLayout );

        gavComposite =
            new GAVComposite( this, widthGroup, validationGroup, toolkit, ( toolkit != null ? 0
                            : GAVComposite.VALIDATE_PROJECT_NAME ) | GAVComposite.VALIDATE_OSGI_VERSION )
            {
                @Override
                protected void saveGroupId( String groupId )
                {
                    getLineupInfo().getLineup().setGroupId( groupId );
                    notifyLineupChangeListeners();
                }

                @Override
                protected void saveArtifactId( String artifactId )
                {
                    getLineupInfo().getLineup().setId( artifactId );
                    notifyLineupChangeListeners();
                }

                @Override
                protected void saveVersion( String version )
                {
                    getLineupInfo().getLineup().setVersion( version );
                    notifyLineupChangeListeners();
                }
            };
        gavComposite.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );
    }

    @Override
    protected void update( NexusLineupPublishingInfo info )
    {
        gavComposite.setVersion( info.getLineup().getVersion() );
        gavComposite.setArtifactId( info.getLineup().getId() );
        gavComposite.setGroupId( info.getLineup().getGroupId() );
    }
}
