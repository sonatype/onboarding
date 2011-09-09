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
package com.sonatype.s2.publisher;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.AgentLocation;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import com.sonatype.s2.extractor.P2InstallationDiscoveryResult;
import com.sonatype.s2.extractor.P2MetadataAdapter;
import com.sonatype.s2.p2lineup.model.IP2LineupSourceRepository;
import com.sonatype.s2.publisher.test.helios.Activator;

public class ExtractorTest extends TestCase {

	private ServiceRegistration agentReg;
	private ServiceRegistration locationReg;

	private void registerAsRunningAgent(String profileId, String path) throws ProvisionException {
		System.setProperty("eclipse.p2.profile", profileId);
		IProvisioningAgentProvider provider = (IProvisioningAgentProvider) Activator.ctx.getService(Activator.ctx.getServiceReference(IProvisioningAgentProvider.class.getName()));
		IProvisioningAgent agent = provider.createAgent(new File(path).toURI());
		IAgentLocation location = new AgentLocation(new File(path).toURI());
		Properties p = new Properties();
		p.put(Constants.SERVICE_RANKING, new Integer(101));
		locationReg = Activator.ctx.registerService(IAgentLocation.class.getName(), location, p);
		
		Dictionary<String, Object> properties = new Hashtable<String, Object>(5);
		properties.put(Constants.SERVICE_RANKING, new Integer(101));
		properties.put(IProvisioningAgent.SERVICE_CURRENT, Boolean.TRUE.toString());
		agentReg = Activator.ctx.registerService(IProvisioningAgent.SERVICE_NAME, agent, properties);
	}
	
	public void testTypicalS2Install36() throws BundleException, ProvisionException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		registerAsRunningAgent("s2", "resources/extractor/s2install/p2");
		
		P2MetadataAdapter extract = new P2MetadataAdapter();
		P2InstallationDiscoveryResult results = null;
		try {
			results = extract.discoverInstallation(new NullProgressMonitor());
		} catch (CoreException e) {
			fail("Extraction of roots failed.");
		}
		assertEquals(1, results.getRootIUs().size());
		assertEquals("s2.p2Lineup", results.getRootIUs().iterator().next().getId());
	
		List<IP2LineupSourceRepository> metadataRepos = results.getSourceRepositories(); 
		assertFalse(metadataRepos.isEmpty());
		List<IP2LineupSourceRepository> artifactRepos = results.getSourceRepositories();
		assertFalse(artifactRepos.isEmpty());
		
		unregisterAgent();
	}
	
	private void unregisterAgent() {
		agentReg.unregister();
		locationReg.unregister();
	}

	public void testInstallWithDropins36() throws BundleException, ProvisionException {
		registerAsRunningAgent("SDKProfile", "resources/extractor/installWithDropins/p2");

		P2MetadataAdapter extract = new P2MetadataAdapter();
		P2InstallationDiscoveryResult roots = null;
		try {
			roots = extract.discoverInstallation(new NullProgressMonitor());
		} catch (CoreException e) {
			fail("Extraction of roots failed.");
		}
		assertNotNull(roots);
		assertFalse(roots.getRootIUs().isEmpty());
	
		List<IP2LineupSourceRepository> metadataRepos = roots.getSourceRepositories(); 
		assertFalse(metadataRepos.isEmpty());
		List<IP2LineupSourceRepository> artifactRepos = roots.getSourceRepositories();
		assertFalse(artifactRepos.isEmpty());
	}
}
