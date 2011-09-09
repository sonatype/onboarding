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
package com.sonatype.nexus.onboarding.its.meclipse957;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sonatype.nexus.onboarding.its.AbstractOnboardingIT;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.descriptor.Project;
import com.sonatype.s2.project.model.descriptor.io.xpp3.S2ProjectDescriptorXpp3Reader;

public class MECLIPSE957NexusBaseURLFilteringIT
    extends AbstractOnboardingIT
{
    private S2ProjectDescriptorXpp3Reader reader = new S2ProjectDescriptorXpp3Reader();

    public MECLIPSE957NexusBaseURLFilteringIT()
    {
        super( "onboarding" );
    }

    @Test
    public void testNexusBaseURLFiltering()
        throws Exception
    {
        getDeployUtils().deployWithWagon( "http", getNexusTestRepoUrl(),
                                     new File( "resources/descriptor-with-property-reference.xml" ),
 "g/a/v/"
                                         + IS2Project.PROJECT_DESCRIPTOR_FILENAME );

        URL url = new URL( getNexusTestRepoUrl() + "g/a/v/" + IS2Project.PROJECT_DESCRIPTOR_FILENAME );

        Project project = readDescriptor( url );
        Assert.assertEquals( getBaseNexusUrl(), project.getMavenSettingsLocation().getUrl() + "/" );
    }

    private Project readDescriptor( URL url )
        throws IOException, FileNotFoundException, XmlPullParserException
    {
        BufferedInputStream is = new BufferedInputStream( url.openStream() );
        try
        {
            return reader.read( is );
        }
        finally
        {
            IOUtil.close( is );
        }
    }

}
