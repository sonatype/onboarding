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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import junit.framework.TestCase;

import org.codehaus.plexus.util.IOUtil;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.S2ProjectFacade;

public class S2ProjectFacadeTest
    extends TestCase
{
    public void testCurrentInstallerVersion_LoadWriteExistingCodebase()
        throws Exception
    {
        IS2Project codebase;
        InputStream is = new FileInputStream( new File( "resources/codebaseDescriptorWithoutInstallerVersion.xml" ) );
        try
        {
            codebase = S2ProjectFacade.loadProject( is, true /* strict */);
            assertEquals( "1.0.4", codebase.getInstallerVersion() );
        }
        finally
        {
            IOUtil.close( is );
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try
        {
            S2ProjectFacade.writeProject( codebase, os );
            is = new ByteArrayInputStream( os.toString().getBytes() );
            codebase = S2ProjectFacade.loadProject( is, true /* strict */);
            assertEquals( IS2Project.CURRENT_INSTALLER_VERSION, codebase.getInstallerVersion() );
        }
        finally
        {
            IOUtil.close( os );
            IOUtil.close( is );
        }
    }

    public void testCurrentInstallerVersion_CreateWriteNewCodebase()
        throws Exception
    {
        IS2Project codebase =
            S2ProjectFacade.createProject( "group.foo", "testCurrentInstallerVersion_CreateWriteNewCodebase", "1.2.3" );
        assertEquals( IS2Project.CURRENT_INSTALLER_VERSION, codebase.getInstallerVersion() );

        InputStream is = null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try
        {
            S2ProjectFacade.writeProject( codebase, os );
            is = new ByteArrayInputStream( os.toString().getBytes() );
            codebase = S2ProjectFacade.loadProject( is, true /* strict */);
            assertEquals( IS2Project.CURRENT_INSTALLER_VERSION, codebase.getInstallerVersion() );
        }
        finally
        {
            IOUtil.close( os );
            IOUtil.close( is );
        }
    }
}
