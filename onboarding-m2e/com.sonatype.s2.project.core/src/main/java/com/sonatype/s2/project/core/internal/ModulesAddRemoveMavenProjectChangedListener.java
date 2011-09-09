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
package com.sonatype.s2.project.core.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;

import com.sonatype.s2.project.core.S2ProjectCore;

public class ModulesAddRemoveMavenProjectChangedListener
    implements IMavenProjectChangedListener
{
    public void mavenProjectChanged( MavenProjectChangedEvent[] events, IProgressMonitor monitor )
    {
        ModulesAddRemoveJob.Request request = new ModulesAddRemoveJob.Request();

        for ( MavenProjectChangedEvent event : events )
        {
            request.addOldModules( getModules( event.getOldMavenProject() ) );
            request.addNewModules( getModules( event.getMavenProject() ) );
        }

        S2ProjectCore.getInstance().getModulesAddRemoveJob().schedule( request );
    }

    private List<IPath> getModules( IMavenProjectFacade projectFacade )
    {
        ArrayList<IPath> modules = new ArrayList<IPath>();

        if ( projectFacade != null && "pom".equals( projectFacade.getPackaging() ) )
        {
            File basedir = projectFacade.getPomFile().getParentFile();

            for ( String module : projectFacade.getMavenProjectModules() )
            {
                modules.add( Path.fromOSString( new File( basedir, module ).getAbsolutePath() ) );
            }
        }

        return modules;
    }

}
