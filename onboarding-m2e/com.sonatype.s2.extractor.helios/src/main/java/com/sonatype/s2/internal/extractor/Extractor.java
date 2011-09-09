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
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;


@SuppressWarnings( "restriction" )
public class Extractor {
	
	IProvisioningAgent agent = null;
	
	public Extractor() {
		agent = (IProvisioningAgent) ServiceHelper.getService(Activator.getContext(), IProvisioningAgent.SERVICE_NAME);
	}
	
	public URI[] getActiveMetadataRepositories() {
		IMetadataRepositoryManager mgr = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		if(mgr == null)
			return null;
		return mgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
	}
	
	/**
	 * @return null if we can't figure out what is running because of misconfiguration
	 */
	public URI[] getActiveArtifactRepositories() {
		IArtifactRepositoryManager mgr = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);;
		if(mgr == null)
			return null;
		return mgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
	}
	
	
	/**
	 * @return null if we can't figure out what is running because of misconfiguration
	 * @throws CoreException if exception could not be found
	 */
	public Collection<IInstallableUnit> getRootIUs(IProgressMonitor monitor) throws CoreException {
		return getRootsFromRunningProfile(monitor);
	}
	
	public Collection<IInstallableUnit> getDropinsEntrie(IProgressMonitor monitor) throws CoreException {
		IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.class.getName());
		if (profileRegistry == null)
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not acquire the profile registry."));
		IProfile runningProfile = profileRegistry.getProfile(IProfileRegistry.SELF);
		if (runningProfile == null)
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The profile of the running instance could not be found."));
		IQueryResult<IInstallableUnit> allDropinsElements = runningProfile.available(new IUProfilePropertyQuery("org.eclipse.equinox.p2.reconciler.dropins", "true"), monitor);
		return allDropinsElements.query(QueryUtil.createIUGroupQuery(), monitor).toUnmodifiableSet();
	}
	
	public URI findRepositoryFor(IInstallableUnit searched, IProgressMonitor monitor) {
		IMetadataRepositoryManager mgr = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		if(mgr == null)
			return null;
		URI[] allRepos = mgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (int j = 0; j < allRepos.length; j++) {
			try {
				IQueryResult<IInstallableUnit> found = mgr.loadRepository(allRepos[j], monitor).query(QueryUtil.createIUQuery(searched), monitor);
				if (! found.isEmpty())
					return allRepos[j];
			} catch (ProvisionException e) {
				//Ignore repos that we can't load
			}
		}
		return null;
	}
	
	private Collection<IInstallableUnit> getRootsFromRunningProfile(IProgressMonitor monitor) throws CoreException {
		IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (profileRegistry == null)
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not acquire the profile registry."));
		IProfile runningProfile = profileRegistry.getProfile(IProfileRegistry.SELF);
		if (runningProfile == null)
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The profile of the running instance could not be found."));
		Collection<IInstallableUnit> allRoots = runningProfile.available(new IUProfilePropertyQuery(IProfile.PROP_PROFILE_ROOT_IU, "true"), monitor).toSet();
		allRoots.removeAll(runningProfile.available(new IUProfilePropertyQuery("org.eclipse.equinox.p2.reconciler.dropins", "true"), monitor).toUnmodifiableSet());
		return allRoots;
	}
}
