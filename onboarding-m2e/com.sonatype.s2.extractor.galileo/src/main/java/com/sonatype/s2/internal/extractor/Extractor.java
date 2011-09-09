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
package com.sonatype.s2.internal.extractor;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.engine.IUProfilePropertyQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IUPropertyQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;

@SuppressWarnings( "restriction" )
public class Extractor {
	
	public URI[] getActiveMetadataRepositories() {
		IMetadataRepositoryManager mgr = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
		if(mgr == null)
			return null;
		URI[] localRepos = mgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_LOCAL);
		Arrays.sort(localRepos);
		URI[] allRepos = mgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
		ArrayList<URI> result = new ArrayList<URI>();
		for (int i = 0; i < allRepos.length; i++) {
			if (Arrays.binarySearch(localRepos, allRepos[i]) < 0)
				result.add(allRepos[i]);
		}
		return result.toArray(new URI[result.size()]);
	}
	
	/**
	 * @return null if we can't figure out what is running because of misconfiguration
	 * @throws CoreException if exception could not be found
	 */
	public Collection<IInstallableUnit> getRootIUs(IProgressMonitor monitor) throws CoreException {
		return getRootsFromRunningProfile(monitor);
	}
	
	public Collection<IInstallableUnit> getDropinsEntrie(IProgressMonitor monitor) throws CoreException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null)
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not acquire the profile registry."));
		IProfile runningProfile = profileRegistry.getProfile(IProfileRegistry.SELF);
		if (runningProfile == null)
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The profile of the running instance could not be found."));
		Collector allDropinsElements = runningProfile.available(new IUProfilePropertyQuery(runningProfile, "org.eclipse.equinox.p2.reconciler.dropins", "true"), new Collector(), monitor);
		return allDropinsElements.query(new IUPropertyQuery(IInstallableUnit.PROP_TYPE_GROUP, "true"), new Collector(), monitor).toCollection();
	}
	
	public URI findRepositoryFor(IInstallableUnit searched, IProgressMonitor monitor) {
		IMetadataRepositoryManager mgr = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
		if(mgr == null)
			return null;
		URI[] allRepos = mgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (int j = 0; j < allRepos.length; j++) {
			try {
				Collector found;
				found = mgr.loadRepository(allRepos[j], monitor).query(new InstallableUnitQuery(searched.getId(), searched.getVersion()), new Collector(), monitor);
				if (! found.isEmpty())
					return allRepos[j];
			} catch (ProvisionException e) {
				//Ignore repos that we can't load
			}
		}
		return null;
	}
	
	private Collection<IInstallableUnit> getRootsFromRunningProfile(IProgressMonitor monitor) throws CoreException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null)
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not acquire the profile registry."));
		IProfile runningProfile = profileRegistry.getProfile(IProfileRegistry.SELF);
		if (runningProfile == null)
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The profile of the running instance could not be found."));
		Collection<IInstallableUnit> allRoots = new HashSet<IInstallableUnit>(runningProfile.available(new IUProfilePropertyQuery(runningProfile, IInstallableUnit.PROP_PROFILE_ROOT_IU, "true"), new Collector(), monitor).toCollection());
		Collection<IInstallableUnit> allDropins = new HashSet<IInstallableUnit>(runningProfile.available(new IUProfilePropertyQuery(runningProfile, "org.eclipse.equinox.p2.reconciler.dropins", "true"), new Collector(), monitor).toCollection());
		allRoots.removeAll(allDropins); 
		return allRoots;
	}
}
