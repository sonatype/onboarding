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
package com.sonatype.s2.project.ui.materialization;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sonatype.s2.project.core.test.AbstractMavenProjectMaterializationTest;

public class WorkingSetMaterializationCallbackTest
    extends AbstractMavenProjectMaterializationTest
{
    @Override
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
    }

    @Override
    @After
    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    @Test
    public void testBasicMaterialization()
        throws Exception
    {
        materialize( "resources/projects/simpleproject.xml" );

        assertMavenProject( "materialization-test", "simple-project", "0.0.1-SNAPSHOT" );
        assertWorkspaceProject( "simpleproject1" );
        IProject project = getWorkspaceProject( "simpleproject1" );

        IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
        IWorkingSet workingSet = workingSetManager.getWorkingSet( "simpleproject1" );
        assertNotNull( "Working set was not created", workingSet );

        // Verify that the project was added to the working set
        boolean found = false;
        for ( IAdaptable element : workingSet.getElements() )
        {
            if ( project.equals( element ) )
            {
                found = true;
                break;
            }
        }
        assertTrue( "The project was not added to the working set", found );
    }
}
