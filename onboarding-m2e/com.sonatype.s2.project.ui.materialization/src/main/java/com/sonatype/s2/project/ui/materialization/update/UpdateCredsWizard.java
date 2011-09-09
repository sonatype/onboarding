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
package com.sonatype.s2.project.ui.materialization.update;

import org.eclipse.core.runtime.IStatus;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.internal.wizards.AbstractMaterializationWizard;
import com.sonatype.s2.project.ui.internal.wizards.ProjectScmSecurityRealmsPage;

/**
 * this class is an UBER-HACK, extensibility by inheritance never works right..

 * problems include:
 * * appearance of next/prev buttons when there is only one page.
 * * next button is always enabled, even on last page
 * * for one page there shall be Ok button, not finish
 * @author mkleint
 * 
 */
class UpdateCredsWizard extends
		AbstractMaterializationWizard {

	public UpdateCredsWizard(IS2Project project, IStatus validationStatus) {
		super();
		setWindowTitle("Update your security realm credentials for codebase");

		this.project = project;
		this.validationStatus = validationStatus;
	}

	@Override
	public boolean performFinish() {
		ProjectScmSecurityRealmsPage page = (ProjectScmSecurityRealmsPage) getContainer()
				.getCurrentPage();
		return page.validateCredentials();
	}

	@Override
	public boolean canFinish() {
		return getContainer().getCurrentPage() == getPages()[getPageCount() - 1];
	}

	@Override
	public void addPages() {
		addMaterializationPages();
		// god bless us someone doesn't add some other panels in the
		// future..
		if (userSettingsPage != null) {
			removePage(userSettingsPage);
		}
		if (projectValidationPage != null) {
			removePage(projectValidationPage);
		}
	}
}