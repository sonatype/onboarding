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
package com.sonatype.s2.project.validation.api;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.IUrlLocation;

/**
 * Interface for s2 access validators.
 * <p>
 * NOTE: Do not forget to exclude the implementation classes from obfuscation (i.e. tycho-proguard-plugin in pom.xml) :)
 */
public interface IS2AccessValidator
{
    String EXTENSION_POINT_ID = "com.sonatype.s2.project.validation.IS2AccessValidator";

    String NULL_SECURITY_REALMID = null;

    /**
     * @return true if this validator can be used to validate the specified location.
     */
    boolean accept( IUrlLocation location );

    /**
     * Validates the specified location.
     * 
     * @param location the location to be validated
     * @return OK if the validation is successful, ERROR otherwise. In both cases, the returned status must have a
     *         meaningful message.
     */
    IS2ProjectValidationStatus validate( IUrlLocation location, IProgressMonitor monitor );

    /**
     * Validates access to the resource locations in an s2 project associated with the specified security realm. If the
     * securityRealmId is null, it validates access to all realms.
     * 
     * @return OK if the validation is successful, ERROR otherwise. In both cases, the returned status must have a
     *         meaningful message.
     */
    IS2ProjectValidationStatus validate( IS2Project s2Project, String securityRealmId, IProgressMonitor monitor );

    void configure( IConfigurationElement configuration );
}
