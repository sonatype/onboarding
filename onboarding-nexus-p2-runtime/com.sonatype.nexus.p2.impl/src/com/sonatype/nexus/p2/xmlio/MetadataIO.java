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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryIO;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;

@SuppressWarnings( "restriction" )
public class MetadataIO
{
    public void writeXML( IMetadataRepository repository, File file )
        throws IOException
    {
        OutputStream os = new BufferedOutputStream( new FileOutputStream( file ) );
        try
        {
            new MetadataRepositoryIO().write( repository, os );
        }
        finally
        {
            os.close();
        }
    }
}
