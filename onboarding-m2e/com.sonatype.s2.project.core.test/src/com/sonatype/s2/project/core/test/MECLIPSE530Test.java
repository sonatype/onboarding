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


public class MECLIPSE530Test
    extends AbstractMavenProjectMaterializationTest
{

    /**
     * Tests that project import does not fail in case a module name clashes with the artifact id of a project.
     */
    public void testModuleNameClashesWithChildProjectName()
        throws Exception
    {
        HttpServer httpServer = newHttpServer();
        httpServer.start();

        materialize( httpServer.getHttpUrl() + "/projects/MECLIPSE-530-project-name-clash/s2.xml" );

        assertWorkspaceProjects( 1 );
        
        // this is aggregator project. note that name matches sourceTree name, not artifactId
        assertWorkspaceProject( "nexus" );
        assertMavenProject( "test", "nexus-aggregator", "0.0.1-SNAPSHOT" );

        // module artifactId=nexus did not get imported as a separate project because of the project name collision
    }

}
