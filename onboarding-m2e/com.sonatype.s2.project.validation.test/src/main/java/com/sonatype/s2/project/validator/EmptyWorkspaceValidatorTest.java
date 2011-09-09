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
package com.sonatype.s2.project.validator;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

import com.sonatype.s2.project.validation.api.IS2ProjectValidationStatus;
import com.sonatype.s2.project.validation.api.IS2ProjectValidator;
import com.sonatype.s2.project.validation.api.S2ProjectValidationContext;

public class EmptyWorkspaceValidatorTest
    extends AbstractMavenProjectTestCase
{
    public void testEmptyWorkspace()
    {
        EmptyWorkspaceValidator validator = new EmptyWorkspaceValidator();
        IS2ProjectValidationStatus validationStatus = validator.validate( null /* s2Project */, monitor );
        assertEquals( Status.OK, validationStatus.getSeverity() );
    }

    public void testNotEmptyWorkspace()
        throws Exception
    {
        ResolverConfiguration configuration = new ResolverConfiguration();
        importProject( "resources/projects/dummy/pom.xml", configuration );
        waitForJobsToComplete();
        EmptyWorkspaceValidator validator = new EmptyWorkspaceValidator();
        IS2ProjectValidationStatus validationStatus =
            validator.validate( null /* s2Project */, new NullProgressMonitor() );
        assertEquals( Status.ERROR, validationStatus.getSeverity() );
        assertEquals( EmptyWorkspaceValidator.VALIDATION_FAILED_MESSAGE, validationStatus.getMessage() );
        assertFalse( validator.canRemediate( true ).isOK() );
        assertFalse( validator.canRemediate( false ).isOK() );
        try
        {
            validator.remediate( false, monitor );
            fail( "Expected UnsupportedOperationException" );
        }
        catch ( UnsupportedOperationException expected )
        {
        }
    }

    public void testIsApplicable()
        throws Exception
    {
        EmptyWorkspaceValidator validator = new EmptyWorkspaceValidator();
        assertTrue( validator.isApplicable( IS2ProjectValidator.NULL_VALIDATION_CONTEXT ) );

        S2ProjectValidationContext validationContext = new S2ProjectValidationContext();
        validationContext.setProperty( S2ProjectValidationContext.FRESH_INSTALL_PROPNAME, "true" );
        assertFalse( validator.isApplicable( validationContext ) );
    }
}
