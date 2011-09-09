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
import java.util.List;


public interface Main
{
    /**
     * The source tree is up to date with source code repository.
     */
    public static final String STATUS_UPTODATE = "uptodate";

    /**
     * The source tree has been added to the (remote) codebase descriptor but has not been checked out and imported into
     * workspace yet.
     */
    public static final String STATUS_ADDED = "added";

    /**
     * The source tree can be updated from remote source repository and/or codebase descriptor
     */
    public static final String STATUS_CHANGED = "changed";

    /**
     * The source tree has been removed from (remote) codebase descriptor and can be safely removed locally.
     */
    public static final String STATUS_REMOVED = "removed";

    /**
     * The source tree is in unsupported synchronization state.
     */
    public static final String STATUS_NOT_SUPPORTED = "not-supported";

    /**
     * Source tree name (from codebase descriptor)
     */
    public String getName();

    /**
     * Names of enabled maven profiles (from codebase descriptor)
     */
    public List<String> getProfiles();

    /**
     * Project import roots (from codebase descriptor)
     */
    public List<String> getRoots();

    /**
     * Scm location url (from codebase descriptor)
     */
    public String getScmUrl();

    /**
     * Scm location branch (from codebase descriptor)
     */
    public String getScmBranch();

    /**
     * Canonical filesystem path of the checked-out source tree root. Never null and used to uniquely identify source
     * trees.
     */
    public String getLocation();

    /**
     * Source tree status. Not null for pending workspace codebases.
     */
    public String getStatus();

    /**
     * Human readable message associated with the source tree status. Not null for STATUS_NOT_SUPPORTED.
     * 
     * @TODO consider making this into list of message ids.
     */
    public String getStatusMessage();
}
