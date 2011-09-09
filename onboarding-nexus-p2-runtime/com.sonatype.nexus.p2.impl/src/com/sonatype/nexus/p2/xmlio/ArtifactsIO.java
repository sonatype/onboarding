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
package com.sonatype.nexus.p2.xmlio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryIO;

@SuppressWarnings( "restriction" )
public class ArtifactsIO
{
    public void writeXML( SimpleArtifactRepository repository, File file )
        throws IOException
    {
        FileOutputStream os = new FileOutputStream( file );
        try
        {
            new SimpleArtifactRepositoryIO().write( repository, os );
        }
        finally
        {
            os.close();
        }
    }
}
