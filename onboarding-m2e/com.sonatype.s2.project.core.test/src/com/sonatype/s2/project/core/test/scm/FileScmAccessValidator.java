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
package com.sonatype.s2.project.core.test.scm;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.sonatype.s2.project.validation.api.IScmAccessData;
import com.sonatype.s2.project.validation.api.IScmAccessValidator;

public class FileScmAccessValidator
    implements IScmAccessValidator
{

    private static final String SCM_TYPE = "scm:testfile:";

    public boolean accept( IScmAccessData data )
    {
        return data.getRepositoryUrl() != null && data.getRepositoryUrl().startsWith( SCM_TYPE );
    }

    public boolean accept( String type )
    {
        return "testfile".equalsIgnoreCase( type );
    }

    public int getPriority()
    {
        return 100;
    }

    public IStatus validate( IScmAccessData data, IProgressMonitor monitor )
    {
        return Status.OK_STATUS;
    }

}
