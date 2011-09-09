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
package com.sonatype.s2.project.ui.materialization.update;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.internal.update.CodebaseUpdateOperation;
import com.sonatype.s2.project.core.internal.update.DetermineCodebaseUpdateStatusOperation;
import com.sonatype.s2.project.ui.materialization.Messages;

@SuppressWarnings( "restriction" )
public class CodebaseUpdateJob
    extends AbstractCodebaseUpdateJob
{
    public CodebaseUpdateJob( IWorkspaceCodebase codebase )
    {
        super( codebase, Messages.codebaseUpdateJob_title, false );
    }

    @Override
    protected IStatus doRun( IProgressMonitor monitor )
        throws CoreException
    {
    	if (codebase == null)
    	{
    		return Status.OK_STATUS;
    	}
    	//MECLIPSE-1839
    	if (CodebaseAccessChecker.checkAccess(codebase.getDescriptorUrl(), monitor)) {
    		// access check was cancelled.
    		return Status.OK_STATUS;
    	}
    	
        DetermineCodebaseUpdateStatusOperation op = new DetermineCodebaseUpdateStatusOperation( codebase );
        
        op.run( monitor );
        
        CodebaseUpdateOperation op2 = new CodebaseUpdateOperation( codebase );

        op2.run( monitor );

        IStatus status = op2.getStatus();

        // TODO show "Merge conflicts" pop-up and show the update view

        // problems that can be reflected in codebase update view, like merge conflicts

        // problems that need separate pop-up, like problems parsing pom.xml files, etc
        // these should be represented as error markers, I guess

        return status;
    }

}
