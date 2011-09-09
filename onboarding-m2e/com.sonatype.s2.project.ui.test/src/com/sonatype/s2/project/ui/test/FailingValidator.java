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
package com.sonatype.s2.project.ui.test;

import org.eclipse.core.runtime.IProgressMonitor;

import com.sonatype.s2.project.core.internal.S2ProjectPlugin;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.validation.api.AbstractProjectValidator;
import com.sonatype.s2.project.validation.api.IS2ProjectValidationStatus;
import com.sonatype.s2.project.validation.api.S2ProjectValidationStatus;

/**
 * A failing validator, handy for testing.
 * <P/>
 * Always fails a project if the project name is "fail-always". <BR/>
 * Fails a project but allows remediation if the name is "fail-first". <BR/>
 * Passes otherwise.
 */
public class FailingValidator
    extends AbstractProjectValidator
{
    private static final String FAIL_ALWAYS = "fail-always";

    private static final String FAIL_FIRST = "fail-first";

    private static IS2Project project;

    private static boolean remediated;

    private static boolean canRemediate;

    public IS2ProjectValidationStatus canRemediate( boolean headless )
    {
        return canRemediate ? pass() : fail();
    }

    public String getPluginId()
    {
        return S2ProjectPlugin.PLUGIN_ID;
    }

    public IS2ProjectValidationStatus remediate( boolean headless, IProgressMonitor monitor )
    {
        if ( canRemediate )
        {
            remediated = true;
            return pass();
        }
        throw new UnsupportedOperationException();
    }

    public IS2ProjectValidationStatus validate( IS2Project s2Project, IProgressMonitor monitor )
    {
        if ( project == s2Project && remediated )
        {
            return pass();
        }

        project = s2Project;
        canRemediate = false;
        remediated = false;

        String name = project.getName();
        if ( FAIL_ALWAYS.equals( name ) )
        {
            return fail();
        }
        if ( FAIL_FIRST.equals( name ) )
        {
            canRemediate = true;
            return fail();
        }

        return pass();
    }

    private IS2ProjectValidationStatus pass()
    {
        return S2ProjectValidationStatus.getOKStatus( this );
    }

    private IS2ProjectValidationStatus fail()
    {
        return createErrorStatus( "FAIL" );
    }
}
