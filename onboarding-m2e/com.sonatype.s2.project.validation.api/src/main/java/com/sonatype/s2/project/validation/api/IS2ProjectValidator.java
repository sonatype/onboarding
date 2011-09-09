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

/**
 * Interface for s2 project validators.
 * <p>
 * NOTE: Do not forget to exclude the implementation classes from obfuscation (i.e. tycho-proguard-plugin in pom.xml) :)
 */
public interface IS2ProjectValidator
{
    String EXTENSION_POINT_ID = "com.sonatype.s2.project.validation.IS2ProjectValidator";

    String CATEGORY_ACCESS_VALIDATION = "access";

    S2ProjectValidationContext NULL_VALIDATION_CONTEXT = null;

    /**
     * Validates the specified s2 project.
     * 
     * @return OK if the validation is successful, ERROR otherwise. In both cases, the returned status must have a
     *         meaningful message.
     */
    IS2ProjectValidationStatus validate( IS2Project s2Project, IProgressMonitor monitor );

    /**
     * @param headless true if the current execution does not have user interface
     * @return OK if the problem(s) detected by a previous call to validate() can be fixed by calling remediate()
     */
    IS2ProjectValidationStatus canRemediate( boolean headless );

    /**
     * @param headless true if the current execution does not have user interface
     * @return OK if the problems detected by a previous call to validate() were fixed
     */
    IS2ProjectValidationStatus remediate( boolean headless, IProgressMonitor monitor );

    String getCategory();
    
    String getName();

    void configure( IConfigurationElement configuration );

    String getPluginId();

    /**
     * @return true if this validator is applicable to the specified validationContext
     */
    boolean isApplicable( S2ProjectValidationContext validationContext );
}
