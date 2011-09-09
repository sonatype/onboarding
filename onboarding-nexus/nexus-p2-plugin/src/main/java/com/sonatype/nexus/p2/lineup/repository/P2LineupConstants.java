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
package com.sonatype.nexus.p2.lineup.repository;

import com.sonatype.nexus.p2.P2Constants;

public interface P2LineupConstants
    extends P2Constants
{
    String LINEUP_DESCRIPTOR_XML = "/p2lineup.xml";

    String LINEUP_INSTALL_TEMPLATE_JNLP = "templates/p2lineup_install.jnlp";

    String LINEUP_INSTALL_TEMPLATE_PROPERTIES = "templates/p2lineup_install.properties";

    String LINEUP_INSTALL_JNLP = "/install.jnlp";

    String LINEUP_INSTALL_PROPERTIES = "/install.properties";
}
