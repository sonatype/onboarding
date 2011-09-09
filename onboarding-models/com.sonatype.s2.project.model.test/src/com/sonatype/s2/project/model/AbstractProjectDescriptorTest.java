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
package com.sonatype.s2.project.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

import org.codehaus.plexus.util.IOUtil;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.S2ProjectFacade;

public abstract class AbstractProjectDescriptorTest
    extends TestCase
{
    protected IS2Project loadS2Project( String projectDescriptorFileName )
        throws Exception
    {
        File projectDescriptorFile = new File( projectDescriptorFileName );
        assertTrue( projectDescriptorFile.exists() );

        InputStream is = new FileInputStream( projectDescriptorFile );
        try
        {
            return S2ProjectFacade.loadProject( is, true );
        }
        finally
        {
            IOUtil.close( is );
        }
    }

    protected String writeS2Project( IS2Project codebase )
        throws Exception
    {
        OutputStream os = new ByteArrayOutputStream();
        try
        {
            S2ProjectFacade.writeProject( codebase, os );
            return os.toString();
        }
        finally
        {
            IOUtil.close( os );
        }
    }
}
