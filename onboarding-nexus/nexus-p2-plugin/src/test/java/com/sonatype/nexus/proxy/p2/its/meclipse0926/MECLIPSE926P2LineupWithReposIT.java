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
package com.sonatype.nexus.proxy.p2.its.meclipse0926;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;
import org.junit.Test;

import com.sonatype.nexus.proxy.p2.its.AbstractP2LineupIT;
import com.sonatype.s2.p2lineup.model.IP2Lineup;
import com.sonatype.s2.p2lineup.model.P2LineupHelper;

public class MECLIPSE926P2LineupWithReposIT
    extends AbstractP2LineupIT
{
    public MECLIPSE926P2LineupWithReposIT()
    {
        super( IP2Lineup.LINEUP_REPOSITORY_ID );
    }

    @Test
    public void p2lineupWithP2Advice()
        throws Exception
    {
        IP2Lineup p2Lineup = uploadP2Lineup( "p2lineup_MECLIPSE926.xml" );

        File installDir = new File( "target/eclipse/meclipse0926" );

        installUsingP2( getP2RepoURL( p2Lineup ), P2LineupHelper.getMasterInstallableUnitId( p2Lineup ),
                        installDir.getCanonicalPath() );

        File feature = new File( installDir, "features/com.sonatype.nexus.p2.its.feature_1.0.0" );
        Assert.assertTrue( feature.exists() && feature.isDirectory() );

        File bundle = new File( installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" );
        Assert.assertTrue( bundle.canRead() );

        File dir = new File( getP2RuntimeLocation(), "mytestpath1" );
        Assert.assertTrue( dir.exists() );
        Assert.assertTrue( dir.isDirectory() );
        dir = new File( getP2RuntimeLocation(), "mytestpath2" );
        Assert.assertTrue( dir.exists() );
        Assert.assertTrue( dir.isDirectory() );

        FileInputStream fis = new FileInputStream(new File(installDir.getCanonicalFile(), "/p2/org.eclipse.equinox.p2.engine/.settings/org.eclipse.equinox.p2.metadata.repository.prefs"));
        try
        {
            Properties metadataPreferencesProps = new Properties();
            metadataPreferencesProps.load( fis );
            // We can't test the installation source p2 repository because p2 removes it :(
            // assertRepository( getBaseNexusUrl() + "content/repositories/nx-p2lineup/MECLIPSE926/MECLIPSE926/0.0.1",
            // true, metadataPreferencesProps );
            assertRepository( getBaseNexusUrl() + "content/repositories/p2proxy", "My p2proxy test repository", false,
                              metadataPreferencesProps );
        }
        finally
        {
            IOUtil.close( fis );
            fis = null;
        }

        fis = new FileInputStream(new File(installDir.getCanonicalFile(), "/p2/org.eclipse.equinox.p2.engine/.settings/org.eclipse.equinox.p2.artifact.repository.prefs"));
        try
        {
            Properties artifactPreferecesProps = new Properties();
            artifactPreferecesProps.load( fis );
            // We can't test the installation source p2 repository because p2 removes it :(
            // assertRepository( getBaseNexusUrl() + "content/repositories/nx-p2lineup/MECLIPSE926/MECLIPSE926/0.0.1",
            // true, artifactPreferecesProps );
            assertRepository( getBaseNexusUrl() + "content/repositories/p2proxy", "My p2proxy test repository", false,
                              artifactPreferecesProps );
        }
        finally
        {
            IOUtil.close( fis );
            fis = null;
        }
    }

    private void assertRepository( String repositoryURL, String repositoryName, boolean enabled, Properties properties )
    {
        String key = repositoryURL.replace( '/', '_' );
        assertProperty( properties, key + "/uri", repositoryURL );
        assertProperty( properties, key + "/nickname", repositoryName );
        assertProperty( properties, key + "/enabled", "" + enabled );
    }

    private void assertProperty( Properties properties, String key, String value )
    {
        String actualValue = properties.getProperty( "repositories/" + key );
        Assert.assertEquals( "Expected " + key + "=" + value, value, actualValue );
    }
}
