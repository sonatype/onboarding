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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;

import com.sonatype.s2.p2lineup.model.IP2LineupTargetEnvironment;
import com.sonatype.s2.p2lineup.model.P2LineupTargetEnvironment;
import com.sonatype.s2.project.ui.internal.Images;
import com.sonatype.s2.project.ui.lineup.Messages;

public class EnvironmentLabelProvider
    extends LabelProvider
    implements ITableLabelProvider
{
    public Image getColumnImage( Object element, int columnIndex )
    {
        if ( element instanceof IP2LineupTargetEnvironment )
        {
            String os = ( (IP2LineupTargetEnvironment) element ).getOsgiOS();
            if ( Platform.OS_LINUX.equals( os ) )
            {
                return Images.PLATFORM_LINUX;
            }
            else if ( Platform.OS_MACOSX.equals( os ) )
            {
                return Images.PLATFORM_MACOSX;
            }
            else if ( Platform.OS_WIN32.equals( os ) )
            {
                return Images.PLATFORM_WIN32;
            }
        }
        return null;
    }

    public String getColumnText( Object element, int columnIndex )
    {
        if ( element instanceof IP2LineupTargetEnvironment )
        {
            IP2LineupTargetEnvironment e = (IP2LineupTargetEnvironment) element;
            return NLS.bind( Messages.environmentLabelProvider_format,
                             new String[] { e.getOsgiOS(), e.getOsgiWS(), e.getOsgiArch() } );
        }
        return element.toString();
    }

    public static List<IP2LineupTargetEnvironment> getSupportedEnvironments()
    {
        List<IP2LineupTargetEnvironment> environments = new ArrayList<IP2LineupTargetEnvironment>();
        environments.add( environment( Platform.OS_WIN32, Platform.WS_WIN32, Platform.ARCH_X86 ) );
        environments.add( environment( Platform.OS_WIN32, Platform.WS_WIN32, Platform.ARCH_X86_64 ) );
        environments.add( environment( Platform.OS_MACOSX, Platform.WS_COCOA, Platform.ARCH_X86_64 ) );
        environments.add( environment( Platform.OS_LINUX, Platform.WS_GTK, Platform.ARCH_X86 ) );
        environments.add( environment( Platform.OS_LINUX, Platform.WS_GTK, Platform.ARCH_X86_64 ) );
        return environments;
    }

    private static IP2LineupTargetEnvironment environment( String os, String ws, String arch )
    {
        P2LineupTargetEnvironment e = new P2LineupTargetEnvironment();
        e.setOsgiOS( os );
        e.setOsgiWS( ws );
        e.setOsgiArch( arch );
        return e;
    }
}
