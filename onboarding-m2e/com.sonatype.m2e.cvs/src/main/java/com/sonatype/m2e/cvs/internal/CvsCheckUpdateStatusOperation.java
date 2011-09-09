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
package com.sonatype.m2e.cvs.internal;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.GlobalOption;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.core.client.listeners.IUpdateMessageListener;
import org.eclipse.team.internal.ccvs.core.client.listeners.UpdateListener;
import org.eclipse.team.internal.ccvs.ui.operations.SingleCommandOperation;
import org.eclipse.ui.IWorkbenchPart;

@SuppressWarnings( "restriction" )
class CvsCheckUpdateStatusOperation
    extends SingleCommandOperation
{
    private boolean changes = false;

    CvsCheckUpdateStatusOperation( IWorkbenchPart part, ResourceMapping[] mappings, LocalOption[] options )
    {
        super( part, mappings, options );
    }

    @Override
    protected IStatus[] getErrors()
    {
        // TODO Auto-generated method stub
        return super.getErrors();
    }

    boolean hasIncomingChanges()
    {
        return changes;
    }

    protected IStatus executeCommand( Session session, CVSTeamProvider provider, ICVSResource[] resources,
                                      boolean recurse, IProgressMonitor monitor )
        throws CVSException, InterruptedException
    {
        int i = 0;
        String[] args = new String[resources.length];
        for ( ICVSResource resource : resources )
        {
            args[i++] = resource.getName();
        }
        // Perform a "cvs -n update -d [-r tag] ." in order to get the
        // messages from the server that will indicate what has changed on the
        // server.
        return Command.SYNCUPDATE.execute( session, new GlobalOption[] { Command.DO_NOT_CHANGE },
                                           getLocalOptions( recurse ), args,
                                           new UpdateListener( new IUpdateMessageListener()
                                           {
                                               public void directoryInformation( ICVSFolder root, String path,
                                                                                 boolean newDirectory )
                                               {
                                               }

                                               public void directoryDoesNotExist( ICVSFolder root, String path )
                                               {
                                               }

                                               public void fileInformation( int type, ICVSFolder root,
                                                                            String filename )
                                               {
                                                   switch ( type )
                                                   {
                                                       case Update.STATE_MERGEABLE_CONFLICT:
                                                       case Update.STATE_CONFLICT:
                                                       case Update.STATE_REMOTE_CHANGES:
                                                           changes = true;
                                                   }
                                               }

                                               public void fileDoesNotExist( ICVSFolder root, String filename )
                                               {
                                                   changes = true;
                                               }
                                           } ), monitor );
    }

    @Override
    protected String getTaskName( CVSTeamProvider provider )
    {
        return "Synchronizing " + provider.getProject().getName();
    }

    @Override
    protected String getTaskName()
    {
        return "Synchronizing";
    }
}