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
package com.sonatype.s2.project.integration.test;

import junit.framework.TestCase;

@SuppressWarnings( "restriction" )
public class ProblemReportTest
    extends TestCase
{

    public void testInclusionOfS2Logs()
        throws Exception
    {
        // DataGatherer gatherer = new DataGatherer( null, null, null, null, null, null );
        // List<File> files = gatherer.gather( null, EnumSet.noneOf( Data.class ), new NullProgressMonitor() );
        //
        // assertTrue( !files.isEmpty() );
        // for ( File tmpFile : files )
        // {
        // tmpFile.deleteOnExit();
        // }
        // File tmpFile = files.get( 0 );
        //
        // assertTrue( tmpFile.isFile() );
        // ZipFile zip = new ZipFile( tmpFile );
        // try
        // {
        // assertNotNull( zip.getEntry( "logs/0.log" ) );
        // }
        // finally
        // {
        // zip.close();
        // }
    }

}
