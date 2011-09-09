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

import java.util.List;

import junit.framework.Assert;

import org.codehaus.plexus.util.StringUtils;

public class ProjectDescriptorTest
    extends AbstractProjectDescriptorTest
{
    public void testLoadProjectDescriptor()
        throws Exception
    {
        IS2Project project = loadS2Project( "resources/descriptor.xml" );

        assertEquals( "group.foo", project.getGroupId() );
        assertEquals( "Foo", project.getArtifactId() );
        assertEquals( "1.2.3", project.getVersion() );

        assertNull( project.getP2LineupLocation() );

        List<IS2Module> modules = project.getModules();
        assertEquals( 1, modules.size() );

        assertEquals( "Bar", modules.get( 0 ).getName() );
        assertEquals( "munchy", modules.get( 0 ).getScmLocation().getUrl() );
        assertEquals( "git", modules.get( 0 ).getScmLocation().getBranch() );

        List<String> profiles = modules.get( 0 ).getProfiles();
        assertEquals( "moo", profiles.get( 0 ) );

        List<String> roots = modules.get( 0 ).getRoots();
        assertEquals( 2, roots.size() );
        assertEquals( ".", roots.get( 0 ) );
        assertEquals( "relpath", roots.get( 1 ) );
        
        List<ICIServerLocation> hudsons = modules.get( 0 ).getCiServers();
        assertEquals( 1, hudsons.size());
        assertEquals( "pron", hudsons.get( 0 ).getUrl());
        assertEquals( 2, hudsons.get( 0 ).getJobs().size());
        assertEquals( "one", hudsons.get( 0 ).getJobs().get( 0 ));
        assertEquals( "two", hudsons.get( 0 ).getJobs().get( 1 ));
        
        
        assertFalse( project.isRequiresMavenSettings() );
        assertEquals( "settings.xml", project.getMavenSettingsLocation().getUrl() );

        assertEquals( "eclipse.prefs", project.getEclipsePreferencesLocation().getUrl() );

        assertEquals( "32m", project.getPrerequisites().getRequiredMemory() );

        assertNotNull( project.getEclipseInstallationLocation() );
        assertEquals( "install", project.getEclipseInstallationLocation().getDirectory() );
        assertTrue( project.getEclipseInstallationLocation().isCustomizable() );

        assertNotNull( project.getEclipseWorkspaceLocation() );
        assertEquals( "workspace", project.getEclipseWorkspaceLocation().getDirectory() );
        assertFalse( project.getEclipseWorkspaceLocation().isCustomizable() );
        

        assertEquals( "1.0.4", project.getInstallerVersion() );
    }

    public void testLoadProjectDescriptorWithP2LineupURL()
        throws Exception
    {
        IS2Project project = loadS2Project( "resources/descriptorWithP2LineupURL.xml" );

        assertEquals( "group.foo", project.getGroupId() );
        assertEquals( "Foo", project.getArtifactId() );
        assertEquals( "1.2.3", project.getVersion() );

        assertNotNull( project.getP2LineupLocation() );
        assertEquals( "file:fake_url", project.getP2LineupLocation().getUrl() );

        List<IS2Module> modules = project.getModules();
        assertEquals( 1, modules.size() );

        assertEquals( "Bar", modules.get( 0 ).getName() );
        assertEquals( "munchy", modules.get( 0 ).getScmLocation().getUrl() );

        List<String> profiles = modules.get( 0 ).getProfiles();
        assertEquals( "moo", profiles.get( 0 ) );

        List<String> roots = modules.get( 0 ).getRoots();
        assertEquals( 2, roots.size() );
        assertEquals( ".", roots.get( 0 ) );
        assertEquals( "relpath", roots.get( 1 ) );

        assertEquals( "settings.xml", project.getMavenSettingsLocation().getUrl() );
    }

    public void testLoadCodebaseDescriptorWithRequiresMavenSettings()
        throws Exception
    {
        IS2Project codebase = loadS2Project( "resources/descriptorWithRequiresMavenSettings.xml" );

        assertTrue( codebase.isRequiresMavenSettings() );
        assertEquals( "settings.xml", codebase.getMavenSettingsLocation().getUrl() );
    }

    public void testCodebaseVersion_1_0_4()
        throws Exception
    {
        IS2Project codebase = loadS2Project( "resources/codebaseDescriptorVersion_1.0.4.xml" );
        String xml = writeS2Project( codebase );
        int count = StringUtils.countMatches( xml, "security-realm" );
        Assert.assertEquals( "Found 'security-realm' in XML:\n" + xml, 0, count );
    }
}
