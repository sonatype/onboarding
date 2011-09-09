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
package com.sonatype.s2.p2lineup.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import junit.framework.TestCase;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.sonatype.s2.p2lineup.model.io.xpp3.P2LineupXpp3Reader;
import com.sonatype.s2.p2lineup.model.io.xstream.P2LineupXstreamIO;

public class P2LineupLoadTest
    extends TestCase
{
    public void testXpp3()
        throws Exception
    {
        File lineupFile = new File( "resources/testLineup.xml" );
        IP2Lineup p2Lineup = loadLineupXpp3( lineupFile );

        assertLineup( p2Lineup );
    }

    protected void assertLineup( IP2Lineup p2Lineup )
    {
        assertEquals( "testLineup", p2Lineup.getId() );
        assertEquals( "1.2.3", p2Lineup.getVersion() );
        assertEquals( "Test Lineup Name", p2Lineup.getName() );
        assertEquals( "Test Lineup Description", p2Lineup.getDescription() );

        assertEquals( 2, p2Lineup.getRepositories().size() );
        Iterator<IP2LineupSourceRepository> repositoryIter = p2Lineup.getRepositories().iterator();
        IP2LineupSourceRepository repository = repositoryIter.next();
        assertEquals( "http://sourcerepo1", repository.getUrl() );
        assertEquals( "p2", repository.getLayout() );
        repository = repositoryIter.next();
        assertEquals( "http://sourcerepo2", repository.getUrl() );
        assertEquals( "m2", repository.getLayout() );

        assertEquals( 2, p2Lineup.getRootInstallableUnits().size() );
        Iterator<IP2LineupInstallableUnit> rootIUIter = p2Lineup.getRootInstallableUnits().iterator();
        IP2LineupInstallableUnit iu = rootIUIter.next();
        assertEquals( "rootIU1", iu.getId() );
        assertEquals( "0.0.1", iu.getVersion() );
        assertEquals( 0, iu.getTargetEnvironments().size() );
        iu = rootIUIter.next();
        assertEquals( "rootIU2", iu.getId() );
        assertEquals( "0.0.2", iu.getVersion() );
        assertEquals( 1, iu.getTargetEnvironments().size() );

        IP2LineupP2Advice p2Advice = p2Lineup.getP2Advice();
        assertNotNull( p2Advice );
        assertEquals( "org.eclipse.equinox.p2.osgi", p2Advice.getTouchpointId() );
        assertEquals( "1.0.0", p2Advice.getTouchpointVersion() );
        assertEquals( 2, p2Advice.getAdvices().size() );
        assertEquals( "advice1", p2Advice.getAdvices().get( 0 ) );
        assertEquals( "advice2", p2Advice.getAdvices().get( 1 ) );

        assertEquals( 2, p2Lineup.getTargetEnvironments().size() );
        Iterator<IP2LineupTargetEnvironment> targetEnvironmentIter = p2Lineup.getTargetEnvironments().iterator();
        IP2LineupTargetEnvironment environment = targetEnvironmentIter.next();
        assertEquals( "win32", environment.getOsgiOS() );
        assertEquals( "win32", environment.getOsgiWS() );
        assertEquals( "x86", environment.getOsgiArch() );
        environment = targetEnvironmentIter.next();
        assertEquals( "linux", environment.getOsgiOS() );
        assertEquals( "gtk", environment.getOsgiWS() );
        assertEquals( "x86_64", environment.getOsgiArch() );
    }

    public void testXstream()
        throws Exception
    {
        File lineupFile = new File( "resources/testLineup.xml" );
        IP2Lineup lineup = loadLineupXpp3( lineupFile );

        P2LineupXstreamIO io = new P2LineupXstreamIO();

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        io.writeLineup( lineup, buf );

        IP2Lineup echo = io.readLineup( new ByteArrayInputStream( buf.toByteArray() ) );
        assertLineup( echo );
    }

    private IP2Lineup loadLineupXpp3( File lineupFile )
        throws IOException, XmlPullParserException
    {
        FileReader fr = new FileReader( lineupFile );
        try
        {
            return new P2LineupXpp3Reader().read( fr, true /* strict */);
        }
        finally
        {
            fr.close();
        }
    }
}
