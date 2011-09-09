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
package com.sonatype.nexus.p2.facade.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.logging.Logger;

import com.sonatype.nexus.p2.rest.model.P2LineupUnresolvedInstallableUnit;

public class P2LineupResolutionResult
{
    private final File tmpFolder;

    private final File contentFile;

    private final File artifactsFile;

    private final File artifactMappingsFile;

    private List<P2LineupUnresolvedInstallableUnit> unresolvedInstallableUnits =
        new ArrayList<P2LineupUnresolvedInstallableUnit>();

    private Logger log;

    public P2LineupResolutionResult( Logger log )
        throws IOException
    {
        this.log = log;
        tmpFolder = File.createTempFile( "p2lineup.content.", ".folder" );
        tmpFolder.delete();
        if ( tmpFolder.mkdirs() == false )
        {
            throw new IOException( "Can't create folder " + tmpFolder.getName() );
        }
        contentFile = new File( tmpFolder, "content.xml" );
        artifactsFile = new File( tmpFolder, "artifacts.xml" );
        artifactMappingsFile = new File( tmpFolder, "p2lineup.artifact-mappings.xml" );
    }

    public File getContentFile()
    {
        return contentFile;
    }

    public File getArtifactsFile()
    {
        return artifactsFile;
    }

    public File getArtifactMappingsFile()
    {
        return artifactMappingsFile;
    }

    public List<P2LineupUnresolvedInstallableUnit> getUnresolvedInstallableUnits()
    {
        return unresolvedInstallableUnits;
    }

    public void addUnresolvedInstallableUnit( P2LineupUnresolvedInstallableUnit iu )
    {
        unresolvedInstallableUnits.add( iu );
    }

    public void cleanup()
    {
        deleteFile( contentFile );
        deleteFile( artifactsFile );
        deleteFile( artifactMappingsFile );
        deleteFile( tmpFolder );
        unresolvedInstallableUnits.clear();
    }

    private void deleteFile( File file )
    {
        if ( file == null )
        {
            return;
        }
        if ( !file.delete() )
        {
            try
            {
                log.error( "Cannot delete file: " + file.getCanonicalPath() );
            }
            catch ( IOException e )
            {
                log.error( e.getMessage(), e );
                log.error( "Cannot delete file: " + file.toString() );
            }
        }
    }

    public boolean isSuccess()
    {
        return unresolvedInstallableUnits.size() == 0;
    }
}
