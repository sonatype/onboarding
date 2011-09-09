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
package com.sonatype.s2.installer.internal.ui;

import java.io.File;

public class MissingMavenSettingsException
    extends RuntimeException
{
    private static final long serialVersionUID = -6167124584369444680L;

    public MissingMavenSettingsException( File defaultUserMavenSettingsFile )
    {
        super( createMessage( defaultUserMavenSettingsFile ) );
    }

    private static String createMessage( File defaultUserMavenSettingsFile )
    {
        String message = "The codebase requires maven settings";
        if ( defaultUserMavenSettingsFile != null )
        {
            message += ", but the " + defaultUserMavenSettingsFile.getAbsolutePath() + " file does not exist.";
        }
        return message;
    }
}
