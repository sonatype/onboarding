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

import com.sonatype.s2.project.model.IS2Project;

public interface S2PublisherConstants
{
    String PMD_FILENAME = IS2Project.PROJECT_DESCRIPTOR_FILENAME;

    String PMD_ICON_FILENAME = IS2Project.PROJECT_ICON_FILENAME;

    String PMD_PATH = "mse";

    String PROJECT_MATERIALIZER_IU_ID = "com.sonatype.s2.project.materializer";

    String TOUCHPOINT_ID = "org.eclipse.equinox.p2.osgi";

    String TOUCHPOINT_VERSION = "1.0.0";
}
