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

public class MECLIPSE1937CodebaseNameContainsColonTest
    extends AbstractMavenProjectMaterializationTest
{
    private String originalUserSettings;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        this.originalUserSettings = mavenConfiguration.getUserSettingsFile();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            mavenConfiguration.setUserSettingsFile( originalUserSettings );
        }
        finally
        {
            super.tearDown();
        }
    }

    public void testProjectSpecificUserSettings()
        throws Exception
    {
        materialize( "resources/projects/MECLIPSE1937CodebaseNameContainsColon/mse-codebase.xml" );

        String userSettings = mavenConfiguration.getUserSettingsFile();

        assertNotNull( userSettings );
        userSettings = userSettings.replace( '\\', '/' );
        assertTrue( "Unexpected settings path:" + userSettings,
                    userSettings.endsWith( "/projects/Test _________/settings.xml" ) );
    }
}
