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


public class MECLIPSE258detachedModulesTest
    extends AbstractMavenProjectMaterializationTest
{
    public void testDetachedModules()
        throws Exception
    {
        materialize( "resources/projects/MECLIPSE-258detachedModules/s2.xml" );

        assertMavenProject( "MECLIPSE-258detachedModules", "parent", "0.0.1-SNAPSHOT" );
        assertMavenProject( "MECLIPSE-258detachedModules", "detached", "0.0.1-SNAPSHOT" );

        assertNotNull( workspace.getRoot().getProject( "MECLIPSE-258detachedModules" ) );
        assertNotNull( workspace.getRoot().getProject( "detached" ) );
    }
}
