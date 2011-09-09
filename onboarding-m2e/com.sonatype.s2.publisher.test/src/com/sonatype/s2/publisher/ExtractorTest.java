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
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import com.sonatype.s2.extractor.P2InstallationDiscoveryResult;
import com.sonatype.s2.extractor.P2MetadataAdapter;
import com.sonatype.s2.p2lineup.model.IP2LineupSourceRepository;

public class ExtractorTest extends TestCase {

	// Test with things in dropins
	// Test with things normally installed
	// Test with shared case?

	public void testTypicalS2Install() throws BundleException {
		try {
			stopTransient("org.eclipse.equinox.p2.exemplarysetup");
		} catch(NullPointerException e) {
			//this will fail when run on 3.6
		}
		stopTransient("org.eclipse.equinox.p2.engine");
		stopTransient("org.eclipse.equinox.p2.core");

		System.setProperty("eclipse.p2.data.area", new File("resources/extractor/s2install/p2").getAbsolutePath());
		System.setProperty("eclipse.p2.profile", "s2");
		startTransient("org.eclipse.equinox.p2.core");
		startTransient("org.eclipse.equinox.p2.engine");
		try {
			startTransient("org.eclipse.equinox.p2.exemplarysetup");
		} catch(NullPointerException e) {
			//fail on 3.6
		}

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
	}
	
	public void testInstallWithDropins() throws BundleException {
		try {
			stopTransient("org.eclipse.equinox.p2.exemplarysetup");
		} catch(NullPointerException e) {
			//this will fail when run on 3.6
		}
		stopTransient("org.eclipse.equinox.p2.engine");
		stopTransient("org.eclipse.equinox.p2.core");

		System.setProperty("eclipse.p2.data.area", new File("resources/extractor/installWithDropins/p2").getAbsolutePath());
		System.setProperty("eclipse.p2.profile", "SDKProfile");
		startTransient("org.eclipse.equinox.p2.core");
		startTransient("org.eclipse.equinox.p2.engine");
		try {
			startTransient("org.eclipse.equinox.p2.exemplarysetup");
		} catch(NullPointerException e) {
			//this will fail when run on 3.6
		}
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

	private boolean startTransient(String bundleName) throws BundleException {
		Bundle bundle = Platform.getBundle(bundleName);
		if (bundle == null)
			return false;
		bundle.start(Bundle.START_TRANSIENT);
		return true;
	}

	private void stopTransient(String bundleName) throws BundleException {
		Bundle bundle = Platform.getBundle(bundleName);
		bundle.stop(Bundle.STOP_TRANSIENT);
	}
}
