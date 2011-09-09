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
package com.sonatype.s2.extractor;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import com.sonatype.s2.internal.extractor.Extractor;
import com.sonatype.s2.p2lineup.model.IP2LineupInstallableUnit;
import com.sonatype.s2.p2lineup.model.IP2LineupSourceRepository;
import com.sonatype.s2.p2lineup.model.P2LineupSourceRepository;

public class P2MetadataAdapter
{

    public P2InstallationDiscoveryResult discoverInstallation( IProgressMonitor monitor ) throws CoreException
    {
        ArrayList<IP2LineupSourceRepository> repos = new ArrayList<IP2LineupSourceRepository>();
        ArrayList<IP2LineupInstallableUnit> ius = new ArrayList<IP2LineupInstallableUnit>();

        Extractor p2 = new Extractor();

        for ( URI uri : p2.getActiveMetadataRepositories() )
        {
            repos.add( toModelObject( uri ) );
        }

        Collection<IInstallableUnit> uis = p2.getRootIUs( monitor );
        if ( uis != null )
        {
            for ( IInstallableUnit root : uis )
            {
                ius.add( AvailableGroupWrapper.toP2LineupInstalleUnit( root ) );
            }
        }

        return new P2InstallationDiscoveryResult( repos, ius );
    }
    
    public static P2LineupSourceRepository toModelObject(URI uri) {
    	try
        {
	    	P2LineupSourceRepository repo = new P2LineupSourceRepository();
	    	repo.setLayout( "p2" );
	    	repo.setUrl( uri.toURL().toExternalForm() );
	    	return repo;
        } catch ( MalformedURLException e )
        {
        	//This can't happen. p2 would have refused it.
             new IllegalStateException("Unexpected exception. Can't convert:" + uri);
         }
        return null;
    }
}
