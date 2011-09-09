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
package com.sonatype.nexus.p2;

public interface P2Constants
{
    String CONTENT_XML = "/content.xml";

    String CONTENT_JAR = "/content.jar";

    String CONTENT_PATH = CONTENT_XML;

    String ARTIFACTS_XML = "/artifacts.xml";

    String ARTIFACTS_JAR = "/artifacts.jar";

    String ARTIFACTS_PATH = ARTIFACTS_XML;

    String COMPOSITE_CONTENT_XML = "/compositeContent.xml";

    String COMPOSITE_CONTENT_JAR = "/compositeContent.jar";

    String COMPOSITE_ARTIFACTS_XML = "/compositeArtifacts.xml";

    String COMPOSITE_ARTIFACTS_JAR = "/compositeArtifacts.jar";

    String[] METADATA_FILE_PATHS = new String[] { CONTENT_XML, CONTENT_JAR, ARTIFACTS_XML, ARTIFACTS_JAR,
        COMPOSITE_ARTIFACTS_XML, COMPOSITE_ARTIFACTS_JAR };

    // Used to create a nexus lock for metadata items - it doesn't have to be ARTIFACTS_PATH...
    // cstamas: IMO, it should NOT be an existing path, to not trip on locking happening in core
    String METADATA_LOCK_PATH = ARTIFACTS_PATH + "-lockPath";

    /**
     * Location of "private" state in repository/group local store. This is not meant to be used by clients directly
     */
    String PRIVATE_ROOT = "/.nexus/p2";

    String ARTIFACT_MAPPINGS_XML = PRIVATE_ROOT + "/artifact-mappings.xml";

    String SITE_XML = "/site.xml";

    String XMLPI_ARTIFACTS =
        "artifactRepository class='org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository' version='1.0.0'";

    String XMLPI_CONTENT =
        "metadataRepository class='org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository' version='1.0.0'";

    /**
     * The key for a string property providing a URL that can return mirrors of this repository.
     * 
     * @see IRepository#PROP_MIRRORS_URL
     */
    String PROP_MIRRORS_URL = "p2.mirrorsURL";

    /**
     * The key for a boolean property indicating that repository metadata is stored in compressed form. A compressed
     * repository will have lower bandwidth cost to read when remote, but higher processing cost to uncompress when
     * reading.
     * 
     * @see IRepository#PROP_COMPRESSED
     */
    String PROP_COMPRESSED = "p2.compressed";

    /**
     * The key for a string property containing the time when the repository was last modified.
     * 
     * @see IRepository#PROP_TIMESTAMP
     */
    String PROP_TIMESTAMP = "p2.timestamp"; //$NON-NLS-1$

    /**
     * Property used for interpolation of the content.xml, to filter ${baseurl} to the actual nexus base url.
     */
    String PROP_BASE_URL = "nexus.baseURL";

    /**
     * Property used to skip interpolation for P2lineup repository requests.
     */
    String PROP_SKIP_INTERPOLATION = "skip-interpolation";

    /**
     * Property key used to mark format of artifact.
     */
    String ARTIFACT_PROP_FORMAT = "format";

    /**
     * Property PROP_FORMAT value used to mark format of artifact as packed by Pack200.
     */
    String ARTIFACT_PROP_FORMAT_VALUE_PACKED = "packed";
}
