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

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IS2CodebaseChangeEventListener;
import com.sonatype.s2.project.core.S2CodebaseChangeEvent;
import com.sonatype.s2.project.core.S2ProjectCore;

/**
 * registered as codebaseChangeListener extension
 * @author mkleint
 *
 */
public class OpenCodebaseViewCallback implements IS2CodebaseChangeEventListener {

    protected static final Logger log = LoggerFactory.getLogger( OpenCodebaseViewCallback.class );

	public void codebaseChanged(S2CodebaseChangeEvent event) {
		if (event.getOldCodebase() == null && S2ProjectCore.getInstance().getWorkspaceCodebases().size() == 1) {
			//only at the first codebase change == materialization.. and only for the first materialized codebase.
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					try {
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(CodebaseUpdateView.ID);
					} catch (PartInitException e) {
						log.info("Could not open the Update Codebase View after materialization", e);
					}
				}
			});
		}
	}

}
