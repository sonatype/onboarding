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

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.composites.ValidatingComposite;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;

import com.sonatype.s2.publisher.nexus.NexusLineupPublishingInfo;

abstract public class LineupComposite
    extends ValidatingComposite
{
    private ListenerList listeners;

    private boolean updating;

    private NexusLineupPublishingInfo info;

    private Font errorFont;

    public LineupComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                            FormToolkit toolkit )
    {
        super( parent, widthGroup, validationGroup, toolkit );
        this.listeners = new ListenerList();
    }

    public void setLineupInfo( NexusLineupPublishingInfo info )
    {
        this.info = info;
        update();
    }

    public NexusLineupPublishingInfo getLineupInfo()
    {
        return info;
    }

    public void addLineupChangeListener( ILineupChangeListener listener )
    {
        assert listener instanceof ILineupChangeListener;
        listeners.add( listener );
    }

    public void removeLineupChangeListener( ILineupChangeListener listener )
    {
        listeners.remove( listener );
    }

    protected void notifyLineupChangeListeners()
    {
        if ( !updating )
        {
            for ( Object listener : listeners.getListeners() )
            {
                ( (ILineupChangeListener) listener ).lineupChanged( info );
            }
        }
    }

    public void update()
    {
        updating = true;
        update( info );
        updating = false;
    }

    protected Font getErrorFont()
    {
        return errorFont;
    }

    protected void createErrorFont( Font baseFont )
    {
        FontData[] data = baseFont.getFontData();
        FontData[] newData = new FontData[data.length];

        for ( int i = data.length - 1; i >= 0; i-- )
        {
            newData[i] = new FontData( data[i].getName(), data[i].getHeight(), SWT.ITALIC );
        }

        errorFont = new Font( Display.getCurrent(), newData );
    }

    @Override
    public void dispose()
    {
        if ( errorFont != null )
        {
            errorFont.dispose();
        }
        super.dispose();
    }

    abstract protected void update( NexusLineupPublishingInfo info );
}
