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
package com.sonatype.s2.project.core.test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.core.ide.IIDEUpdater;
import com.sonatype.s2.project.core.internal.WorkspaceCodebase;
import com.sonatype.s2.project.core.internal.update.CodebaseUpdateOperation;
import com.sonatype.s2.project.core.internal.update.IUpdateOperation;
import com.sonatype.s2.project.model.IS2Project;

public class MECLIPSE1849UpdateIDENotLineupCreatedTest
    extends AbstractMavenProjectMaterializationTest
{
    private static final String CODEBASES_FOLDER = "target/codebases";

    private S2ProjectCore core;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        FileUtils.deleteDirectory( new File( CODEBASES_FOLDER ) );

        core = S2ProjectCore.getInstance();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            core = null;

            FileUtils.deleteDirectory( new File( CODEBASES_FOLDER ) );
        }
        finally
        {
            super.tearDown();
        }
    }

    /**
     * @param targetPath must match mse-codebase.xml!
     */
    private File getBasedir( String sourcePath, String targetPath )
        throws IOException
    {
        File src = new File( sourcePath );

        File dst = new File( CODEBASES_FOLDER, targetPath );
        FileUtils.deleteDirectory( dst );

        FileUtils.copyDirectoryStructure( src, dst );

        filterCodebaseDescriptor( new File( sourcePath, IS2Project.PROJECT_DESCRIPTOR_FILENAME ),
                                  new File( dst, IS2Project.PROJECT_DESCRIPTOR_FILENAME ) );

        return dst;
    }

    public void testUpdateIDENotLineupCreated()
        throws Exception
    {
        File basedir =
            getBasedir( "resources/projects/MECLIPSE-1849-UpdateIDENotLineupCreated",
                        "MECLIPSE-1849-UpdateIDENotLineupCreated" );

        materialize( new File( basedir, IS2Project.PROJECT_DESCRIPTOR_FILENAME ).getCanonicalPath(), false );

        assertEquals( 0, core.getWorkspaceCodebases().get( 0 ).getSourceTrees().size() );

        IWorkspaceCodebase codebase = core.getWorkspaceCodebases().get( 0 );
        ( (WorkspaceCodebase) codebase ).setIsP2LineupUpToDate( IIDEUpdater.NOT_LINEUP_MANAGED );
        assertEquals( IIDEUpdater.NOT_LINEUP_MANAGED, codebase.getIsP2LineupUpToDate() );
        ( (WorkspaceCodebase) codebase ).setPending( codebase );
        CodebaseUpdateOperation codebaseUpdateOperation = new CodebaseUpdateOperation( codebase );
        List<IUpdateOperation> operations = codebaseUpdateOperation.getOperations();
        assertEquals( operations.toString(), 0, operations.size() );
    }
}
