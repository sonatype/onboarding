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
package com.sonatype.s2.ssh;

public interface SshHandler
{

    /**
     * The priority of this handler. The handler with the highest priority will be used.
     * 
     * @return The priority of this handler.
     */
    int getPriority();

    /**
     * Asks the user a yes/no question.
     * 
     * @param message The question to ask.
     * @return {@code true} if the answer is 'yes', {@code false} otherwise.
     */
    boolean promptYesNo( String message );

    /**
     * Notifies the user of a message.
     * 
     * @param message The informational message.
     */
    void showMessage( String message );

}
